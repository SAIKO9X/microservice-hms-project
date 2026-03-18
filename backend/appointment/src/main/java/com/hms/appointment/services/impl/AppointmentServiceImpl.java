package com.hms.appointment.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hms.appointment.clients.ProfileFeignClient;
import com.hms.appointment.clients.UserFeignClient;
import com.hms.appointment.config.RabbitMQConfig;
import com.hms.appointment.dto.event.AppointmentEvent;
import com.hms.appointment.dto.event.AppointmentStatusChangedEvent;
import com.hms.appointment.dto.event.WaitlistNotificationEvent;
import com.hms.appointment.dto.external.DoctorProfile;
import com.hms.appointment.dto.external.PatientProfile;
import com.hms.appointment.dto.external.UserResponse;
import com.hms.appointment.dto.request.AppointmentCreateRequest;
import com.hms.appointment.dto.request.AvailabilityRequest;
import com.hms.appointment.dto.response.*;
import com.hms.appointment.entities.*;
import com.hms.appointment.enums.AppointmentStatus;
import com.hms.appointment.enums.AppointmentType;
import com.hms.appointment.repositories.*;
import com.hms.appointment.services.AppointmentService;
import com.hms.common.audit.AuditChangeTracker;
import com.hms.common.dto.event.AppointmentCompletionStartedEvent;
import com.hms.common.dto.event.EventEnvelope;
import com.hms.common.dto.response.ResponseWrapper;
import com.hms.common.exceptions.AccessDeniedException;
import com.hms.common.exceptions.InvalidOperationException;
import com.hms.common.exceptions.ResourceNotFoundException;
import com.hms.common.exceptions.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
public class AppointmentServiceImpl implements AppointmentService {

  private static final String AGGREGATE_TYPE_APPOINTMENT = "Appointment";

  private final AppointmentRepository appointmentRepository;
  private final DoctorReadModelRepository doctorReadModelRepository;
  private final PatientReadModelRepository patientReadModelRepository;
  private final DoctorAvailabilityRepository availabilityRepository;
  private final DoctorUnavailabilityRepository unavailabilityRepository;
  private final WaitlistRepository waitlistRepository;
  private final RabbitTemplate rabbitTemplate;
  private final ProfileFeignClient profileFeignClient;
  private final UserFeignClient userFeignClient;
  private final OutboxEventRepository outboxEventRepository;
  private final ObjectMapper objectMapper;

  private final AppointmentService self;

  public AppointmentServiceImpl(
    AppointmentRepository appointmentRepository,
    DoctorReadModelRepository doctorReadModelRepository,
    PatientReadModelRepository patientReadModelRepository,
    DoctorAvailabilityRepository availabilityRepository,
    DoctorUnavailabilityRepository unavailabilityRepository,
    WaitlistRepository waitlistRepository,
    RabbitTemplate rabbitTemplate,
    ProfileFeignClient profileFeignClient,
    UserFeignClient userFeignClient,
    OutboxEventRepository outboxEventRepository,
    ObjectMapper objectMapper,
    @Lazy AppointmentService self
  ) {
    this.appointmentRepository = appointmentRepository;
    this.doctorReadModelRepository = doctorReadModelRepository;
    this.patientReadModelRepository = patientReadModelRepository;
    this.availabilityRepository = availabilityRepository;
    this.unavailabilityRepository = unavailabilityRepository;
    this.waitlistRepository = waitlistRepository;
    this.rabbitTemplate = rabbitTemplate;
    this.profileFeignClient = profileFeignClient;
    this.userFeignClient = userFeignClient;
    this.outboxEventRepository = outboxEventRepository;
    this.objectMapper = objectMapper;
    this.self = self;
  }

  @Value("${application.rabbitmq.exchange}")
  private String exchange;

  private DoctorReadModel getOrSyncDoctor(Long userIdInput) {
    return doctorReadModelRepository.findByUserId(userIdInput)
      .orElseGet(() -> {
        log.info("Médico com userId {} não encontrado localmente. Sincronizando via Profile Service...", userIdInput);
        try {
          ResponseWrapper<DoctorProfile> response = self.fetchDoctorByUserIdSafely(userIdInput);

          if (response == null || response.data() == null) {
            throw new ResourceNotFoundException("Doctor Profile (User ID)", userIdInput);
          }

          DoctorProfile ext = response.data();
          DoctorReadModel model = new DoctorReadModel();
          model.setDoctorId(ext.id());
          model.setUserId(ext.userId());
          model.setFullName(ext.name());
          model.setSpecialization(ext.specialization());

          return doctorReadModelRepository.save(model);
        } catch (Exception e) {
          log.error("Falha ao sincronizar médico userId {}: {}", userIdInput, e.getMessage());
          throw new ResourceNotFoundException("Doctor Profile (User ID)", userIdInput);
        }
      });
  }

  private PatientReadModel getOrSyncPatient(Long userIdInput) {
    return patientReadModelRepository.findByUserId(userIdInput)
      .orElseGet(() -> {
        log.info("Paciente com userId {} não encontrado localmente. Sincronizando via Profile Service...", userIdInput);
        try {
          ResponseWrapper<PatientProfile> response = self.fetchPatientByUserIdSafely(userIdInput);
          PatientProfile ext = response.data();

          if (ext == null || ext.id() == null) {
            throw new ResourceNotFoundException("Patient Profile (User ID)", userIdInput);
          }

          PatientReadModel model = new PatientReadModel();
          model.setPatientId(ext.id());
          model.setUserId(ext.userId());
          model.setFullName(ext.name());
          model.setEmail(ext.email());
          model.setPhoneNumber(ext.phoneNumber());

          return patientReadModelRepository.save(model);
        } catch (Exception e) {
          log.error("Falha ao sincronizar paciente userId {}: {}", userIdInput, e.getMessage());
          throw new ResourceNotFoundException("Patient Profile (User ID)", userIdInput);
        }
      });
  }

  private Long resolveDoctorId(Long requesterUserId) {
    return getOrSyncDoctor(requesterUserId).getDoctorId();
  }

  private Long resolvePatientId(Long requesterUserId) {
    return getOrSyncPatient(requesterUserId).getPatientId();
  }

  @Override
  @Transactional
  public AppointmentResponse createAppointment(Long patientUserId, AppointmentCreateRequest request) {
    PatientReadModel patient = getOrSyncPatient(patientUserId);

    DoctorReadModel doctor = doctorReadModelRepository.findById(request.doctorId())
      .orElseThrow(() -> new ResourceNotFoundException("Doctor Profile", request.doctorId()));

    int duration = request.duration() != null ? request.duration() : 60;
    LocalDateTime start = request.appointmentDateTime();
    LocalDateTime end = start.plusMinutes(duration);

    validateNewAppointment(patient.getPatientId(), doctor.getDoctorId(), start, end);

    Appointment appointment = new Appointment();
    appointment.setPatientId(patient.getPatientId());
    appointment.setDoctorId(doctor.getDoctorId());
    appointment.setAppointmentDateTime(start);
    appointment.setDuration(duration);
    appointment.setAppointmentEndTime(end);
    appointment.setReason(request.reason());
    appointment.setStatus(AppointmentStatus.SCHEDULED);

    if (request.type() == AppointmentType.ONLINE) {
      appointment.setType(AppointmentType.ONLINE);
      appointment.setMeetingUrl("https://meet.jit.si/hms-" + System.currentTimeMillis() + "-" + patient.getPatientId());
    } else {
      appointment.setType(AppointmentType.IN_PERSON);
    }

    Appointment saved = appointmentRepository.save(appointment);
    publishStatusEvent(saved, "SCHEDULED", null, patientUserId);
    scheduleReminder(saved);

    return AppointmentResponse.fromEntity(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public AppointmentResponse getAppointmentById(Long appointmentId, Long requesterUserId) {
    Appointment app = findAppointmentByIdOrThrow(appointmentId);
    validateAccess(app, requesterUserId);
    return AppointmentResponse.fromEntity(app);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<AppointmentResponse> getAppointmentsForPatient(Long userId, Pageable pageable) {
    Long patientId = resolvePatientId(userId);
    return appointmentRepository.findByPatientId(patientId, pageable)
      .map(AppointmentResponse::fromEntity);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<AppointmentResponse> getAppointmentsForDoctor(Long userId, Pageable pageable) {
    Long doctorId = resolveDoctorId(userId);
    return appointmentRepository.findByDoctorId(doctorId, pageable)
      .map(AppointmentResponse::fromEntity);
  }

  @Override
  @Transactional(readOnly = true)
  @Cacheable(value = "appointments", key = "#userId")
  public List<AppointmentDetailResponse> getAppointmentDetailsForDoctor(Long userId, String dateFilter) {
    DoctorReadModel doctor = getOrSyncDoctor(userId);

    List<Appointment> appointments;
    if (dateFilter == null || "all".equalsIgnoreCase(dateFilter)) {
      appointments = appointmentRepository.findByDoctorId(doctor.getDoctorId());
    } else {
      var range = calculateDateRange(dateFilter);
      appointments = appointmentRepository.findByDoctorIdAndAppointmentDateTimeBetween(
        doctor.getDoctorId(), range.start(), range.end());
    }

    if (appointments.isEmpty()) return Collections.emptyList();

    return appointments.stream()
      .map(app -> mapToDetailResponse(app, doctor))
      .toList();
  }

  @Override
  @Transactional
  public AppointmentResponse rescheduleAppointment(Long appointmentId, LocalDateTime newDateTime, Long requesterUserId) {
    Appointment app = findAppointmentByIdOrThrow(appointmentId);
    validateAccess(app, requesterUserId);

    if (app.getStatus() != AppointmentStatus.SCHEDULED)
      throw new InvalidOperationException("Apenas consultas agendadas podem ser remarcadas.");

    int duration = app.getDuration() != null ? app.getDuration() : 60;
    LocalDateTime newEnd = newDateTime.plusMinutes(duration);

    if (appointmentRepository.hasDoctorConflictExcludingId(app.getDoctorId(), newDateTime, newEnd, appointmentId))
      throw new InvalidOperationException("Conflito de horário com outra consulta existente.");

    validateAvailability(app.getDoctorId(), newDateTime, newEnd);

    LocalDateTime oldDate = app.getAppointmentDateTime();
    AuditChangeTracker.addChange("appointmentDateTime", oldDate, newDateTime);

    app.setAppointmentDateTime(newDateTime);
    app.setAppointmentEndTime(newEnd);
    app.setReminder24hSent(false);
    app.setReminder1hSent(false);

    Appointment saved = appointmentRepository.save(app);
    publishStatusEvent(saved, "RESCHEDULED", "De: " + oldDate, requesterUserId);
    checkAndNotifyWaitlist(saved.getDoctorId(), oldDate);
    scheduleReminder(saved);

    return AppointmentResponse.fromEntity(saved);
  }

  @Override
  @Transactional
  public AppointmentResponse cancelAppointment(Long appointmentId, Long requesterUserId) {
    Appointment app = findAppointmentByIdOrThrow(appointmentId);
    validateAccess(app, requesterUserId);

    if (app.getStatus() != AppointmentStatus.SCHEDULED)
      throw new InvalidOperationException("Status inválido para cancelamento. A consulta deve estar AGENDADA.");

    AuditChangeTracker.addChange("status", app.getStatus(), AppointmentStatus.CANCELED);
    app.setStatus(AppointmentStatus.CANCELED);

    Appointment saved = appointmentRepository.save(app);
    publishStatusEvent(saved, "CANCELED", "Solicitado pelo usuário", requesterUserId);
    checkAndNotifyWaitlist(app.getDoctorId(), app.getAppointmentDateTime());

    return AppointmentResponse.fromEntity(saved);
  }

  @Override
  @Transactional
  @CacheEvict(value = "appointments", key = "#requesterUserId")
  public AppointmentResponse completeAppointment(Long appointmentId, String notes, Long requesterUserId) {
    Appointment app = findAppointmentByIdOrThrow(appointmentId);

    Long doctorId = resolveDoctorId(requesterUserId);
    if (!app.getDoctorId().equals(doctorId)) {
      throw new AccessDeniedException("Apenas o médico responsável pode finalizar a consulta.");
    }

    if (app.getStatus() != AppointmentStatus.SCHEDULED && app.getStatus() != AppointmentStatus.COMPLETED)
      throw new InvalidOperationException("Status inválido para finalização.");

    app.setStatus(AppointmentStatus.COMPLETION_PENDING);
    app.setNotes(notes);
    Appointment saved = appointmentRepository.save(app);

    publishSagaStartEvent(saved);

    return AppointmentResponse.fromEntity(saved);
  }

  private void publishSagaStartEvent(Appointment app) {
    try {
      AppointmentCompletionStartedEvent event = AppointmentCompletionStartedEvent.builder()
        .appointmentId(app.getId())
        .patientId(app.getPatientId())
        .doctorId(app.getDoctorId())
        .status("COMPLETION_PENDING")
        .build();

      EventEnvelope<AppointmentCompletionStartedEvent> envelope = EventEnvelope.<AppointmentCompletionStartedEvent>builder()
        .eventId(UUID.randomUUID().toString())
        .eventType("APPOINTMENT_COMPLETION_STARTED")
        .occurredAt(LocalDateTime.now())
        .correlationId(String.valueOf(app.getId()))
        .payload(event)
        .build();

      OutboxEvent outboxEvent = OutboxEvent.builder()
        .id(UUID.randomUUID())
        .aggregateType(AGGREGATE_TYPE_APPOINTMENT)
        .aggregateId(app.getId().toString())
        .eventType("appointment.saga.started")
        .payload(objectMapper.writeValueAsString(envelope))
        .createdAt(LocalDateTime.now())
        .processed(false)
        .build();

      outboxEventRepository.save(outboxEvent);

    } catch (JsonProcessingException e) {
      log.error("Failed to serialize saga start event for appointment {}", app.getId(), e);
      throw new IllegalStateException("Error serializing outbox event", e);
    } catch (Exception e) {
      log.error("Failed to publish saga start event for appointment {}", app.getId(), e);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public AppointmentDetailResponse getAppointmentDetailsById(Long appointmentId, Long requesterUserId) {
    Appointment app = findAppointmentByIdOrThrow(appointmentId);
    validateAccess(app, requesterUserId);

    DoctorReadModel doctor = doctorReadModelRepository.findById(app.getDoctorId()).orElse(null);
    return mapToDetailResponse(app, doctor);
  }

  @Override
  @Transactional(readOnly = true)
  public AppointmentResponse getNextAppointmentForPatient(Long userId) {
    Long patientId = resolvePatientId(userId);
    return appointmentRepository
      .findFirstByPatientIdAndStatusAndAppointmentDateTimeAfterOrderByAppointmentDateTimeAsc(
        patientId, AppointmentStatus.SCHEDULED, LocalDateTime.now())
      .map(AppointmentResponse::fromEntity)
      .orElse(null);
  }

  @Override
  public AppointmentStatsResponse getAppointmentStatsForPatient(Long userId) {
    Long patientId = resolvePatientId(userId);
    List<Appointment> apps = appointmentRepository.findByPatientId(patientId);

    return new AppointmentStatsResponse(
      apps.size(),
      apps.stream().filter(a -> a.getStatus() == AppointmentStatus.SCHEDULED).count(),
      apps.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count(),
      apps.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELED).count()
    );
  }

  @Override
  public DoctorDashboardStatsResponse getDoctorDashboardStats(Long userId) {
    Long doctorId = resolveDoctorId(userId);

    long today = appointmentRepository.countAppointmentsForToday(doctorId);
    long weekCompleted = appointmentRepository.countCompletedAppointmentsSince(
      doctorId, LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay());

    Map<AppointmentStatus, Long> distribution = new EnumMap<>(AppointmentStatus.class);
    Arrays.stream(AppointmentStatus.values()).forEach(s -> distribution.put(s, 0L));
    appointmentRepository.countAppointmentsByStatus(doctorId)
      .forEach(row -> distribution.put((AppointmentStatus) row[0], (Long) row[1]));

    return new DoctorDashboardStatsResponse(today, weekCompleted, distribution);
  }

  @Override
  public long countUniquePatientsForDoctor(Long userId) {
    return appointmentRepository.countDistinctPatientsByDoctorId(resolveDoctorId(userId));
  }

  @Override
  public List<PatientGroupResponse> getPatientGroupsForDoctor(Long userId) {
    Long doctorId = resolveDoctorId(userId);

    var groups = Map.of(
      "Diabéticos", List.of("diabetes", "diabético", "glicemia"),
      "Hipertensos", List.of("hipertensão", "pressão alta", "has"),
      "Cardíacos", List.of("cardíaco", "cardiopatia", "infarto")
    );

    return groups.entrySet().stream()
      .map(entry -> {
        Set<Long> ids = new HashSet<>();
        entry.getValue().forEach(k ->
          ids.addAll(appointmentRepository.findDistinctPatientIdsByDoctorAndDiagnosisKeyword(doctorId, k)));
        return new PatientGroupResponse(entry.getKey(), ids.size());
      })
      .filter(g -> g.patientCount() > 0)
      .sorted((a, b) -> Long.compare(b.patientCount(), a.patientCount()))
      .toList();
  }

  @Override
  public List<DailyActivityDto> getDailyActivityStats() {
    LocalDateTime start = LocalDateTime.now().minusDays(30);

    Map<LocalDate, Long> appointments = mapQueryResults(
      appointmentRepository.countAppointmentsFromDateGroupedByDay(start));

    Map<LocalDate, Long> newPatients = appointmentRepository.findFirstAppointmentDateForPatients(start)
      .stream()
      .collect(Collectors.groupingBy(
        r -> {
          Object dateObj = r[1]; // a data está no r[1]
          if (dateObj instanceof java.sql.Date d) return d.toLocalDate();
          if (dateObj instanceof LocalDate d) return d;
          return LocalDate.parse(dateObj.toString());
        },
        Collectors.counting()
      ));

    return IntStream.range(0, 30)
      .mapToObj(i -> LocalDate.now().minusDays(i))
      .map(d -> new DailyActivityDto(d, newPatients.getOrDefault(d, 0L), appointments.getOrDefault(d, 0L)))
      .sorted(Comparator.comparing(DailyActivityDto::date))
      .toList();
  }

  @Override
  public long countAllAppointmentsForToday() {
    return appointmentRepository.countAllAppointmentsForToday();
  }

  @Override
  @Transactional(readOnly = true)
  public List<AppointmentResponse> getAppointmentsByPatientId(Long patientId) {
    return appointmentRepository.findByPatientId(patientId)
      .stream()
      .sorted(Comparator.comparing(Appointment::getAppointmentDateTime))
      .map(AppointmentResponse::fromEntity)
      .toList();
  }

  @Override
  @Transactional
  public void joinWaitlist(Long patientUserId, AppointmentCreateRequest request) {
    int duration = request.duration() != null ? request.duration() : 60;
    if (!appointmentRepository.hasDoctorConflict(
      request.doctorId(), request.appointmentDateTime(),
      request.appointmentDateTime().plusMinutes(duration))) {
      throw new InvalidOperationException(
        "O horário selecionado está disponível. Agende diretamente em vez de entrar na lista de espera.");
    }

    PatientReadModel patient = getOrSyncPatient(patientUserId);

    if (waitlistRepository.existsByPatientIdAndDoctorIdAndDate(
      patient.getPatientId(), request.doctorId(), request.appointmentDateTime().toLocalDate())) {
      throw new InvalidOperationException("Você já está na lista de espera para este dia.");
    }

    waitlistRepository.save(new WaitlistEntry(
      null,
      request.doctorId(),
      patient.getPatientId(),
      patient.getFullName(),
      patient.getEmail(),
      request.appointmentDateTime().toLocalDate(),
      LocalDateTime.now()
    ));
  }

  @Override
  public List<DoctorPatientSummaryDto> getPatientsForDoctor(Long userId) {
    Long doctorId = resolveDoctorId(userId);
    return appointmentRepository.findPatientsSummaryByDoctor(doctorId).stream()
      .map(p -> new DoctorPatientSummaryDto(
        p.getPatientId(), p.getUserId(), p.getPatientName(), p.getPatientEmail(),
        p.getTotalAppointments(), p.getLastAppointmentDate(),
        p.getLastAppointmentDate().isAfter(LocalDateTime.now().minusMonths(6)) ? "ACTIVE" : "INACTIVE",
        p.getProfilePicture()
      )).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<DoctorSummaryProjection> getMyDoctors(Long patientUserId) {
    Long patientId = resolvePatientId(patientUserId);
    return appointmentRepository.findDoctorsSummaryByPatient(patientId);
  }

  @Override
  @Transactional
  public AvailabilityResponse addAvailability(Long userId, AvailabilityRequest request) {
    Long doctorId = resolveDoctorId(userId);

    if (request.startTime().isAfter(request.endTime()))
      throw new InvalidOperationException("Início deve ser antes do fim.");

    boolean conflict = availabilityRepository.findByDoctorId(doctorId).stream()
      .filter(s -> s.getDayOfWeek() == request.dayOfWeek())
      .anyMatch(s -> request.startTime().isBefore(s.getEndTime()) && request.endTime().isAfter(s.getStartTime()));

    if (conflict) throw new InvalidOperationException("Conflito de horário.");

    DoctorAvailability saved = availabilityRepository.save(DoctorAvailability.builder()
      .doctorId(doctorId)
      .dayOfWeek(request.dayOfWeek())
      .startTime(request.startTime())
      .endTime(request.endTime())
      .build());

    return new AvailabilityResponse(saved.getId(), saved.getDayOfWeek(), saved.getStartTime(), saved.getEndTime());
  }

  @Override
  public List<AvailabilityResponse> getDoctorAvailability(Long userIdOrDoctorId) {
    Optional<DoctorReadModel> doctor = doctorReadModelRepository.findByUserId(userIdOrDoctorId);
    Long actualDoctorId = doctor.map(DoctorReadModel::getDoctorId).orElse(userIdOrDoctorId);

    return availabilityRepository.findByDoctorId(actualDoctorId).stream()
      .map(a -> new AvailabilityResponse(a.getId(), a.getDayOfWeek(), a.getStartTime(), a.getEndTime()))
      .toList();
  }

  @Override
  public void deleteAvailability(Long id) {
    if (!availabilityRepository.existsById(id))
      throw new ResourceNotFoundException("Doctor Availability", id);
    availabilityRepository.deleteById(id);
  }

  @Override
  public List<Long> getActiveDoctorIdsInLastHour() {
    return appointmentRepository
      .findByAppointmentDateTimeBetween(LocalDateTime.now().minusHours(1), LocalDateTime.now())
      .stream().map(Appointment::getDoctorId).distinct().toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> getAvailableTimeSlots(Long doctorId, LocalDate date, Integer duration) {
    DayOfWeek dayOfWeek = date.getDayOfWeek();

    List<DoctorAvailability> availabilities = availabilityRepository.findByDoctorId(doctorId).stream()
      .filter(a -> a.getDayOfWeek() == dayOfWeek)
      .toList();

    if (availabilities.isEmpty()) {
      return List.of();
    }

    int slotDurationMinutes = (duration != null && duration > 0) ? duration : 30;
    List<LocalTime> potentialSlots = new ArrayList<>();

    for (DoctorAvailability availability : availabilities) {
      LocalTime currentSlot = availability.getStartTime();
      while (!currentSlot.plusMinutes(slotDurationMinutes).isAfter(availability.getEndTime())) {
        potentialSlots.add(currentSlot);
        currentSlot = currentSlot.plusMinutes(slotDurationMinutes);
      }
    }

    List<Appointment> dailyAppointments = appointmentRepository.findByDoctorIdAndAppointmentDateTimeBetween(
      doctorId,
      date.atStartOfDay(),
      date.atTime(LocalTime.MAX)
    ).stream().filter(a -> a.getStatus() != AppointmentStatus.CANCELED).toList();

    List<String> availableSlots = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();

    for (LocalTime slot : potentialSlots) {
      LocalDateTime slotStart = LocalDateTime.of(date, slot);
      LocalDateTime slotEnd = slotStart.plusMinutes(slotDurationMinutes);

      if (slotStart.isBefore(now)) {
        continue;
      }

      boolean isBlocked = unavailabilityRepository.hasUnavailability(doctorId, slotStart, slotEnd);

      boolean hasAppointment = dailyAppointments.stream().anyMatch(a -> {
        LocalDateTime appStart = a.getAppointmentDateTime();
        LocalDateTime appEnd = a.getAppointmentEndTime();
        return slotStart.isBefore(appEnd) && slotEnd.isAfter(appStart);
      });

      if (!isBlocked && !hasAppointment) {
        availableSlots.add(slot.toString());
      }
    }

    return availableSlots;
  }

  private void validateNewAppointment(Long patientId, Long doctorId, LocalDateTime start, LocalDateTime end) {
    validateBusinessHours(start);

    if (start.isBefore(LocalDateTime.now().plusHours(2)))
      throw new InvalidOperationException("Agendamentos devem ser feitos com antecedência mínima de 2 horas.");

    if (start.isAfter(LocalDateTime.now().plusMonths(3)))
      throw new InvalidOperationException("Agendamentos permitidos apenas para os próximos 3 meses.");

    if (appointmentRepository.countByPatientIdAndDate(patientId, start.toLocalDate()) >= 2)
      throw new InvalidOperationException("Limite diário de agendamentos atingido para o paciente.");

    validateAvailability(doctorId, start, end);

    if (appointmentRepository.hasDoctorConflict(doctorId, start, end))
      throw new InvalidOperationException("O médico já possui um agendamento neste horário.");

    if (appointmentRepository.hasPatientConflict(patientId, start, end))
      throw new InvalidOperationException("Você já possui um agendamento neste horário.");
  }

  private void validateAvailability(Long doctorId, LocalDateTime start, LocalDateTime end) {
    if (unavailabilityRepository.hasUnavailability(doctorId, start, end))
      throw new InvalidOperationException("Médico indisponível (Bloqueio de agenda).");

    List<DoctorAvailability> slots = availabilityRepository.findByDoctorId(doctorId);
    if (slots.isEmpty()) return;

    boolean isCovered = slots.stream().anyMatch(slot ->
      slot.getDayOfWeek() == start.getDayOfWeek() &&
        !start.toLocalTime().isBefore(slot.getStartTime()) &&
        !end.toLocalTime().isAfter(slot.getEndTime()));

    if (!isCovered)
      throw new InvalidOperationException("O horário selecionado está fora do expediente do médico.");
  }

  private void validateBusinessHours(LocalDateTime date) {
    LocalTime t = date.toLocalTime();
    if (t.isBefore(LocalTime.of(6, 0)) || t.isAfter(LocalTime.of(22, 0)))
      throw new InvalidOperationException("Horário inválido. O sistema opera entre 06:00 e 22:00.");
  }

  // verifica se o requester é o paciente ou o médico envolvido na consulta
  private void validateAccess(Appointment app, Long requesterUserId) {
    boolean isPatientOwner = patientReadModelRepository.findByUserId(requesterUserId)
      .map(patient -> patient.getPatientId().equals(app.getPatientId()))
      .orElse(false);

    if (isPatientOwner) return;

    boolean isDoctorOwner = doctorReadModelRepository.findByUserId(requesterUserId)
      .map(doctor -> doctor.getDoctorId().equals(app.getDoctorId()))
      .orElse(false);

    if (isDoctorOwner) return;

    throw new AccessDeniedException("Você não tem permissão para acessar este agendamento.");
  }

  private Appointment findAppointmentByIdOrThrow(Long id) {
    return appointmentRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE_TYPE_APPOINTMENT, id));
  }

  private DateRange calculateDateRange(String filter) {
    LocalDateTime now = LocalDate.now().atStartOfDay();
    return switch (filter.toLowerCase()) {
      case "today" -> new DateRange(now, now.plusDays(1).minusNanos(1));
      case "week" -> new DateRange(
        now.with(DayOfWeek.MONDAY),
        now.with(DayOfWeek.SUNDAY).plusDays(1).minusNanos(1));
      case "month" -> new DateRange(
        now.withDayOfMonth(1),
        now.with(TemporalAdjusters.lastDayOfMonth()).plusDays(1).minusNanos(1));
      default -> new DateRange(now.minusYears(1), now.plusYears(1));
    };
  }

  private AppointmentDetailResponse mapToDetailResponse(Appointment app, DoctorReadModel doctor) {
    PatientReadModel p = patientReadModelRepository.findById(app.getPatientId())
      .orElse(new PatientReadModel(app.getPatientId(), null, "Paciente", "N/A", null, null));
    String docName = (doctor != null) ? doctor.getFullName() : "Dr. Desconhecido";
    return new AppointmentDetailResponse(
      app.getId(), app.getPatientId(), p.getFullName(), p.getPhoneNumber(),
      app.getDoctorId(), docName, app.getAppointmentDateTime(), app.getReason(), app.getStatus());
  }

  private Map<LocalDate, Long> mapQueryResults(List<Object[]> results) {
    return results.stream().collect(Collectors.toMap(
      r -> {
        Object dateObj = r[0];
        if (dateObj instanceof java.sql.Date d) return d.toLocalDate();
        if (dateObj instanceof LocalDate d) return d;
        return LocalDate.parse(dateObj.toString());
      },
      r -> ((Number) r[1]).longValue()
    ));
  }


  private void publishStatusEvent(Appointment app, String status, String notes, Long requesterUserId) {
    try {
      PatientReadModel patient = patientReadModelRepository.findById(app.getPatientId()).orElse(null);
      DoctorReadModel doctor = doctorReadModelRepository.findById(app.getDoctorId()).orElse(null);

      if (patient == null) {
        PatientProfile profile = self.fetchPatientByIdSafely(app.getPatientId());
        if (profile == null) {
          log.warn("Paciente {} não encontrado. Evento não publicado.", app.getPatientId());
          return;
        }
        PatientReadModel newModel = new PatientReadModel();
        newModel.setPatientId(profile.id());
        newModel.setUserId(profile.userId());
        newModel.setFullName(profile.name());
        newModel.setEmail(profile.email());
        newModel.setPhoneNumber(profile.phoneNumber());
        patient = patientReadModelRepository.save(newModel);
      }

      if (doctor == null) {
        log.warn("Médico {} não encontrado. Evento não publicado.", app.getDoctorId());
        return;
      }

      boolean triggeredByPatient = patient.getUserId() != null && patient.getUserId().equals(requesterUserId);

      AppointmentStatusChangedEvent event = new AppointmentStatusChangedEvent(
        app.getId(),
        app.getPatientId(),
        patient.getUserId(),
        doctor.getDoctorId(),
        doctor.getUserId(),
        patient.getEmail(),
        patient.getFullName(),
        doctor.getFullName(),
        app.getAppointmentDateTime(),
        status,
        notes,
        triggeredByPatient
      );

      EventEnvelope<AppointmentStatusChangedEvent> envelope = EventEnvelope.create(
        "APPOINTMENT_STATUS_CHANGED",
        UUID.randomUUID().toString(),
        event
      );

      OutboxEvent outboxEvent = OutboxEvent.builder()
        .id(UUID.randomUUID())
        .aggregateType(AGGREGATE_TYPE_APPOINTMENT)
        .aggregateId(app.getId().toString())
        .eventType("appointment.status.changed")
        .payload(objectMapper.writeValueAsString(envelope))
        .createdAt(LocalDateTime.now())
        .processed(false)
        .build();

      outboxEventRepository.save(outboxEvent);

    } catch (JsonProcessingException e) {
      log.error("Erro ao serializar evento de status: {}", e.getMessage());
    } catch (Exception e) {
      log.error("Erro ao publicar evento de status: {}", e.getMessage());
    }
  }

  private void scheduleReminder(Appointment app) {
    try {
      long delay = Duration.between(LocalDateTime.now(), app.getAppointmentDateTime().minusHours(24)).toMillis();
      if (delay <= 0) return;

      PatientReadModel patient = patientReadModelRepository.findById(app.getPatientId()).orElse(null);
      DoctorReadModel doctor = doctorReadModelRepository.findById(app.getDoctorId()).orElse(null);

      String patientName = patient != null ? patient.getFullName() : "Paciente";
      String patientEmail = resolvePatientEmail(patient);

      String doctorName = doctor != null ? doctor.getFullName() : "Médico";

      var event = new AppointmentEvent(
        app.getId(),
        app.getPatientId(),
        patient != null ? patient.getUserId() : null,
        patientName,
        patientEmail,
        doctorName,
        app.getAppointmentDateTime(),
        app.getMeetingUrl()
      );

      EventEnvelope<AppointmentEvent> envelope = EventEnvelope.create(
        "APPOINTMENT_REMINDER", String.valueOf(app.getId()), event);

      rabbitTemplate.convertAndSend(
        RabbitMQConfig.DELAYED_EXCHANGE,
        RabbitMQConfig.REMINDER_ROUTING_KEY,
        envelope,
        m -> {
          m.getMessageProperties().setHeader("x-delay", delay);
          return m;
        });
    } catch (Exception e) {
      log.error("Erro ao agendar lembrete: {}", e.getMessage());
    }
  }

  private String resolvePatientEmail(PatientReadModel patient) {
    String patientEmail = null;
    if (patient != null && patient.getUserId() != null) {
      try {
        var user = self.fetchUserByIdSafely(patient.getUserId());
        if (user != null) patientEmail = user.email();
      } catch (Exception e) {
        log.warn("Falha ao buscar e-mail via Feign para usuário {}: {}. Usando fallback.", patient.getUserId(), e.getMessage());
        patientEmail = patient.getEmail();
      }
    } else if (patient != null) {
      patientEmail = patient.getEmail();
    }
    return patientEmail;
  }

  private void checkAndNotifyWaitlist(Long doctorId, LocalDateTime date) {
    try {
      waitlistRepository.findFirstByDoctorIdAndDateOrderByCreatedAtAsc(doctorId, date.toLocalDate())
        .ifPresent(entry -> {
          try {
            DoctorReadModel doctor = doctorReadModelRepository.findById(doctorId).orElse(null);
            String doctorName = doctor != null ? doctor.getFullName() : "Médico";

            PatientReadModel patient = patientReadModelRepository.findById(entry.getPatientId()).orElse(null);
            Long userId = patient != null ? patient.getUserId() : null;

            WaitlistNotificationEvent event = new WaitlistNotificationEvent(
              userId,
              entry.getPatientEmail(),
              entry.getPatientName(),
              doctorName,
              date
            );

            EventEnvelope<WaitlistNotificationEvent> envelope = EventEnvelope.create(
              "WAITLIST_NOTIFICATION", String.valueOf(entry.getId()), event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
              .id(UUID.randomUUID())
              .aggregateType("WaitlistEntry")
              .aggregateId(entry.getId().toString())
              .eventType(RabbitMQConfig.WAITLIST_ROUTING_KEY)
              .payload(objectMapper.writeValueAsString(envelope))
              .createdAt(LocalDateTime.now())
              .processed(false)
              .build();

            outboxEventRepository.save(outboxEvent);
            waitlistRepository.delete(entry);
          } catch (JsonProcessingException e) {
            log.error("Erro ao serializar notificação de lista de espera: {}", e.getMessage());
            throw new IllegalStateException("Serialization error", e);
          }
        });
    } catch (Exception e) {
      log.error("Erro ao notificar lista de espera: {}", e.getMessage());
    }
  }

  @CircuitBreaker(name = "profileService", fallbackMethod = "fetchDoctorFallback")
  @Retry(name = "profileService")
  public ResponseWrapper<DoctorProfile> fetchDoctorByUserIdSafely(Long userId) {
    return profileFeignClient.getDoctorByUserId(userId);
  }

  @SuppressWarnings("unused")
  public ResponseWrapper<DoctorProfile> fetchDoctorFallback(Long userId, Throwable t) {
    log.error("Profile Service indisponível ao buscar Médico {}: {}", userId, t.getMessage());
    throw new ServiceUnavailableException("Serviço de perfis indisponível. Não foi possível validar o médico. Tente novamente mais tarde.");
  }

  @CircuitBreaker(name = "profileService", fallbackMethod = "fetchPatientFallback")
  @Retry(name = "profileService")
  public ResponseWrapper<PatientProfile> fetchPatientByUserIdSafely(Long userId) {
    return profileFeignClient.getPatientByUserId(userId);
  }

  @SuppressWarnings("unused")
  public ResponseWrapper<PatientProfile> fetchPatientFallback(Long userId, Throwable t) {
    log.error("Profile Service indisponível ao buscar Paciente por UserID {}: {}", userId, t.getMessage());
    throw new ServiceUnavailableException("Serviço de perfis indisponível. Tente novamente mais tarde.");
  }

  @CircuitBreaker(name = "profileService", fallbackMethod = "fetchPatientByIdFallback")
  @Retry(name = "profileService")
  public PatientProfile fetchPatientByIdSafely(Long patientId) {
    return profileFeignClient.getPatientById(patientId);
  }

  @SuppressWarnings("unused")
  public PatientProfile fetchPatientByIdFallback(Long patientId, Throwable t) {
    log.warn("Profile Service fora do ar. Retornando paciente null para ID {}: {}", patientId, t.getMessage());
    // retorna null para permitir que o processo continue, mas sem dados de contato. O lembrete será enviado sem e-mail se não houver cache.
    return null;
  }

  @CircuitBreaker(name = "userService", fallbackMethod = "fetchUserFallback")
  @Retry(name = "userService")
  public UserResponse fetchUserByIdSafely(Long userId) {
    return userFeignClient.getUserById(userId);
  }

  @SuppressWarnings("unused")
  public UserResponse fetchUserFallback(Long userId, Throwable t) {
    log.warn("User Service fora do ar. Fallback para UserID {}. O lembrete será enviado sem e-mail se não houver cache.", userId);
    // retorna null ou um Mock para não travar a emissão do lembrete RabbitMQ
    return null;
  }

  private record DateRange(LocalDateTime start, LocalDateTime end) {
  }
}

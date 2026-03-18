package com.hms.appointment.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hms.appointment.clients.ProfileFeignClient;
import com.hms.appointment.clients.UserFeignClient;
import com.hms.appointment.dto.request.AppointmentCreateRequest;
import com.hms.appointment.dto.response.AppointmentResponse;
import com.hms.appointment.entities.*;
import com.hms.appointment.enums.AppointmentStatus;
import com.hms.appointment.enums.AppointmentType;
import com.hms.appointment.repositories.*;
import com.hms.common.exceptions.AccessDeniedException;
import com.hms.common.exceptions.InvalidOperationException;
import com.hms.common.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

  @InjectMocks
  private AppointmentServiceImpl appointmentService;

  @Mock
  private AppointmentRepository appointmentRepository;
  @Mock
  private DoctorReadModelRepository doctorReadModelRepository;
  @Mock
  private PatientReadModelRepository patientReadModelRepository;
  @Mock
  private DoctorAvailabilityRepository availabilityRepository;
  @Mock
  private DoctorUnavailabilityRepository unavailabilityRepository;
  @Mock
  private WaitlistRepository waitlistRepository;
  @Mock
  private RabbitTemplate rabbitTemplate;
  @Mock
  private ProfileFeignClient profileFeignClient;
  @Mock
  private UserFeignClient userFeignClient;
  @Mock
  private OutboxEventRepository outboxEventRepository;
  @Mock
  private ObjectMapper objectMapper;

  private PatientReadModel mockPatient;
  private DoctorReadModel mockDoctor;

  @BeforeEach
  void setUp() {
    mockPatient = new PatientReadModel();
    mockPatient.setPatientId(1L);
    mockPatient.setUserId(100L);
    mockPatient.setFullName("João Paciente");

    mockDoctor = new DoctorReadModel();
    mockDoctor.setDoctorId(2L);
    mockDoctor.setUserId(200L);
    mockDoctor.setFullName("Dr. House");
  }

  @Test
  @DisplayName("Deve criar um agendamento com sucesso quando todas as regras forem atendidas")
  void createAppointment_Success() throws JsonProcessingException {
    Long patientUserId = 100L;
    AppointmentCreateRequest request = new AppointmentCreateRequest(
      2L,
      LocalDateTime.now().plusDays(2).withHour(14).withMinute(0),
      60,
      "Consulta de rotina",
      AppointmentType.ONLINE
    );

    when(patientReadModelRepository.findByUserId(anyLong())).thenReturn(Optional.of(mockPatient));
    when(patientReadModelRepository.findById(anyLong())).thenReturn(Optional.of(mockPatient));
    when(doctorReadModelRepository.findById(anyLong())).thenReturn(Optional.of(mockDoctor));

    DoctorAvailability availability = new DoctorAvailability();
    availability.setDayOfWeek(request.appointmentDateTime().getDayOfWeek());
    availability.setStartTime(LocalTime.MIN);
    availability.setEndTime(LocalTime.MAX);
    when(availabilityRepository.findByDoctorId(anyLong())).thenReturn(List.of(availability));

    when(unavailabilityRepository.hasUnavailability(anyLong(), any(), any())).thenReturn(false);
    when(appointmentRepository.hasDoctorConflict(anyLong(), any(), any())).thenReturn(false);
    when(appointmentRepository.hasPatientConflict(anyLong(), any(), any())).thenReturn(false);
    when(objectMapper.writeValueAsString(any())).thenReturn("{}");

    Appointment savedAppointment = new Appointment();
    savedAppointment.setId(1L);
    savedAppointment.setPatientId(1L);
    savedAppointment.setDoctorId(2L);
    savedAppointment.setAppointmentDateTime(request.appointmentDateTime());
    savedAppointment.setStatus(AppointmentStatus.SCHEDULED);
    when(appointmentRepository.save(any(Appointment.class))).thenReturn(savedAppointment);

    AppointmentResponse response = appointmentService.createAppointment(patientUserId, request);

    assertNotNull(response);
    assertEquals(1L, response.id());
    assertEquals(AppointmentStatus.SCHEDULED, response.status());
    verify(appointmentRepository, times(1)).save(any(Appointment.class));

    verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));

    verify(rabbitTemplate, atLeastOnce()).convertAndSend(
      any(), anyString(), any(com.hms.common.dto.event.EventEnvelope.class), any(org.springframework.amqp.core.MessagePostProcessor.class)
    );
  }

  @Test
  @DisplayName("Deve lançar erro ao tentar agendar com antecedência inferior ao SLA")
  void createAppointment_Fails_TooSoon() {
    // 30 minutos no futuro para forçar a quebra do SLA (2 horas).
    LocalDateTime tooSoonTime = LocalDateTime.now().plusMinutes(30);

    AppointmentCreateRequest request = new AppointmentCreateRequest(
      2L, tooSoonTime, 60, "Checkup", AppointmentType.IN_PERSON
    );

    when(patientReadModelRepository.findByUserId(100L)).thenReturn(Optional.of(mockPatient));
    when(doctorReadModelRepository.findById(2L)).thenReturn(Optional.of(mockDoctor));

    InvalidOperationException exception = assertThrows(
      InvalidOperationException.class,
      () -> appointmentService.createAppointment(100L, request)
    );

    // validação de mensagem para garantir que o erro seja relacionado ao SLA ou ao horário de funcionamento
    assertTrue(
      exception.getMessage().contains("antecedência mínima de 2 horas") ||
        exception.getMessage().contains("sistema opera entre 06:00 e 22:00")
    );
  }

  @Test
  @DisplayName("Deve lançar erro ao tentar agendar fora do horário de funcionamento")
  void createAppointment_Fails_OutsideBusinessHours() {
    // o sistema restringe agendamentos fora da janela operacional padrão da clínica (06:00 às 22:00)
    LocalDateTime earlyTime = LocalDateTime.now().plusDays(1).withHour(5).withMinute(0);
    AppointmentCreateRequest request = new AppointmentCreateRequest(
      2L, earlyTime, 60, "Checkup", AppointmentType.IN_PERSON
    );

    when(patientReadModelRepository.findByUserId(100L)).thenReturn(Optional.of(mockPatient));
    when(doctorReadModelRepository.findById(2L)).thenReturn(Optional.of(mockDoctor));

    InvalidOperationException exception = assertThrows(
      InvalidOperationException.class,
      () -> appointmentService.createAppointment(100L, request)
    );
    assertTrue(exception.getMessage().contains("sistema opera entre 06:00 e 22:00"));
  }

  @Test
  @DisplayName("Deve lançar erro se paciente exceder o limite de agendamentos no mesmo dia")
  void createAppointment_Fails_DailyLimitExceeded() {
    LocalDateTime validDateTime = LocalDateTime.now().plusDays(2).withHour(14).withMinute(0);
    AppointmentCreateRequest request = new AppointmentCreateRequest(
      2L, validDateTime, 60, "Checkup", AppointmentType.IN_PERSON
    );

    when(patientReadModelRepository.findByUserId(100L)).thenReturn(Optional.of(mockPatient));
    when(doctorReadModelRepository.findById(2L)).thenReturn(Optional.of(mockDoctor));

    // prevenção contra abusos e overbooking por parte de um único paciente (Corrigido para 2L)
    when(appointmentRepository.countByPatientIdAndDate(anyLong(), any())).thenReturn(2L);

    InvalidOperationException exception = assertThrows(
      InvalidOperationException.class,
      () -> appointmentService.createAppointment(100L, request)
    );
    assertTrue(exception.getMessage().contains("Limite diário de agendamentos atingido"));
  }

  @Test
  @DisplayName("Deve bloquear a criação se houver conflito de horário na agenda do médico")
  void createAppointment_Fails_DoctorConflict() {
    LocalDateTime validDateTime = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
    AppointmentCreateRequest request = new AppointmentCreateRequest(
      2L, validDateTime, 60, "Checkup", AppointmentType.IN_PERSON
    );

    when(patientReadModelRepository.findByUserId(100L)).thenReturn(Optional.of(mockPatient));
    when(doctorReadModelRepository.findById(2L)).thenReturn(Optional.of(mockDoctor));
    when(appointmentRepository.countByPatientIdAndDate(anyLong(), any())).thenReturn(0L);
    when(unavailabilityRepository.hasUnavailability(anyLong(), any(), any())).thenReturn(false);

    DoctorAvailability availability = new DoctorAvailability();
    availability.setDayOfWeek(validDateTime.getDayOfWeek());
    availability.setStartTime(LocalTime.of(8, 0));
    availability.setEndTime(LocalTime.of(18, 0));
    when(availabilityRepository.findByDoctorId(2L)).thenReturn(List.of(availability));
    when(appointmentRepository.hasDoctorConflict(anyLong(), any(), any())).thenReturn(true);

    InvalidOperationException exception = assertThrows(
      InvalidOperationException.class,
      () -> appointmentService.createAppointment(100L, request)
    );
    assertTrue(exception.getMessage().contains("O médico já possui um agendamento neste horário"));
  }

  @Test
  @DisplayName("Deve lançar exceção caso a entidade do médico não seja encontrada")
  void createAppointment_Fails_DoctorNotFound() {
    LocalDateTime validDateTime = LocalDateTime.now().plusDays(2).withHour(10);
    AppointmentCreateRequest request = new AppointmentCreateRequest(
      999L, validDateTime, 60, "Checkup", AppointmentType.IN_PERSON
    );

    when(patientReadModelRepository.findByUserId(100L)).thenReturn(Optional.of(mockPatient));
    when(doctorReadModelRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class,
      () -> appointmentService.createAppointment(100L, request)
    );
  }

  @Test
  @DisplayName("Deve remarcar a consulta com sucesso e liberar a vaga anterior para a lista de espera")
  void rescheduleAppointment_Success() {
    Long appointmentId = 1L;
    Long patientUserId = 100L;
    LocalDateTime oldDate = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
    LocalDateTime newDate = LocalDateTime.now().plusDays(3).withHour(14).withMinute(0);

    Appointment existingApp = new Appointment();
    existingApp.setId(appointmentId);
    existingApp.setPatientId(1L);
    existingApp.setDoctorId(2L);
    existingApp.setAppointmentDateTime(oldDate);
    existingApp.setDuration(60);
    existingApp.setStatus(AppointmentStatus.SCHEDULED);

    when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(existingApp));
    when(patientReadModelRepository.findByUserId(patientUserId)).thenReturn(Optional.of(mockPatient));
    when(appointmentRepository.hasDoctorConflictExcludingId(eq(2L), any(), any(), eq(appointmentId))).thenReturn(false);
    when(unavailabilityRepository.hasUnavailability(anyLong(), any(), any())).thenReturn(false);

    DoctorAvailability availability = new DoctorAvailability();
    availability.setDayOfWeek(newDate.getDayOfWeek());
    availability.setStartTime(LocalTime.of(8, 0));
    availability.setEndTime(LocalTime.of(18, 0));
    when(availabilityRepository.findByDoctorId(2L)).thenReturn(List.of(availability));

    when(appointmentRepository.save(any(Appointment.class))).thenReturn(existingApp);

    AppointmentResponse response = appointmentService.rescheduleAppointment(appointmentId, newDate, patientUserId);

    assertEquals(newDate, response.appointmentDateTime());
    verify(appointmentRepository, times(1)).save(any(Appointment.class));
    // verifica se checou a lista de espera para a data antiga que ficou vaga
    verify(waitlistRepository, times(1)).findFirstByDoctorIdAndDateOrderByCreatedAtAsc(2L, oldDate.toLocalDate());
  }

  @Test
  @DisplayName("Deve lançar erro ao tentar cancelar uma consulta que já foi concluída")
  void cancelAppointment_Fails_InvalidStatus() {
    Long appointmentId = 1L;
    Long patientUserId = 100L;

    Appointment completedApp = new Appointment();
    completedApp.setId(appointmentId);
    completedApp.setPatientId(1L);
    completedApp.setDoctorId(2L);
    completedApp.setStatus(AppointmentStatus.COMPLETED); // status inválido para cancelar

    when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(completedApp));
    when(patientReadModelRepository.findByUserId(patientUserId)).thenReturn(Optional.of(mockPatient));

    InvalidOperationException exception = assertThrows(
      InvalidOperationException.class,
      () -> appointmentService.cancelAppointment(appointmentId, patientUserId)
    );
    assertTrue(exception.getMessage().contains("Status inválido para cancelamento"));
  }

  @Test
  @DisplayName("Deve lançar AccessDeniedException se um médico tentar finalizar a consulta de outro médico")
  void completeAppointment_Fails_WrongDoctor() {
    Long appointmentId = 1L;
    Long intruderDoctorUserId = 999L; // um médico intruso tentando fechar a consulta

    Appointment app = new Appointment();
    app.setId(appointmentId);
    app.setDoctorId(2L); // o dono real é o médico 2
    app.setStatus(AppointmentStatus.SCHEDULED);

    DoctorReadModel intruderDoctor = new DoctorReadModel();
    intruderDoctor.setDoctorId(5L); // id diferente do médico responsável

    when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(app));
    when(doctorReadModelRepository.findByUserId(intruderDoctorUserId)).thenReturn(Optional.of(intruderDoctor));

    AccessDeniedException exception = assertThrows(
      AccessDeniedException.class,
      () -> appointmentService.completeAppointment(appointmentId, "Tudo certo", intruderDoctorUserId)
    );
    assertTrue(exception.getMessage().contains("Apenas o médico responsável pode finalizar a consulta"));
  }

  @Test
  @DisplayName("Deve calcular corretamente os horários disponíveis ignorando bloqueios e consultas agendadas")
  void getAvailableTimeSlots_CalculatesCorrectly() {
    Long doctorId = 2L;
    LocalDate futureDate = LocalDate.now().plusDays(5);

    DoctorAvailability morningShift = new DoctorAvailability();
    morningShift.setDayOfWeek(futureDate.getDayOfWeek());
    morningShift.setStartTime(LocalTime.of(8, 0));
    morningShift.setEndTime(LocalTime.of(12, 0));
    when(availabilityRepository.findByDoctorId(doctorId)).thenReturn(List.of(morningShift));

    // simula uma consulta já marcada
    Appointment existingApp = new Appointment();
    existingApp.setAppointmentDateTime(futureDate.atTime(9, 0));
    existingApp.setAppointmentEndTime(futureDate.atTime(10, 0));
    existingApp.setStatus(AppointmentStatus.SCHEDULED);
    when(appointmentRepository.findByDoctorIdAndAppointmentDateTimeBetween(anyLong(), any(), any()))
      .thenReturn(List.of(existingApp));

    when(unavailabilityRepository.hasUnavailability(eq(doctorId), any(LocalDateTime.class), any(LocalDateTime.class)))
      .thenAnswer(invocation -> {
        LocalDateTime start = invocation.getArgument(1);
        return start.toLocalTime().equals(LocalTime.of(11, 0));
      });

    List<String> slots = appointmentService.getAvailableTimeSlots(doctorId, futureDate, 60);

    assertEquals(2, slots.size(), "Devem sobrar exatamente 2 slots disponíveis");
    assertTrue(slots.contains("08:00"), "O slot das 08:00 deve estar livre");
    assertTrue(slots.contains("10:00"), "O slot das 10:00 deve estar livre");
    assertFalse(slots.contains("09:00"), "O slot das 09:00 deve estar ocupado pela consulta");
    assertFalse(slots.contains("11:00"), "O slot das 11:00 deve estar ocupado pelo bloqueio manual");
  }
}
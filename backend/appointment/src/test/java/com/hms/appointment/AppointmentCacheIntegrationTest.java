package com.hms.appointment;

import com.hms.appointment.entities.Appointment;
import com.hms.appointment.entities.DoctorReadModel;
import com.hms.appointment.enums.AppointmentStatus;
import com.hms.appointment.enums.AppointmentType;
import com.hms.appointment.repositories.AppointmentRepository;
import com.hms.appointment.repositories.DoctorReadModelRepository;
import com.hms.appointment.services.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

import java.time.LocalDateTime;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AppointmentCacheIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private AppointmentService appointmentService;

  @Autowired
  private CacheManager cacheManager;

  @Autowired
  private AppointmentRepository appointmentRepository;

  @Autowired
  private DoctorReadModelRepository doctorReadModelRepository;

  @BeforeEach
  void setUp() {
    appointmentRepository.deleteAll();
    doctorReadModelRepository.deleteAll();
    if (cacheManager.getCache("doctor_appointments") != null) {
      Objects.requireNonNull(cacheManager.getCache("doctor_appointments")).clear();
    }
  }

  @Test
  void shouldEvictCache_WhenAppointmentStatusIsUpdated() {
    String cacheName = "doctor_appointments";
    Long doctorUserId = 1L;

    stubFor(get(urlEqualTo("/profile/doctors/by-user/" + doctorUserId))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("""
              {
                  "success": true,
                  "message": "Doctor retrieved successfully",
                  "data": {
                      "id": 100,
                      "userId": 1,
                      "name": "Dr. Gregory House",
                      "specialization": "Diagnóstico"
                  }
              }
          """)));

    DoctorReadModel doctor = new DoctorReadModel();
    doctor.setDoctorId(100L);
    doctor.setUserId(doctorUserId);
    doctor.setFullName("Dr. Gregory House");
    doctorReadModelRepository.save(doctor);

    Appointment appointment = new Appointment();
    appointment.setDoctorId(100L);
    appointment.setPatientId(1L);
    appointment.setStatus(AppointmentStatus.SCHEDULED);
    appointment.setAppointmentDateTime(LocalDateTime.now().minusHours(1));
    appointment.setAppointmentEndTime(LocalDateTime.now());
    appointment.setDuration(60);
    appointment.setType(AppointmentType.IN_PERSON);
    Appointment saved = appointmentRepository.save(appointment);

    appointmentService.getAppointmentDetailsForDoctor(doctorUserId, "all");
    assertNotNull(cacheManager.getCache(cacheName));
    assertNotNull(Objects.requireNonNull(cacheManager.getCache(cacheName)).get(doctorUserId));

    appointmentService.completeAppointment(saved.getId(), "Consulta finalizada", doctorUserId);

    assertNull(Objects.requireNonNull(cacheManager.getCache(cacheName)).get(doctorUserId));
  }
}
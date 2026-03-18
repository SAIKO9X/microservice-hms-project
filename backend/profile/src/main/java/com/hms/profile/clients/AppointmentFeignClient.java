package com.hms.profile.clients;

import com.hms.common.config.FeignClientInterceptor;
import com.hms.common.dto.response.ResponseWrapper;
import com.hms.common.exceptions.ServiceUnavailableException;
import com.hms.profile.dto.response.AppointmentResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "appointment-service", configuration = FeignClientInterceptor.class)
public interface AppointmentFeignClient {

  Logger log = LoggerFactory.getLogger(AppointmentFeignClient.class);

  @CircuitBreaker(name = "appointmentService", fallbackMethod = "getActiveDoctorIdsFallback")
  @GetMapping("/admin/stats/active-doctors")
  List<Long> getActiveDoctorIds();

  default List<Long> getActiveDoctorIdsFallback(Throwable t) {
    log.error("Fallback triggered for getActiveDoctorIds: {}", t.getMessage());
    throw new ServiceUnavailableException("Appointment Service is unavailable");
  }

  @CircuitBreaker(name = "appointmentService", fallbackMethod = "getAppointmentHistoryForPatientFallback")
  @GetMapping("/appointments/history/patient/{patientId}")
  ResponseWrapper<List<AppointmentResponse>> getAppointmentHistoryForPatient(@PathVariable("patientId") Long patientId);

  default ResponseWrapper<List<AppointmentResponse>> getAppointmentHistoryForPatientFallback(Long patientId, Throwable t) {
    log.error("Fallback triggered for getAppointmentHistoryForPatient: {}", t.getMessage());
    throw new ServiceUnavailableException("Appointment Service is unavailable");
  }

  @CircuitBreaker(name = "appointmentService", fallbackMethod = "getAppointmentByIdFallback")
  @GetMapping("/appointments/{id}")
  ResponseWrapper<AppointmentResponse> getAppointmentById(@PathVariable("id") Long id);

  default ResponseWrapper<AppointmentResponse> getAppointmentByIdFallback(Long id, Throwable t) {
    log.error("Fallback triggered for getAppointmentById: {}", t.getMessage());
    throw new ServiceUnavailableException("Appointment Service is unavailable");
  }
}
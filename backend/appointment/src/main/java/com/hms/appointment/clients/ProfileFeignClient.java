package com.hms.appointment.clients;

import com.hms.appointment.dto.external.DoctorProfile;
import com.hms.appointment.dto.external.PatientProfile;
import com.hms.common.config.FeignClientInterceptor;
import com.hms.common.dto.response.ResponseWrapper;
import com.hms.common.exceptions.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
  name = "profile-service",
  url = "${feign.profile-service.url:}",
  configuration = FeignClientInterceptor.class
)
public interface ProfileFeignClient {

  Logger log = LoggerFactory.getLogger(ProfileFeignClient.class);

  @CircuitBreaker(name = "profileService", fallbackMethod = "getDoctorByUserIdFallback")
  @GetMapping("/profile/doctors/by-user/{userId}")
  ResponseWrapper<DoctorProfile> getDoctorByUserId(@PathVariable("userId") Long userId);

  default ResponseWrapper<DoctorProfile> getDoctorByUserIdFallback(Long userId, Throwable t) {
    log.error("Fallback triggered for getDoctorByUserId: {}", t.getMessage());
    throw new ServiceUnavailableException("Profile Service is unavailable");
  }

  @CircuitBreaker(name = "profileService", fallbackMethod = "getDoctorFallback")
  @GetMapping("/profile/doctors/{id}")
  ResponseWrapper<DoctorProfile> getDoctor(@PathVariable("id") Long id);

  default ResponseWrapper<DoctorProfile> getDoctorFallback(Long id, Throwable t) {
    log.error("Fallback triggered for getDoctor: {}", t.getMessage());
    throw new ServiceUnavailableException("Profile Service is unavailable");
  }

  @CircuitBreaker(name = "profileService", fallbackMethod = "getPatientFallback")
  @GetMapping("/profile/patients/{id}")
  PatientProfile getPatient(@PathVariable("id") Long id);

  default PatientProfile getPatientFallback(Long id, Throwable t) {
    log.error("Fallback triggered for getPatient: {}", t.getMessage());
    throw new ServiceUnavailableException("Profile Service is unavailable");
  }

  @CircuitBreaker(name = "profileService", fallbackMethod = "getPatientByUserIdFallback")
  @GetMapping("/profile/patients/by-user/{userId}")
  ResponseWrapper<PatientProfile> getPatientByUserId(@PathVariable("userId") Long userId);

  default ResponseWrapper<PatientProfile> getPatientByUserIdFallback(Long userId, Throwable t) {
    log.error("Fallback triggered for getPatientByUserId: {}", t.getMessage());
    throw new ServiceUnavailableException("Profile Service is unavailable");
  }

  @CircuitBreaker(name = "profileService", fallbackMethod = "getPatientByIdFallback")
  @GetMapping("/api/patients/{id}")
  PatientProfile getPatientById(@PathVariable("id") Long id);

  default PatientProfile getPatientByIdFallback(Long id, Throwable t) {
    log.error("Fallback triggered for getPatientById: {}", t.getMessage());
    throw new ServiceUnavailableException("Profile Service is unavailable");
  }
}
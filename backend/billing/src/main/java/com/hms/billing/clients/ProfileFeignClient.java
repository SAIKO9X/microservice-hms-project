package com.hms.billing.clients;

import com.hms.billing.dto.external.DoctorDTO;
import com.hms.billing.dto.external.PatientDTO;
import com.hms.common.config.FeignClientInterceptor;
import com.hms.common.dto.response.ResponseWrapper;
import com.hms.common.exceptions.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "profile-service", configuration = FeignClientInterceptor.class)
public interface ProfileFeignClient {

  Logger log = LoggerFactory.getLogger(ProfileFeignClient.class);

  @CircuitBreaker(name = "profileService", fallbackMethod = "getPatientFallback")
  @GetMapping("/profile/patients/{id}")
  ResponseWrapper<PatientDTO> getPatient(@PathVariable("id") Long id);

  default ResponseWrapper<PatientDTO> getPatientFallback(Long id, Throwable t) {
    log.error("Fallback triggered for getPatient: {}", t.getMessage());
    throw new ServiceUnavailableException("Profile Service is unavailable");
  }

  @CircuitBreaker(name = "profileService", fallbackMethod = "getDoctorFallback")
  @GetMapping("/profile/doctors/{id}")
  ResponseWrapper<DoctorDTO> getDoctor(@PathVariable("id") Long id);

  default ResponseWrapper<DoctorDTO> getDoctorFallback(Long id, Throwable t) {
    log.error("Fallback triggered for getDoctor: {}", t.getMessage());
    throw new ServiceUnavailableException("Profile Service is unavailable");
  }

  @CircuitBreaker(name = "profileService", fallbackMethod = "getPatientByUserIdFallback")
  @GetMapping("/profile/patients/user/{userId}")
  ResponseWrapper<PatientDTO> getPatientByUserId(@PathVariable("userId") Long userId);

  default ResponseWrapper<PatientDTO> getPatientByUserIdFallback(Long userId, Throwable t) {
    log.error("Fallback triggered for getPatientByUserId: {}", t.getMessage());
    throw new ServiceUnavailableException("Profile Service is unavailable");
  }
}
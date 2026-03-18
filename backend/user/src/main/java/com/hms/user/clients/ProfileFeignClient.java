package com.hms.user.clients;

import com.hms.common.dto.response.ResponseWrapper;
import com.hms.common.exceptions.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "profile-service")
public interface ProfileFeignClient {

  Logger log = LoggerFactory.getLogger(ProfileFeignClient.class);

  @CircuitBreaker(name = "profileService", fallbackMethod = "checkCpfExistsFallback")
  @GetMapping("/profile/patients/exists/cpf/{cpf}")
  ResponseWrapper<Boolean> checkCpfExists(@PathVariable("cpf") String cpf);

  default ResponseWrapper<Boolean> checkCpfExistsFallback(String cpf, Throwable t) {
    log.error("Fallback triggered for checkCpfExists: {}", t.getMessage());
    throw new ServiceUnavailableException("Profile Service is unavailable");
  }

  @CircuitBreaker(name = "profileService", fallbackMethod = "checkCrmExistsFallback")
  @GetMapping("/profile/doctors/exists/crm/{crm}")
  ResponseWrapper<Boolean> checkCrmExists(@PathVariable("crm") String crm);

  default ResponseWrapper<Boolean> checkCrmExistsFallback(String crm, Throwable t) {
    log.error("Fallback triggered for checkCrmExists: {}", t.getMessage());
    throw new ServiceUnavailableException("Profile Service is unavailable");
  }
}
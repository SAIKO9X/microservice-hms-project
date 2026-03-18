package com.hms.appointment.clients;

import com.hms.appointment.dto.external.UserResponse;
import com.hms.common.config.FeignClientInterceptor;
import com.hms.common.exceptions.ResourceNotFoundException;
import com.hms.common.exceptions.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
  name = "user-service",
  url = "${feign.user-service.url:}",
  configuration = FeignClientInterceptor.class
)
public interface UserFeignClient {

  Logger log = LoggerFactory.getLogger(UserFeignClient.class);

  @CircuitBreaker(name = "userService", fallbackMethod = "getUserByIdFallback")
  @GetMapping("/users/{id}")
  UserResponse getUserById(@PathVariable("id") Long id);

  default UserResponse getUserByIdFallback(Long id, Throwable t) {
    if (t instanceof ResourceNotFoundException) {
      throw (ResourceNotFoundException) t;
    }
    log.error("Fallback triggered for getUserById: {}", t.getMessage());
    throw new ServiceUnavailableException("User Service is unavailable");
  }
}
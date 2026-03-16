package com.hms.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class GatewayApplication {

  public static void main(String[] args) {
    SpringApplication.run(GatewayApplication.class, args);
  }

  // nean para identificar o IP da requisição
  @Bean
  public KeyResolver ipKeyResolver() {
    return exchange -> Mono.just(
      exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
    );
  }
}
package com.hms.appointment.services.impl;

import com.hms.appointment.entities.OutboxEvent;
import com.hms.appointment.repositories.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxRelayScheduler {

  private final OutboxEventRepository outboxEventRepository;
  private final RabbitTemplate rabbitTemplate;

  @Value("${application.rabbitmq.exchange}")
  private String exchangeName;

  @Scheduled(fixedDelay = 5000) // roda a cada 5 segundos
  @Transactional
  public void processOutboxEvents() {
    List<OutboxEvent> events = outboxEventRepository.findTop50ByProcessedFalseOrderByCreatedAtAsc();

    for (OutboxEvent event : events) {
      try {
        rabbitTemplate.convertAndSend(exchangeName, event.getEventType(), event.getPayload());

        // marca como processado APÓS o RabbitMQ confirmar o envio
        event.setProcessed(true);
        log.info("Outbox event {} processed successfully", event.getId());

      } catch (Exception e) {
        log.error("Failed to process outbox event {}. Will retry later.", event.getId(), e);
        // não marca como processado para tentar novamente na próxima execução
      }
    }

    if (!events.isEmpty()) {
      outboxEventRepository.saveAll(events);
    }
  }
}


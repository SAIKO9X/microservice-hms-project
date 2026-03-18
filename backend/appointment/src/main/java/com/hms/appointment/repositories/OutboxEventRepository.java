package com.hms.appointment.repositories;

import com.hms.appointment.entities.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
  // busca os eventos mais antigos que ainda não foram processados
  List<OutboxEvent> findTop50ByProcessedFalseOrderByCreatedAtAsc();
}


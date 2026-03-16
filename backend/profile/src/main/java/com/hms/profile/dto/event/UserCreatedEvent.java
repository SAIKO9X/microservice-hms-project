package com.hms.profile.dto.event;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hms.common.util.DataMaskingSerializer;

public record UserCreatedEvent(
  Long userId,
  String name,
  String email,
  String role,
  @JsonSerialize(using = DataMaskingSerializer.class)
  String cpf,      // Para Paciente
  String crm       // Para Médico
) {
}
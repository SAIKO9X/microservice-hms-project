package com.hms.notification.dto.event;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hms.common.util.DataMaskingSerializer;

public record UserCreatedEvent(
  String name,
  String email,
  @JsonSerialize(using = DataMaskingSerializer.class)
  String verificationCode
) {
}
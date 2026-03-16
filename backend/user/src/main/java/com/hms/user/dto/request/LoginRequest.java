package com.hms.user.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hms.common.util.DataMaskingSerializer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
  @Email(message = "O email deve ser válido")
  @NotBlank(message = "O email é obrigatório")
  String email,

  @NotBlank(message = "A senha é obrigatória")
  @JsonSerialize(using = DataMaskingSerializer.class)
  String password,

   String deviceId
) {
}

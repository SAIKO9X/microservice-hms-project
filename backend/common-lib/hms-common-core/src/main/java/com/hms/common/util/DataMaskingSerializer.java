package com.hms.common.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class DataMaskingSerializer extends JsonSerializer<String> {

  @Override
  public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    if (value == null || value.isBlank()) {
      gen.writeString(value);
      return;
    }

    String cleanValue = value.replaceAll("\\D", "");

    // se for CPF, mantém a formatação mascarada: ..***-12 (conforme solicitado, adaptado para manter consistência segura)
    if (cleanValue.length() == 11) {
      gen.writeString("..***-" + cleanValue.substring(9));
    } else {
      // Para qualquer outro campo anotado (senha, token), oculta totalmente
      gen.writeString("********");
    }
  }
}
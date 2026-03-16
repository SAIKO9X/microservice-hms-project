package com.hms.common.util;

import com.hms.common.exceptions.CryptoConversionException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

@Converter
@Component
public class DeterministicCryptoConverter implements AttributeConverter<String, String> {

  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final String HMAC_ALGO = "HmacSHA256";
  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 128;

  private static String secretKey;

  @Value("${app.security.encryption-key}")
  public void setSecretKey(String key) {
    DeterministicCryptoConverter.secretKey = key;
  }

  @Override
  public String convertToDatabaseColumn(String attribute) {
    if (attribute == null) {
      return null;
    }
    try {
      byte[] iv = generateDeterministicIV(attribute);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), "AES");
      GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

      cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);
      byte[] encrypted = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

      ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encrypted.length);
      byteBuffer.put(iv);
      byteBuffer.put(encrypted);

      return Base64.getEncoder().encodeToString(byteBuffer.array());
    } catch (GeneralSecurityException e) {
      throw new CryptoConversionException("Erro ao criptografar o dado de forma determinística", e);
    }
  }

  @Override
  public String convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    }
    try {
      byte[] decoded = Base64.getDecoder().decode(dbData);

      if (decoded.length < GCM_IV_LENGTH + GCM_TAG_LENGTH / 8) {
        return dbData;
      }

      byte[] iv = new byte[GCM_IV_LENGTH];
      System.arraycopy(decoded, 0, iv, 0, iv.length);

      byte[] encryptedBytes = new byte[decoded.length - GCM_IV_LENGTH];
      System.arraycopy(decoded, GCM_IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), "AES");
      GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

      cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);
      byte[] decrypted = cipher.doFinal(encryptedBytes);

      return new String(decrypted, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException | GeneralSecurityException e) {
      return dbData;
    }
  }

  private byte[] generateDeterministicIV(String data) throws GeneralSecurityException {
    Mac mac = Mac.getInstance(HMAC_ALGO);
    SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), HMAC_ALGO);
    mac.init(secretKeySpec);
    byte[] hmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

    byte[] iv = new byte[GCM_IV_LENGTH];
    System.arraycopy(hmac, 0, iv, 0, GCM_IV_LENGTH);
    return iv;
  }
}
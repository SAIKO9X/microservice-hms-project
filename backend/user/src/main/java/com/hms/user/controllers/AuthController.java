package com.hms.user.controllers;

import com.hms.common.dto.response.ResponseWrapper;
import com.hms.user.docs.AuthControllerDocs;
import com.hms.user.dto.request.ForgotPasswordRequest;
import com.hms.user.dto.request.LoginRequest;
import com.hms.user.dto.request.RefreshTokenRequest;
import com.hms.user.dto.request.ResetPasswordRequest;
import com.hms.user.dto.response.AuthResponse;
import com.hms.user.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController implements AuthControllerDocs {

  private final UserService userService;

  @PostMapping("/login")
  public ResponseEntity<ResponseWrapper<AuthResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
    String ipAddress = servletRequest.getHeader("X-Forwarded-For");
    if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
      ipAddress = servletRequest.getRemoteAddr();
    }

    AuthResponse response = userService.login(request, ipAddress);
    return ResponseEntity.ok(ResponseWrapper.success(response));
  }

  @PostMapping("/verify")
  public ResponseEntity<ResponseWrapper<Void>> verifyAccount(@RequestParam String email, @RequestParam String code) {
    userService.verifyAccount(email, code);
    return ResponseEntity.ok(ResponseWrapper.success(null, "Conta verificada com sucesso."));
  }

  @PostMapping("/resend-code")
  public ResponseEntity<ResponseWrapper<Void>> resendCode(@RequestParam String email) {
    userService.resendVerificationCode(email);
    return ResponseEntity.ok(ResponseWrapper.success(null, "Código de verificação reenviado."));
  }

  @PostMapping("/refresh")
  public ResponseEntity<ResponseWrapper<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
    AuthResponse response = userService.refreshToken(request.refreshToken());
    return ResponseEntity.ok(ResponseWrapper.success(response));
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<ResponseWrapper<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    userService.forgotPassword(request.email());
    return ResponseEntity.ok(ResponseWrapper.success(null, "Se o e-mail existir, um link de recuperação foi enviado."));
  }

  @PostMapping("/reset-password")
  public ResponseEntity<ResponseWrapper<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    userService.resetPassword(request.token(), request.newPassword());
    return ResponseEntity.ok(ResponseWrapper.success(null, "Senha alterada com sucesso. Você já pode fazer login."));
  }
}
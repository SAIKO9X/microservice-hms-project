package com.hms.user.services.impl;

import com.hms.common.dto.event.EventEnvelope;
import com.hms.common.exceptions.*;
import com.hms.user.clients.ProfileFeignClient;
import com.hms.user.dto.event.UserCreatedEvent;
import com.hms.user.dto.event.UserUpdatedEvent;
import com.hms.user.dto.request.AdminCreateUserRequest;
import com.hms.user.dto.request.AdminUpdateUserRequest;
import com.hms.user.dto.request.LoginRequest;
import com.hms.user.dto.request.UserRequest;
import com.hms.user.dto.response.AuthResponse;
import com.hms.user.dto.response.UserResponse;
import com.hms.user.entities.User;
import com.hms.user.enums.UserRole;
import com.hms.user.repositories.UserRepository;
import com.hms.user.services.JwtService;
import com.hms.user.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder encoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;
  private final RabbitTemplate rabbitTemplate;
  private final ProfileFeignClient profileFeignClient;

  @Value("${application.rabbitmq.exchange}")
  private String exchange;

  @Value("${application.frontend.url:http://localhost:5173}")
  private String frontendUrl;

  @Value("${application.rabbitmq.user-created-routing-key}")
  private String userCreatedRoutingKey;

  @Value("${application.rabbitmq.user-updated-routing-key:user.event.updated}")
  private String userUpdatedRoutingKey;

  @Value("${application.rabbitmq.password-reset-routing-key:user.event.password-reset}")
  private String passwordResetRoutingKey;

  @Override
  public AuthResponse login(LoginRequest request, String ipAddress) {
    User user = userRepository.findByEmail(request.email())
      .orElseThrow(InvalidCredentialsException::new);

    validateAccountLock(user);

    try {
      authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.email(), request.password())
      );
    } catch (BadCredentialsException ex) {
      handleFailedLogin(user);
      throw new InvalidCredentialsException();
    }

    if (!user.isActive()) {
      throw new IllegalStateException("Conta não verificada. Por favor, verifique seu e-mail.");
    }

    resetFailedAttempts(user);

    String currentDeviceId = request.deviceId() != null ? request.deviceId() : "Desconhecido";
    checkAndLogNewDevice(user, ipAddress, currentDeviceId);

    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);
    long expirationTime = jwtService.getExpirationTime();

    user.setRefreshToken(refreshToken);
    user.setLastIpAddress(ipAddress);
    user.setLastDeviceId(currentDeviceId);

    userRepository.save(user);

    return AuthResponse.create(UserResponse.fromEntity(user), expirationTime, accessToken, refreshToken);
  }

  private void validateAccountLock(User user) {
    if (user.getAccountLockedUntil() != null) {
      if (user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
        long minutesLeft = java.time.Duration.between(LocalDateTime.now(), user.getAccountLockedUntil()).toMinutes();
        throw new InvalidOperationException("Conta temporariamente bloqueada por excesso de tentativas. Tente novamente em " + (minutesLeft > 0 ? minutesLeft : 1) + " minuto(s).");
      } else {
        user.setAccountLockedUntil(null);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
      }
    }
  }

  private void handleFailedLogin(User user) {
    int attempts = user.getFailedLoginAttempts() + 1;
    user.setFailedLoginAttempts(attempts);

    if (attempts >= 5) {
      user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(15));
      userRepository.save(user);
      throw new InvalidOperationException("Muitas tentativas incorretas. Conta bloqueada por 15 minutos.");
    }
    userRepository.save(user);
  }

  private void resetFailedAttempts(User user) {
    if (user.getFailedLoginAttempts() > 0) {
      user.setFailedLoginAttempts(0);
      user.setAccountLockedUntil(null);
    }
  }

  private void checkAndLogNewDevice(User user, String ipAddress, String currentDeviceId) {
    boolean isNewDeviceOrIp = (user.getLastIpAddress() != null && !user.getLastIpAddress().equals(ipAddress)) ||
            (user.getLastDeviceId() != null && !user.getLastDeviceId().equals(currentDeviceId));

    if (isNewDeviceOrIp) {
      log.warn("Alerta de Segurança: Novo login detectado na sua conta. IP: {}", ipAddress);
    }
  }

  @Override
  public AuthResponse refreshToken(String incomingRefreshToken) {
    String userEmail = jwtService.extractUsername(incomingRefreshToken);

    if (userEmail == null) {
      throw new InvalidTokenException("Refresh token inválido ou malformado.");
    }

    User user = userRepository.findByEmail(userEmail)
      .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));

    if (!jwtService.isTokenValid(incomingRefreshToken, user.getEmail()) || !incomingRefreshToken.equals(user.getRefreshToken())) {
      throw new InvalidTokenException("Refresh token expirado ou revogado. Faça login novamente.");
    }

    // gerar novos tokens (Rotação de tokens)
    String newAccessToken = jwtService.generateAccessToken(user);
    String newRefreshToken = jwtService.generateRefreshToken(user);

    // atualizar o Refresh Token ativo no banco
    user.setRefreshToken(newRefreshToken);
    userRepository.save(user);

    return AuthResponse.create(UserResponse.fromEntity(user), jwtService.getExpirationTime(), newAccessToken, newRefreshToken);
  }

  @Override
  @Transactional
  public UserResponse createUser(UserRequest request) {
    validateEmailUnique(request.email(), null);

    validateCpfOrCrm(request.role(), request.cpfOuCrm());

    User user = request.toEntity();
    user.setPassword(encoder.encode(user.getPassword()));
    user.setActive(false);

    String code = generateVerificationCode();
    user.setVerificationCode(code);
    user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));

    User savedUser = userRepository.save(user);

    String cpf = (savedUser.getRole() == UserRole.PATIENT) ? request.cpfOuCrm() : null;
    String crm = (savedUser.getRole() == UserRole.DOCTOR) ? request.cpfOuCrm() : null;

    publishUserCreatedEvent(savedUser, cpf, crm, code);
    return UserResponse.fromEntity(savedUser);
  }

  @Override
  @Transactional(readOnly = true)
  @Cacheable(value = "users", key = "#id")
  public UserResponse getUserById(Long id) {
    return userRepository.findById(id)
      .map(UserResponse::fromEntity)
      .orElseThrow(() -> new ResourceNotFoundException("User", id));
  }

  @Override
  public UserResponse getUserByEmail(String email) {
    return userRepository.findByEmail(email)
      .map(UserResponse::fromEntity)
      .orElseThrow(() -> new ResourceNotFoundException("User", email));
  }

  @Override
  @Transactional
  @CachePut(value = "users", key = "#id")
  public UserResponse updateUser(Long id, UserRequest request) {
    User user = findUserByIdOrThrow(id);
    validateEmailUnique(request.email(), id);

    user.setName(request.name());
    user.setEmail(request.email());
    user.setRole(request.role());
    user.setPassword(encoder.encode(request.password()));

    return UserResponse.fromEntity(userRepository.save(user));
  }

  @Override
  @Transactional
  @CacheEvict(value = "users", key = "#id")
  public void updateUserStatus(Long id, boolean active) {
    User user = findUserByIdOrThrow(id);
    user.setActive(active);
    userRepository.save(user);
  }

  @Override
  @Transactional
  public UserResponse adminCreateUser(AdminCreateUserRequest request) {
    validateEmailUnique(request.email(), null);

    if (request.role() == UserRole.PATIENT) {
      validateCpfOrCrm(UserRole.PATIENT, request.cpf());
    } else if (request.role() == UserRole.DOCTOR) {
      validateCpfOrCrm(UserRole.DOCTOR, request.crmNumber());
    }

    User newUser = new User();
    newUser.setName(request.name());
    newUser.setEmail(request.email());
    newUser.setPassword(encoder.encode(request.password()));
    newUser.setRole(request.role());
    newUser.setActive(true);

    User savedUser = userRepository.save(newUser);
    publishUserCreatedEvent(savedUser, request.cpf(), request.crmNumber(), null);

    return UserResponse.fromEntity(savedUser);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<UserResponse> findAllUsers(Pageable pageable) {
    return userRepository.findAll(pageable).map(UserResponse::fromEntity);
  }

  @Override
  @Transactional
  @CacheEvict(value = "users", key = "#userId")
  public void adminUpdateUser(Long userId, AdminUpdateUserRequest request) {
    User user = findUserByIdOrThrow(userId);

    if (request.email() != null && !request.email().isBlank()) {
      validateEmailUnique(request.email(), userId);
      user.setEmail(request.email());
    }
    if (request.name() != null && !request.name().isBlank()) {
      user.setName(request.name());
    }

    userRepository.save(user);
    publishUserUpdatedEvent(user, request);
  }

  @Override
  @Transactional
  public void verifyAccount(String email, String code) {
    User user = userRepository.findByEmail(email)
      .orElseThrow(() -> new ResourceNotFoundException("User", email));

    if (user.isActive()) throw new IllegalArgumentException("Conta já verificada.");
    if (user.getVerificationCode() == null || !user.getVerificationCode().equals(code)) {
      throw new IllegalArgumentException("Código inválido.");
    }
    if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
      throw new IllegalArgumentException("Código expirado.");
    }

    user.setActive(true);
    user.setVerificationCode(null);
    user.setVerificationCodeExpiresAt(null);
    userRepository.save(user);
  }

  @Override
  @Transactional
  public void resendVerificationCode(String email) {
    User user = userRepository.findByEmail(email)
      .orElseThrow(() -> new ResourceNotFoundException("User", email));
    if (user.isActive()) throw new IllegalArgumentException("Conta já verificada.");

    String newCode = generateVerificationCode();
    user.setVerificationCode(newCode);
    user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
    userRepository.save(user);

    publishUserCreatedEvent(user, null, null, newCode);
  }

  @Override
  @Transactional
  public void forgotPassword(String email) {
    User user = userRepository.findByEmail(email)
      .orElseThrow(() -> new ResourceNotFoundException("Usuário", email));

    // --- PROTEÇÃO CONTRA SPAM ---
    LocalDateTime now = LocalDateTime.now();

    // se ele já pediu reset nas últimas 24 horas...
    if (user.getLastPasswordResetRequest() != null && user.getLastPasswordResetRequest().isAfter(now.minusHours(24))) {
      if (user.getPasswordResetRequests() >= 3) {
        throw new InvalidOperationException("Limite de solicitações excedido. Você só pode solicitar a recuperação de senha 3 vezes a cada 24 horas.");
      }
      user.setPasswordResetRequests(user.getPasswordResetRequests() + 1);
    } else {
      // passou de 24 horas, reseta o contador
      user.setPasswordResetRequests(1);
    }
    user.setLastPasswordResetRequest(now);

    // gera o token e salva no banco com expiração de 30 minutos
    String token = UUID.randomUUID().toString();
    user.setResetPasswordToken(token);
    user.setResetPasswordTokenExpiresAt(now.plusMinutes(30));
    userRepository.save(user);

    String resetLink = frontendUrl + "/reset-password?token=" + token;

    var event = new com.hms.user.dto.event.PasswordResetEvent(
      user.getEmail(),
      user.getName(),
      resetLink,
      30
    );

    EventEnvelope<com.hms.user.dto.event.PasswordResetEvent> envelope = EventEnvelope.create(
      "PASSWORD_RESET_REQUESTED",
      UUID.randomUUID().toString(),
      event
    );

    rabbitTemplate.convertAndSend(exchange, passwordResetRoutingKey, envelope);
    log.info("Evento PASSWORD_RESET enviado para usuário ID: {}", user.getId());
  }

  @Override
  @Transactional
  public void resetPassword(String token, String newPassword) {
    User user = userRepository.findByResetPasswordToken(token)
      .orElseThrow(() -> new InvalidOperationException("Token inválido ou não encontrado."));

    if (user.getResetPasswordTokenExpiresAt().isBefore(LocalDateTime.now())) {
      throw new InvalidOperationException("O token de recuperação expirou. Solicite um novo.");
    }

    // atualiza a senha e limpa os tokens
    user.setPassword(encoder.encode(newPassword));
    user.setResetPasswordToken(null);
    user.setResetPasswordTokenExpiresAt(null);

    // invalida o refresh token atual para deslogar de outros dispositivos por segurança
    user.setRefreshToken(null);

    userRepository.save(user);
  }

  private User findUserByIdOrThrow(Long id) {
    return userRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("User", id));
  }

  private void validateEmailUnique(String email, Long excludeId) {
    userRepository.findByEmail(email).ifPresent(u -> {
      if (!u.getId().equals(excludeId)) {
        throw new ResourceAlreadyExistsException("User", email);
      }
    });
  }

  private String generateVerificationCode() {
    return String.format("%06d", new Random().nextInt(999999));
  }

  private void publishUserCreatedEvent(User user, String cpf, String crm, String code) {
    try {
      var event = new UserCreatedEvent(user.getId(), user.getName(), user.getEmail(), user.getRole(), cpf, crm, code);

      EventEnvelope<UserCreatedEvent> envelope = EventEnvelope.create(
        "USER_CREATED",
        UUID.randomUUID().toString(),
        event
      );

      rabbitTemplate.convertAndSend(exchange, userCreatedRoutingKey, envelope);
      log.info("Evento USER_CREATED enviado para usuário ID: {}", user.getId());
    } catch (Exception e) {
      log.error("Erro RabbitMQ UserCreated: {}", e.getMessage());
    }
  }

  private void publishUserUpdatedEvent(User user, AdminUpdateUserRequest req) {
    try {
      var event = new UserUpdatedEvent(
        user.getId(), user.getName(), user.getEmail(), user.getRole(),
        req.phoneNumber(), req.dateOfBirth(), req.cpf(), req.address(), req.emergencyContactName(), req.emergencyContactPhone(),
        req.bloodGroup(), req.gender(), req.chronicDiseases(), req.allergies(), req.crmNumber(), req.specialization(),
        req.department(), req.biography(), req.qualifications(), req.yearsOfExperience()
      );

      EventEnvelope<UserUpdatedEvent> envelope = EventEnvelope.create(
        "USER_UPDATED",
        UUID.randomUUID().toString(),
        event
      );

      rabbitTemplate.convertAndSend(exchange, userUpdatedRoutingKey, envelope);
      log.info("Evento USER_UPDATED enviado para usuário ID: {}", user.getId());
    } catch (Exception e) {
      log.error("Erro RabbitMQ UserUpdated: {}", e.getMessage());
    }
  }

  private void validateCpfOrCrm(UserRole role, String cpfOuCrm) {
    if (cpfOuCrm == null || cpfOuCrm.isBlank()) return;

    try {
      if (role == UserRole.PATIENT) {
        if (Boolean.TRUE.equals(profileFeignClient.checkCpfExists(cpfOuCrm).data())) {
          throw new ResourceAlreadyExistsException("Paciente", "Já existe um cadastro utilizando este CPF.");
        }
      } else if (role == UserRole.DOCTOR) {
        if (Boolean.TRUE.equals(profileFeignClient.checkCrmExists(cpfOuCrm).data())) {
          throw new ResourceAlreadyExistsException("Médico", "Já existe um cadastro utilizando este CRM.");
        }
      }
    } catch (feign.FeignException e) {
      log.error("Erro ao comunicar com profile-service para checar CPF/CRM: {}", e.getMessage());
      throw new InvalidOperationException("Não foi possível validar o documento neste momento. Tente novamente em instantes.");
    }
  }
}


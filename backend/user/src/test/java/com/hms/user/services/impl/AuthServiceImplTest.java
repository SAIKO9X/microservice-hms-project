package com.hms.user.services.impl;

import com.hms.common.exceptions.InvalidCredentialsException;
import com.hms.common.exceptions.InvalidOperationException;
import com.hms.user.dto.request.LoginRequest;
import com.hms.user.dto.response.AuthResponse;
import com.hms.user.entities.User;
import com.hms.user.enums.UserRole;
import com.hms.user.repositories.UserRepository;
import com.hms.user.services.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private JwtService jwtService;

  @Mock
  private AuthenticationManager authenticationManager;

  @InjectMocks
  private UserServiceImpl userService;

  private User mockUser;
  private LoginRequest request;
  private final String IP_ADDRESS = "192.168.1.1";
  private final String DEVICE_ID = "device-xyz-123";

  @BeforeEach
  void setUp() {
    mockUser = new User();
    mockUser.setId(10L);
    mockUser.setEmail("medico@hms.com");
    mockUser.setRole(UserRole.DOCTOR);
    mockUser.setActive(true);
    mockUser.setFailedLoginAttempts(0);

    request = new LoginRequest("medico@hms.com", "senha123", DEVICE_ID);
  }

  @Test
  @DisplayName("Deve realizar login com sucesso, salvar IP/Device, zerar falhas e retornar Access e Refresh Tokens")
  void login_WithValidCredentials_ShouldReturnTokens() {
    // simulando que ele tinha 2 erros anteriores, mas agora acertou
    mockUser.setFailedLoginAttempts(2);

    when(userRepository.findByEmail("medico@hms.com")).thenReturn(Optional.of(mockUser));
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
    when(jwtService.generateAccessToken(mockUser)).thenReturn("access-token-123");
    when(jwtService.generateRefreshToken(mockUser)).thenReturn("refresh-token-123");
    when(jwtService.getExpirationTime()).thenReturn(900000L); // 15 min

    AuthResponse response = userService.login(request, IP_ADDRESS);

    assertNotNull(response);
    assertEquals("access-token-123", response.accessToken());
    assertEquals("refresh-token-123", response.refreshToken());

    // verifica se salvou o refresh token no banco e se zerou as falhas
    assertEquals("refresh-token-123", mockUser.getRefreshToken());
    assertEquals(0, mockUser.getFailedLoginAttempts());

    // verifica se o IP e DeviceID foram salvos corretamente (Fingerprint)
    assertEquals(IP_ADDRESS, mockUser.getLastIpAddress());
    assertEquals(DEVICE_ID, mockUser.getLastDeviceId());

    verify(userRepository, times(1)).save(mockUser);
  }

  @Test
  @DisplayName("Deve incrementar número de falhas ao errar a senha")
  void login_WithInvalidCredentials_ShouldIncrementFailures() {
    mockUser.setFailedLoginAttempts(1);

    when(userRepository.findByEmail("medico@hms.com")).thenReturn(Optional.of(mockUser));
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
      .thenThrow(new BadCredentialsException("Bad credentials"));

    assertThrows(InvalidCredentialsException.class, () -> userService.login(request, IP_ADDRESS));

    assertEquals(2, mockUser.getFailedLoginAttempts());
    assertNull(mockUser.getAccountLockedUntil()); // ainda não deve estar bloqueado
    verify(userRepository, times(1)).save(mockUser);
    verify(jwtService, never()).generateAccessToken(any());
  }

  @Test
  @DisplayName("Deve bloquear a conta na quinta tentativa incorreta de login")
  void login_FifthInvalidAttempt_ShouldLockAccount() {
    // o usuário já errou 4 vezes
    mockUser.setFailedLoginAttempts(4);

    when(userRepository.findByEmail("medico@hms.com")).thenReturn(Optional.of(mockUser));
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
      .thenThrow(new BadCredentialsException("Bad credentials"));

    InvalidOperationException exception = assertThrows(InvalidOperationException.class, () -> userService.login(request, IP_ADDRESS));

    assertTrue(exception.getMessage().contains("Conta bloqueada"));
    assertEquals(5, mockUser.getFailedLoginAttempts());
    assertNotNull(mockUser.getAccountLockedUntil());
    verify(userRepository, times(1)).save(mockUser);
  }

  @Test
  @DisplayName("Não deve tentar autenticar se a conta já estiver bloqueada no tempo atual")
  void login_WithAccountLocked_ShouldThrowExceptionImmediately() {
    // conta travada para os próximos 15 minutos
    mockUser.setAccountLockedUntil(LocalDateTime.now().plusMinutes(15));

    when(userRepository.findByEmail("medico@hms.com")).thenReturn(Optional.of(mockUser));

    InvalidOperationException exception = assertThrows(InvalidOperationException.class, () -> userService.login(request, IP_ADDRESS));

    assertTrue(exception.getMessage().contains("Conta temporariamente bloqueada"));

    // o AuthenticationManager NUNCA deve ser chamado se a conta está bloqueada
    verify(authenticationManager, never()).authenticate(any());
  }

  @Test
  @DisplayName("Deve liberar a conta se o tempo de bloqueio já tiver passado")
  void login_WithExpiredLock_ShouldUnlockAndAuthenticate() {
    // a conta foi bloqueada no passado e já deve ser liberada
    mockUser.setAccountLockedUntil(LocalDateTime.now().minusMinutes(5));
    mockUser.setFailedLoginAttempts(5);

    when(userRepository.findByEmail("medico@hms.com")).thenReturn(Optional.of(mockUser));
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
    when(jwtService.generateAccessToken(mockUser)).thenReturn("access-token-123");

    userService.login(request, IP_ADDRESS);

    // verifica se limpou a trava antes de seguir
    assertNull(mockUser.getAccountLockedUntil());
    assertEquals(0, mockUser.getFailedLoginAttempts());
    verify(authenticationManager, times(1)).authenticate(any());
  }

  @Test
  @DisplayName("Deve lançar erro genérico se o usuário não for encontrado")
  void login_UserNotFound_ShouldThrowInvalidCredentialsException() {
    when(userRepository.findByEmail("inexistente@hms.com")).thenReturn(Optional.empty());

    LoginRequest invalidReq = new LoginRequest("inexistente@hms.com", "senha123", DEVICE_ID);

    assertThrows(InvalidCredentialsException.class, () -> userService.login(invalidReq, IP_ADDRESS));
    verify(authenticationManager, never()).authenticate(any());
  }
}
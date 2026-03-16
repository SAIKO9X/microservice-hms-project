package com.hms.user.entities;

import com.hms.user.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tb_users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  @Column(unique = true)

  private String email;

  private String password;

  @Enumerated(EnumType.STRING)
  private UserRole role;

  private boolean active = false;

  private String verificationCode;

  private LocalDateTime verificationCodeExpiresAt;

  @Column(length = 512)
  private String refreshToken;

  @Column(length = 45)
  private String lastIpAddress;

  @Column(length = 255)
  private String lastDeviceId; // pode ser um hash do user-agent ou um identificador único do dispositivo

  @Column(length = 255)
  private String resetPasswordToken;

  private LocalDateTime resetPasswordTokenExpiresAt;

  private int failedLoginAttempts = 0;

  private LocalDateTime accountLockedUntil;

  private int passwordResetRequests = 0;

  private LocalDateTime lastPasswordResetRequest;
}

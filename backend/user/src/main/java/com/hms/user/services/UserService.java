package com.hms.user.services;

import com.hms.user.dto.request.AdminCreateUserRequest;
import com.hms.user.dto.request.AdminUpdateUserRequest;
import com.hms.user.dto.request.LoginRequest;
import com.hms.user.dto.request.UserRequest;
import com.hms.user.dto.response.AuthResponse;
import com.hms.user.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface UserService {
  UserResponse createUser(UserRequest request);

  UserResponse getUserById(Long id);

  UserResponse getUserByEmail(String email);

  void verifyAccount(String email, String code);

  void resendVerificationCode(String email);

  UserResponse updateUser(Long id, UserRequest request);

  AuthResponse login(LoginRequest request, String ipAddress);

  void updateUserStatus(Long id, boolean active);

  UserResponse adminCreateUser(AdminCreateUserRequest request);

  Page<UserResponse> findAllUsers(Pageable pageable);

  void adminUpdateUser(Long userId, AdminUpdateUserRequest request);

  AuthResponse refreshToken(String refreshToken);

  @Transactional
  void forgotPassword(String email);

  @Transactional
  void resetPassword(String token, String newPassword);

  @Transactional
  void changePassword(Long id, String oldPassword, String newPassword);

  Boolean checkCpfExistsSafely(String cpf);

  Boolean checkCrmExistsSafely(String crm);
}

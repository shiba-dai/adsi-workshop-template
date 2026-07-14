package com.example.attendance.service;

import com.example.attendance.dto.LoginRequest;
import com.example.attendance.entity.Employee;
import com.example.attendance.enums.Role;
import com.example.attendance.exception.AccountDisabledException;
import com.example.attendance.exception.AuthenticationFailedException;
import com.example.attendance.repository.EmployeeRepository;
import com.example.attendance.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(employeeRepository, passwordEncoder);
    }

    @Test
    @DisplayName("正しい認証情報でログインするとEmployeeResponseが返される")
    void login_validCredentials_returnsEmployeeResponse() {
        var employee = createEmployee(true);
        when(employeeRepository.findByEmail("tanaka@example.com")).thenReturn(Optional.of(employee));

        var request = new LoginRequest("tanaka@example.com", "password123");
        var result = authService.login(request);

        assertThat(result.name()).isEqualTo("田中太郎");
        assertThat(result.role()).isEqualTo("EMPLOYEE");
    }

    @Test
    @DisplayName("存在しないメールアドレスで認証失敗例外が発生する")
    void login_nonExistingEmail_throwsAuthenticationFailed() {
        when(employeeRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        var request = new LoginRequest("unknown@example.com", "password123");

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    @DisplayName("パスワード不一致で認証失敗例外が発生する")
    void login_wrongPassword_throwsAuthenticationFailed() {
        var employee = createEmployee(true);
        when(employeeRepository.findByEmail("tanaka@example.com")).thenReturn(Optional.of(employee));

        var request = new LoginRequest("tanaka@example.com", "wrongpassword");

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    @DisplayName("無効化されたアカウントでAccountDisabledException例外が発生する")
    void login_disabledAccount_throwsAccountDisabled() {
        var employee = createEmployee(false);
        when(employeeRepository.findByEmail("tanaka@example.com")).thenReturn(Optional.of(employee));

        var request = new LoginRequest("tanaka@example.com", "password123");

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(AccountDisabledException.class);
    }

    private Employee createEmployee(boolean enabled) {
        return Employee.builder()
            .id(1L)
            .employeeCode("EMP001")
            .name("田中太郎")
            .email("tanaka@example.com")
            .passwordHash(passwordEncoder.encode("password123"))
            .role(Role.EMPLOYEE)
            .enabled(enabled)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
}

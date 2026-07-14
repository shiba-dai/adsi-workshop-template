package com.example.attendance.controller;

import com.example.attendance.dto.EmployeeResponse;
import com.example.attendance.dto.LoginRequest;
import com.example.attendance.exception.AuthenticationFailedException;
import com.example.attendance.exception.GlobalExceptionHandler;
import com.example.attendance.security.SecurityConfig;
import com.example.attendance.security.SessionAuthenticationFilter;
import com.example.attendance.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, SessionAuthenticationFilter.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/auth/login 正常系: 200とEmployeeResponseが返る")
    void login_validCredentials_returns200() throws Exception {
        var response = new EmployeeResponse(1L, "EMP001", "田中太郎", "tanaka@example.com", "EMPLOYEE");
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("tanaka@example.com", "password123"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("田中太郎"))
            .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    @DisplayName("POST /api/auth/login 異常系: 401が返る")
    void login_invalidCredentials_returns401() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new AuthenticationFailedException());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("wrong@example.com", "wrong"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/auth/me 認証済み: 200とEmployeeResponseが返る")
    void me_authenticated_returns200() throws Exception {
        var employee = new EmployeeResponse(1L, "EMP001", "田中太郎", "tanaka@example.com", "EMPLOYEE");
        var session = new MockHttpSession();
        session.setAttribute("authenticatedEmployee", employee);

        mockMvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("田中太郎"));
    }

    @Test
    @DisplayName("GET /api/auth/me 未認証: 401が返る")
    void me_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/logout: 204が返る")
    void logout_returns204() throws Exception {
        var session = new MockHttpSession();
        session.setAttribute("authenticatedEmployee", new EmployeeResponse(1L, "EMP001", "田中太郎", "tanaka@example.com", "EMPLOYEE"));

        mockMvc.perform(post("/api/auth/logout").session(session))
            .andExpect(status().isNoContent());
    }
}

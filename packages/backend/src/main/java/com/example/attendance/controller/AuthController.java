package com.example.attendance.controller;

import com.example.attendance.dto.EmployeeResponse;
import com.example.attendance.dto.LoginRequest;
import com.example.attendance.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String SESSION_EMPLOYEE_KEY = "authenticatedEmployee";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<EmployeeResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        var employee = authService.login(request);
        var session = httpRequest.getSession(true);
        session.setAttribute(SESSION_EMPLOYEE_KEY, employee);

        var auth = new UsernamePasswordAuthenticationToken(
            employee,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + employee.role()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        return ResponseEntity.ok(employee);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<EmployeeResponse> me(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session == null) {
            return ResponseEntity.status(401).build();
        }
        var employee = (EmployeeResponse) session.getAttribute(SESSION_EMPLOYEE_KEY);
        if (employee == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(employee);
    }
}

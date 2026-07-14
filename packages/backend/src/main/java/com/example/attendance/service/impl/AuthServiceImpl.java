package com.example.attendance.service.impl;

import com.example.attendance.dto.EmployeeResponse;
import com.example.attendance.dto.LoginRequest;
import com.example.attendance.exception.AccountDisabledException;
import com.example.attendance.exception.AuthenticationFailedException;
import com.example.attendance.repository.EmployeeRepository;
import com.example.attendance.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public EmployeeResponse login(LoginRequest request) {
        var employee = employeeRepository.findByEmail(request.email())
            .orElseThrow(AuthenticationFailedException::new);

        if (!employee.isEnabled()) {
            throw new AccountDisabledException();
        }

        if (!passwordEncoder.matches(request.password(), employee.getPasswordHash())) {
            throw new AuthenticationFailedException();
        }

        return EmployeeResponse.from(employee);
    }
}

package com.example.attendance.service;

import com.example.attendance.dto.EmployeeResponse;
import com.example.attendance.dto.LoginRequest;

public interface AuthService {

    EmployeeResponse login(LoginRequest request);
}

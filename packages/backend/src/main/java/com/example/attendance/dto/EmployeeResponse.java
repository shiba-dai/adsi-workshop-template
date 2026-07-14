package com.example.attendance.dto;

import com.example.attendance.entity.Employee;

public record EmployeeResponse(
    Long id,
    String employeeCode,
    String name,
    String email,
    String role
) {
    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
            employee.getId(),
            employee.getEmployeeCode(),
            employee.getName(),
            employee.getEmail(),
            employee.getRole().name()
        );
    }
}

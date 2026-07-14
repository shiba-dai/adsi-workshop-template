package com.example.attendance.controller;

import com.example.attendance.dto.AttendanceResponse;
import com.example.attendance.dto.EmployeeResponse;
import com.example.attendance.service.AttendanceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private static final String SESSION_EMPLOYEE_KEY = "authenticatedEmployee";

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/clock-in")
    public ResponseEntity<AttendanceResponse> clockIn(HttpServletRequest request) {
        var employeeId = getEmployeeId(request);
        var response = attendanceService.clockIn(employeeId);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/clock-out")
    public ResponseEntity<AttendanceResponse> clockOut(HttpServletRequest request) {
        var employeeId = getEmployeeId(request);
        var response = attendanceService.clockOut(employeeId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/today")
    public ResponseEntity<AttendanceResponse> today(HttpServletRequest request) {
        var employeeId = getEmployeeId(request);
        return attendanceService.getToday(employeeId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/history")
    public ResponseEntity<List<AttendanceResponse>> history(
            @RequestParam int year,
            @RequestParam int month,
            HttpServletRequest request) {
        var employeeId = getEmployeeId(request);
        var history = attendanceService.getMonthlyHistory(employeeId, year, month);
        return ResponseEntity.ok(history);
    }

    private Long getEmployeeId(HttpServletRequest request) {
        var session = request.getSession(false);
        var employee = (EmployeeResponse) session.getAttribute(SESSION_EMPLOYEE_KEY);
        return employee.id();
    }
}

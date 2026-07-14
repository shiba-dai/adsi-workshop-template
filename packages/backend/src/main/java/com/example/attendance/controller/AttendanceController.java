package com.example.attendance.controller;

import com.example.attendance.dto.*;
import com.example.attendance.service.AttendanceService;
import com.example.attendance.service.BreakService;
import com.example.attendance.service.WorkingTimeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private static final String SESSION_EMPLOYEE_KEY = "authenticatedEmployee";

    private final AttendanceService attendanceService;
    private final BreakService breakService;
    private final WorkingTimeService workingTimeService;

    public AttendanceController(AttendanceService attendanceService,
                                BreakService breakService,
                                WorkingTimeService workingTimeService) {
        this.attendanceService = attendanceService;
        this.breakService = breakService;
        this.workingTimeService = workingTimeService;
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
    public ResponseEntity<List<AttendanceDetailResponse>> history(
            @RequestParam int year,
            @RequestParam int month,
            HttpServletRequest request) {
        var employeeId = getEmployeeId(request);
        var history = attendanceService.getMonthlyDetailHistory(employeeId, year, month);
        return ResponseEntity.ok(history);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AttendanceDetailResponse> updateAttendance(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAttendanceRequest body,
            HttpServletRequest request) {
        var employeeId = getEmployeeId(request);
        var response = attendanceService.updateAttendance(employeeId, id, body);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{attendanceId}/breaks")
    public ResponseEntity<List<BreakResponse>> getBreaks(
            @PathVariable Long attendanceId) {
        var breaks = breakService.getBreaks(attendanceId);
        return ResponseEntity.ok(breaks);
    }

    @PostMapping("/{attendanceId}/breaks")
    public ResponseEntity<BreakResponse> addBreak(
            @PathVariable Long attendanceId,
            @Valid @RequestBody CreateBreakRequest body,
            HttpServletRequest request) {
        var employeeId = getEmployeeId(request);
        var response = breakService.addBreak(employeeId, attendanceId, body);
        return ResponseEntity.status(201).body(response);
    }

    @DeleteMapping("/{attendanceId}/breaks/{breakId}")
    public ResponseEntity<Void> deleteBreak(
            @PathVariable Long attendanceId,
            @PathVariable Long breakId,
            HttpServletRequest request) {
        var employeeId = getEmployeeId(request);
        breakService.deleteBreak(employeeId, breakId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/overtime")
    public ResponseEntity<OvertimeSummaryResponse> overtime(
            @RequestParam int year,
            @RequestParam int month,
            HttpServletRequest request) {
        var employeeId = getEmployeeId(request);
        var summary = workingTimeService.getOvertimeSummary(employeeId, year, month);
        return ResponseEntity.ok(summary);
    }

    private Long getEmployeeId(HttpServletRequest request) {
        var session = request.getSession(false);
        var employee = (EmployeeResponse) session.getAttribute(SESSION_EMPLOYEE_KEY);
        return employee.id();
    }
}

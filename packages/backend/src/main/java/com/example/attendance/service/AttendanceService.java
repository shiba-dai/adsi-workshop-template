package com.example.attendance.service;

import com.example.attendance.dto.AttendanceDetailResponse;
import com.example.attendance.dto.AttendanceResponse;
import com.example.attendance.dto.UpdateAttendanceRequest;

import java.util.List;
import java.util.Optional;

public interface AttendanceService {

    AttendanceResponse clockIn(Long employeeId);

    AttendanceResponse clockOut(Long employeeId);

    Optional<AttendanceResponse> getToday(Long employeeId);

    List<AttendanceResponse> getMonthlyHistory(Long employeeId, int year, int month);

    AttendanceDetailResponse updateAttendance(Long employeeId, Long recordId, UpdateAttendanceRequest request);

    List<AttendanceDetailResponse> getMonthlyDetailHistory(Long employeeId, int year, int month);
}

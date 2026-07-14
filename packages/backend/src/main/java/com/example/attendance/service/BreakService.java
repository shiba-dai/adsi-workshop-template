package com.example.attendance.service;

import com.example.attendance.dto.BreakResponse;
import com.example.attendance.dto.CreateBreakRequest;

import java.util.List;

public interface BreakService {

    BreakResponse addBreak(Long employeeId, Long attendanceId, CreateBreakRequest request);

    void deleteBreak(Long employeeId, Long breakId);

    List<BreakResponse> getBreaks(Long attendanceId);
}

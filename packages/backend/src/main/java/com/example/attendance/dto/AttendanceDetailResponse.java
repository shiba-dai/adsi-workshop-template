package com.example.attendance.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AttendanceDetailResponse(
    Long id,
    LocalDate workDate,
    LocalDateTime clockInTime,
    LocalDateTime clockOutTime,
    int workingMinutes,
    int breakMinutes,
    int overtimeMinutes,
    List<BreakResponse> breaks,
    Long version
) {}

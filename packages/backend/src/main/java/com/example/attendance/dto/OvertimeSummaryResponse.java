package com.example.attendance.dto;

public record OvertimeSummaryResponse(
    int year,
    int month,
    int monthlyOvertimeMinutes,
    int yearlyOvertimeMinutes,
    boolean monthlyLimitWarning,
    boolean yearlyLimitWarning
) {}

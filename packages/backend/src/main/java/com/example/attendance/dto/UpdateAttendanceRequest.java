package com.example.attendance.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record UpdateAttendanceRequest(
    @NotNull LocalDateTime clockInTime,
    LocalDateTime clockOutTime,
    @NotNull Long version
) {}

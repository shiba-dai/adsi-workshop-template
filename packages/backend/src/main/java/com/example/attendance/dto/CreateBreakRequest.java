package com.example.attendance.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateBreakRequest(
    @NotNull LocalDateTime startTime,
    @NotNull LocalDateTime endTime
) {}

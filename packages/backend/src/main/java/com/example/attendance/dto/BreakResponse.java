package com.example.attendance.dto;

import com.example.attendance.entity.BreakRecord;

import java.time.Duration;
import java.time.LocalDateTime;

public record BreakResponse(
    Long id,
    LocalDateTime startTime,
    LocalDateTime endTime,
    int durationMinutes
) {
    public static BreakResponse from(BreakRecord record) {
        int duration = (int) Duration.between(record.getStartTime(), record.getEndTime()).toMinutes();
        return new BreakResponse(
            record.getId(),
            record.getStartTime(),
            record.getEndTime(),
            duration
        );
    }
}

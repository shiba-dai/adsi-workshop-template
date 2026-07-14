package com.example.attendance.dto;

import com.example.attendance.entity.AttendanceRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceResponse(
    Long id,
    LocalDate workDate,
    LocalDateTime clockInTime,
    LocalDateTime clockOutTime
) {
    public static AttendanceResponse from(AttendanceRecord record) {
        return new AttendanceResponse(
            record.getId(),
            record.getWorkDate(),
            record.getClockInTime(),
            record.getClockOutTime()
        );
    }
}

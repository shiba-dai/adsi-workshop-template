package com.example.attendance.service;

import com.example.attendance.dto.OvertimeSummaryResponse;
import com.example.attendance.entity.AttendanceRecord;
import com.example.attendance.entity.BreakRecord;

import java.util.List;

public interface WorkingTimeService {

    WorkDuration calculateDuration(AttendanceRecord record, List<BreakRecord> breaks);

    OvertimeSummaryResponse getOvertimeSummary(Long employeeId, int year, int month);

    record WorkDuration(
        int totalMinutes,
        int breakMinutes,
        int workingMinutes,
        int overtimeMinutes
    ) {}
}

package com.example.attendance.service.impl;

import com.example.attendance.dto.OvertimeSummaryResponse;
import com.example.attendance.entity.AttendanceRecord;
import com.example.attendance.entity.BreakRecord;
import com.example.attendance.repository.AttendanceRecordRepository;
import com.example.attendance.repository.BreakRecordRepository;
import com.example.attendance.service.WorkingTimeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class WorkingTimeServiceImpl implements WorkingTimeService {

    private static final int STANDARD_WORKING_MINUTES = 450;
    private static final int MONTHLY_OVERTIME_LIMIT_MINUTES = 2700;
    private static final int YEARLY_OVERTIME_LIMIT_MINUTES = 21600;
    private static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    private static final LocalTime LUNCH_END = LocalTime.of(13, 0);
    private static final int LUNCH_DURATION_MINUTES = 60;
    private static final int AUTO_BREAK_THRESHOLD_MINUTES = 360;

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final BreakRecordRepository breakRecordRepository;

    public WorkingTimeServiceImpl(AttendanceRecordRepository attendanceRecordRepository,
                                  BreakRecordRepository breakRecordRepository) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.breakRecordRepository = breakRecordRepository;
    }

    public WorkingTimeServiceImpl() {
        this.attendanceRecordRepository = null;
        this.breakRecordRepository = null;
    }

    @Override
    public WorkDuration calculateDuration(AttendanceRecord record, List<BreakRecord> breaks) {
        if (record.getClockOutTime() == null) {
            return new WorkDuration(0, 0, 0, 0);
        }

        int totalMinutes = (int) Duration.between(record.getClockInTime(), record.getClockOutTime()).toMinutes();
        int autoBreakMinutes = calculateAutoBreak(record);
        int manualBreakMinutes = breaks.stream()
            .mapToInt(b -> (int) Duration.between(b.getStartTime(), b.getEndTime()).toMinutes())
            .sum();
        int breakMinutes = autoBreakMinutes + manualBreakMinutes;
        int workingMinutes = totalMinutes - breakMinutes;
        int overtimeMinutes = Math.max(0, workingMinutes - STANDARD_WORKING_MINUTES);

        return new WorkDuration(totalMinutes, breakMinutes, workingMinutes, overtimeMinutes);
    }

    @Override
    public OvertimeSummaryResponse getOvertimeSummary(Long employeeId, int year, int month) {
        int monthlyOvertime = calculateMonthlyOvertime(employeeId, year, month);
        int yearlyOvertime = calculateYearlyOvertime(employeeId, year);

        return new OvertimeSummaryResponse(
            year,
            month,
            monthlyOvertime,
            yearlyOvertime,
            monthlyOvertime >= MONTHLY_OVERTIME_LIMIT_MINUTES,
            yearlyOvertime >= YEARLY_OVERTIME_LIMIT_MINUTES
        );
    }

    private int calculateAutoBreak(AttendanceRecord record) {
        LocalDateTime clockIn = record.getClockInTime();
        LocalDateTime clockOut = record.getClockOutTime();
        int totalMinutes = (int) Duration.between(clockIn, clockOut).toMinutes();

        if (totalMinutes <= AUTO_BREAK_THRESHOLD_MINUTES) {
            return 0;
        }

        LocalDateTime lunchStart = LocalDateTime.of(record.getWorkDate(), LUNCH_START);
        LocalDateTime lunchEnd = LocalDateTime.of(record.getWorkDate(), LUNCH_END);

        boolean overlapsLunch = clockIn.isBefore(lunchEnd) && clockOut.isAfter(lunchStart);
        return overlapsLunch ? LUNCH_DURATION_MINUTES : 0;
    }

    private int calculateMonthlyOvertime(Long employeeId, int year, int month) {
        var start = LocalDate.of(year, month, 1);
        var end = start.withDayOfMonth(start.lengthOfMonth());
        return calculateOvertimeForPeriod(employeeId, start, end);
    }

    private int calculateYearlyOvertime(Long employeeId, int year) {
        var start = LocalDate.of(year, 1, 1);
        var end = LocalDate.of(year, 12, 31);
        return calculateOvertimeForPeriod(employeeId, start, end);
    }

    private int calculateOvertimeForPeriod(Long employeeId, LocalDate start, LocalDate end) {
        var records = attendanceRecordRepository.findByEmployeeIdAndWorkDateBetween(employeeId, start, end);
        var recordIds = records.stream().map(AttendanceRecord::getId).toList();
        var allBreaks = breakRecordRepository.findByAttendanceRecordIdIn(recordIds);

        return records.stream()
            .mapToInt(record -> {
                var recordBreaks = allBreaks.stream()
                    .filter(b -> b.getAttendanceRecordId().equals(record.getId()))
                    .toList();
                return calculateDuration(record, recordBreaks).overtimeMinutes();
            })
            .sum();
    }
}

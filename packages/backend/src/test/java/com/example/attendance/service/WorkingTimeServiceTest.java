package com.example.attendance.service;

import com.example.attendance.entity.AttendanceRecord;
import com.example.attendance.entity.BreakRecord;
import com.example.attendance.service.impl.WorkingTimeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkingTimeServiceTest {

    private WorkingTimeService workingTimeService;

    @BeforeEach
    void setUp() {
        workingTimeService = new WorkingTimeServiceImpl();
    }

    @Test
    @DisplayName("9:00-18:00勤務で自動休憩60分控除、実働8時間、残業30分")
    void calculate_normalDay_returnsCorrectDuration() {
        var record = buildRecord(
            LocalDate.of(2026, 7, 1),
            LocalDateTime.of(2026, 7, 1, 9, 0),
            LocalDateTime.of(2026, 7, 1, 18, 0)
        );

        var result = workingTimeService.calculateDuration(record, List.of());

        assertThat(result.workingMinutes()).isEqualTo(480);
        assertThat(result.breakMinutes()).isEqualTo(60);
        assertThat(result.overtimeMinutes()).isEqualTo(30);
    }

    @Test
    @DisplayName("9:00-17:30勤務で自動休憩60分控除、実働7.5時間、残業0分")
    void calculate_exactStandardHours_noOvertime() {
        var record = buildRecord(
            LocalDate.of(2026, 7, 1),
            LocalDateTime.of(2026, 7, 1, 9, 0),
            LocalDateTime.of(2026, 7, 1, 17, 30)
        );

        var result = workingTimeService.calculateDuration(record, List.of());

        assertThat(result.workingMinutes()).isEqualTo(450);
        assertThat(result.breakMinutes()).isEqualTo(60);
        assertThat(result.overtimeMinutes()).isEqualTo(0);
    }

    @Test
    @DisplayName("13:00-18:00勤務（5時間）で自動控除なし、残業0分")
    void calculate_afternoonOnly_noAutoBreak() {
        var record = buildRecord(
            LocalDate.of(2026, 7, 1),
            LocalDateTime.of(2026, 7, 1, 13, 0),
            LocalDateTime.of(2026, 7, 1, 18, 0)
        );

        var result = workingTimeService.calculateDuration(record, List.of());

        assertThat(result.workingMinutes()).isEqualTo(300);
        assertThat(result.breakMinutes()).isEqualTo(0);
        assertThat(result.overtimeMinutes()).isEqualTo(0);
    }

    @Test
    @DisplayName("9:00-14:00勤務（5時間）で6時間未満のため自動控除なし")
    void calculate_lessThan6Hours_noAutoBreak() {
        var record = buildRecord(
            LocalDate.of(2026, 7, 1),
            LocalDateTime.of(2026, 7, 1, 9, 0),
            LocalDateTime.of(2026, 7, 1, 14, 0)
        );

        var result = workingTimeService.calculateDuration(record, List.of());

        assertThat(result.workingMinutes()).isEqualTo(300);
        assertThat(result.breakMinutes()).isEqualTo(0);
        assertThat(result.overtimeMinutes()).isEqualTo(0);
    }

    @Test
    @DisplayName("手動休憩15分がある場合、自動控除60分+手動15分=75分控除")
    void calculate_withManualBreak_addsToBothBreaks() {
        var record = buildRecord(
            LocalDate.of(2026, 7, 1),
            LocalDateTime.of(2026, 7, 1, 9, 0),
            LocalDateTime.of(2026, 7, 1, 18, 0)
        );

        var manualBreak = BreakRecord.builder()
            .id(1L)
            .attendanceRecordId(record.getId())
            .startTime(LocalDateTime.of(2026, 7, 1, 15, 0))
            .endTime(LocalDateTime.of(2026, 7, 1, 15, 15))
            .build();

        var result = workingTimeService.calculateDuration(record, List.of(manualBreak));

        assertThat(result.breakMinutes()).isEqualTo(75);
        assertThat(result.workingMinutes()).isEqualTo(465);
        assertThat(result.overtimeMinutes()).isEqualTo(15);
    }

    @Test
    @DisplayName("退勤時刻がnullの場合、全て0を返す")
    void calculate_noClockOut_returnsZero() {
        var record = buildRecord(
            LocalDate.of(2026, 7, 1),
            LocalDateTime.of(2026, 7, 1, 9, 0),
            null
        );

        var result = workingTimeService.calculateDuration(record, List.of());

        assertThat(result.workingMinutes()).isEqualTo(0);
        assertThat(result.breakMinutes()).isEqualTo(0);
        assertThat(result.overtimeMinutes()).isEqualTo(0);
    }

    @Test
    @DisplayName("11:00-12:30勤務で12:00-13:00に部分的に被るが6時間未満なので自動控除なし")
    void calculate_overlapsLunchButUnder6Hours_noAutoBreak() {
        var record = buildRecord(
            LocalDate.of(2026, 7, 1),
            LocalDateTime.of(2026, 7, 1, 11, 0),
            LocalDateTime.of(2026, 7, 1, 12, 30)
        );

        var result = workingTimeService.calculateDuration(record, List.of());

        assertThat(result.breakMinutes()).isEqualTo(0);
        assertThat(result.workingMinutes()).isEqualTo(90);
    }

    private AttendanceRecord buildRecord(LocalDate workDate, LocalDateTime clockIn, LocalDateTime clockOut) {
        return AttendanceRecord.builder()
            .id(1L)
            .employeeId(1L)
            .workDate(workDate)
            .clockInTime(clockIn)
            .clockOutTime(clockOut)
            .version(0L)
            .build();
    }
}

package com.example.attendance.service;

import com.example.attendance.entity.AttendanceRecord;
import com.example.attendance.exception.AlreadyClockedInException;
import com.example.attendance.exception.AlreadyClockedOutException;
import com.example.attendance.exception.NotClockedInException;
import com.example.attendance.repository.AttendanceRecordRepository;
import com.example.attendance.service.impl.AttendanceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceImplTest {

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    private AttendanceServiceImpl attendanceService;

    private final Long employeeId = 1L;

    @BeforeEach
    void setUp() {
        attendanceService = new AttendanceServiceImpl(attendanceRecordRepository);
    }

    @Test
    @DisplayName("出勤打刻: 未打刻の場合、レコードが作成される")
    void clockIn_noExistingRecord_createsRecord() {
        when(attendanceRecordRepository.findByEmployeeIdAndWorkDate(employeeId, LocalDate.now()))
            .thenReturn(Optional.empty());
        when(attendanceRecordRepository.save(any(AttendanceRecord.class)))
            .thenAnswer(inv -> {
                var record = inv.getArgument(0, AttendanceRecord.class);
                record.setId(1L);
                return record;
            });

        var result = attendanceService.clockIn(employeeId);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.workDate()).isEqualTo(LocalDate.now());
        assertThat(result.clockInTime()).isNotNull();
    }

    @Test
    @DisplayName("出勤打刻: 既に打刻済みの場合、AlreadyClockedInExceptionが発生する")
    void clockIn_alreadyClockedIn_throwsAlreadyClockedIn() {
        var existing = createRecord(null);
        when(attendanceRecordRepository.findByEmployeeIdAndWorkDate(employeeId, LocalDate.now()))
            .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> attendanceService.clockIn(employeeId))
            .isInstanceOf(AlreadyClockedInException.class);
    }

    @Test
    @DisplayName("退勤打刻: 出勤済み・未退勤の場合、レコードが更新される")
    void clockOut_clockedInNotOut_updatesRecord() {
        var existing = createRecord(null);
        when(attendanceRecordRepository.findByEmployeeIdAndWorkDate(employeeId, LocalDate.now()))
            .thenReturn(Optional.of(existing));
        when(attendanceRecordRepository.save(any(AttendanceRecord.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        var result = attendanceService.clockOut(employeeId);

        assertThat(result.clockOutTime()).isNotNull();
    }

    @Test
    @DisplayName("退勤打刻: 未出勤の場合、NotClockedInExceptionが発生する")
    void clockOut_notClockedIn_throwsNotClockedIn() {
        when(attendanceRecordRepository.findByEmployeeIdAndWorkDate(employeeId, LocalDate.now()))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.clockOut(employeeId))
            .isInstanceOf(NotClockedInException.class);
    }

    @Test
    @DisplayName("退勤打刻: 既に退勤済みの場合、AlreadyClockedOutExceptionが発生する")
    void clockOut_alreadyClockedOut_throwsAlreadyClockedOut() {
        var existing = createRecord(LocalDateTime.now().minusHours(1));
        when(attendanceRecordRepository.findByEmployeeIdAndWorkDate(employeeId, LocalDate.now()))
            .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> attendanceService.clockOut(employeeId))
            .isInstanceOf(AlreadyClockedOutException.class);
    }

    @Test
    @DisplayName("本日の打刻: レコードが存在する場合、レスポンスが返される")
    void getToday_existingRecord_returnsResponse() {
        var existing = createRecord(null);
        when(attendanceRecordRepository.findByEmployeeIdAndWorkDate(employeeId, LocalDate.now()))
            .thenReturn(Optional.of(existing));

        var result = attendanceService.getToday(employeeId);

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("本日の打刻: レコードが存在しない場合、空が返される")
    void getToday_noRecord_returnsEmpty() {
        when(attendanceRecordRepository.findByEmployeeIdAndWorkDate(employeeId, LocalDate.now()))
            .thenReturn(Optional.empty());

        var result = attendanceService.getToday(employeeId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("月次履歴: 指定月のレコードが返される")
    void getMonthlyHistory_returnsRecordsForMonth() {
        var records = List.of(
            createRecordForDate(LocalDate.of(2026, 7, 1)),
            createRecordForDate(LocalDate.of(2026, 7, 15))
        );
        when(attendanceRecordRepository.findByEmployeeIdAndWorkDateBetween(
            employeeId, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
            .thenReturn(records);

        var result = attendanceService.getMonthlyHistory(employeeId, 2026, 7);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("月次履歴: レコードがない場合、空リストが返される")
    void getMonthlyHistory_noRecords_returnsEmptyList() {
        when(attendanceRecordRepository.findByEmployeeIdAndWorkDateBetween(
            employeeId, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
            .thenReturn(List.of());

        var result = attendanceService.getMonthlyHistory(employeeId, 2026, 7);

        assertThat(result).isEmpty();
    }

    private AttendanceRecord createRecord(LocalDateTime clockOutTime) {
        return AttendanceRecord.builder()
            .id(1L)
            .employeeId(employeeId)
            .workDate(LocalDate.now())
            .clockInTime(LocalDateTime.now().minusHours(8))
            .clockOutTime(clockOutTime)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    private AttendanceRecord createRecordForDate(LocalDate date) {
        return AttendanceRecord.builder()
            .id(1L)
            .employeeId(employeeId)
            .workDate(date)
            .clockInTime(date.atTime(9, 0))
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
}

package com.example.attendance.service;

import com.example.attendance.dto.CreateBreakRequest;
import com.example.attendance.entity.AttendanceRecord;
import com.example.attendance.entity.BreakRecord;
import com.example.attendance.exception.BusinessException;
import com.example.attendance.repository.AttendanceRecordRepository;
import com.example.attendance.repository.BreakRecordRepository;
import com.example.attendance.service.impl.BreakServiceImpl;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BreakServiceImplTest {

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @Mock
    private BreakRecordRepository breakRecordRepository;

    private BreakService breakService;

    @BeforeEach
    void setUp() {
        breakService = new BreakServiceImpl(attendanceRecordRepository, breakRecordRepository);
    }

    @Test
    @DisplayName("有効な休憩を追加すると休憩レスポンスが返される")
    void addBreak_validRequest_returnsBreakResponse() {
        var record = buildAttendanceRecord(1L, 1L,
            LocalDateTime.of(2026, 7, 1, 9, 0),
            LocalDateTime.of(2026, 7, 1, 18, 0));
        when(attendanceRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(breakRecordRepository.findByAttendanceRecordId(1L)).thenReturn(List.of());
        when(breakRecordRepository.save(any())).thenAnswer(inv -> {
            var br = (BreakRecord) inv.getArgument(0);
            br.setId(10L);
            return br;
        });

        var request = new CreateBreakRequest(
            LocalDateTime.of(2026, 7, 1, 15, 0),
            LocalDateTime.of(2026, 7, 1, 15, 15)
        );

        var result = breakService.addBreak(1L, 1L, request);

        assertThat(result.durationMinutes()).isEqualTo(15);
        assertThat(result.startTime()).isEqualTo(request.startTime());
    }

    @Test
    @DisplayName("他人の勤怠に休憩を追加しようとすると403エラー")
    void addBreak_otherEmployee_throwsForbidden() {
        var record = buildAttendanceRecord(1L, 2L,
            LocalDateTime.of(2026, 7, 1, 9, 0),
            LocalDateTime.of(2026, 7, 1, 18, 0));
        when(attendanceRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        var request = new CreateBreakRequest(
            LocalDateTime.of(2026, 7, 1, 15, 0),
            LocalDateTime.of(2026, 7, 1, 15, 15)
        );

        assertThatThrownBy(() -> breakService.addBreak(1L, 1L, request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("権限");
    }

    @Test
    @DisplayName("休憩開始が終了以降だとバリデーションエラー")
    void addBreak_startAfterEnd_throwsBadRequest() {
        var record = buildAttendanceRecord(1L, 1L,
            LocalDateTime.of(2026, 7, 1, 9, 0),
            LocalDateTime.of(2026, 7, 1, 18, 0));
        when(attendanceRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        var request = new CreateBreakRequest(
            LocalDateTime.of(2026, 7, 1, 15, 30),
            LocalDateTime.of(2026, 7, 1, 15, 0)
        );

        assertThatThrownBy(() -> breakService.addBreak(1L, 1L, request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("開始時刻より後");
    }

    @Test
    @DisplayName("勤務時間外の休憩はバリデーションエラー")
    void addBreak_outsideWorkingHours_throwsBadRequest() {
        var record = buildAttendanceRecord(1L, 1L,
            LocalDateTime.of(2026, 7, 1, 9, 0),
            LocalDateTime.of(2026, 7, 1, 18, 0));
        when(attendanceRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        var request = new CreateBreakRequest(
            LocalDateTime.of(2026, 7, 1, 18, 0),
            LocalDateTime.of(2026, 7, 1, 18, 30)
        );

        assertThatThrownBy(() -> breakService.addBreak(1L, 1L, request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("勤務時間内");
    }

    @Test
    @DisplayName("既存休憩と時間が重複する場合はエラー")
    void addBreak_overlapsExisting_throwsConflict() {
        var record = buildAttendanceRecord(1L, 1L,
            LocalDateTime.of(2026, 7, 1, 9, 0),
            LocalDateTime.of(2026, 7, 1, 18, 0));
        when(attendanceRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        var existing = BreakRecord.builder()
            .id(5L)
            .attendanceRecordId(1L)
            .startTime(LocalDateTime.of(2026, 7, 1, 15, 0))
            .endTime(LocalDateTime.of(2026, 7, 1, 15, 15))
            .build();
        when(breakRecordRepository.findByAttendanceRecordId(1L)).thenReturn(List.of(existing));

        var request = new CreateBreakRequest(
            LocalDateTime.of(2026, 7, 1, 15, 10),
            LocalDateTime.of(2026, 7, 1, 15, 30)
        );

        assertThatThrownBy(() -> breakService.addBreak(1L, 1L, request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("重複");
    }

    @Test
    @DisplayName("休憩削除が成功する")
    void deleteBreak_ownRecord_succeeds() {
        var breakRecord = BreakRecord.builder()
            .id(5L)
            .attendanceRecordId(1L)
            .startTime(LocalDateTime.of(2026, 7, 1, 15, 0))
            .endTime(LocalDateTime.of(2026, 7, 1, 15, 15))
            .build();
        var attendance = buildAttendanceRecord(1L, 1L,
            LocalDateTime.of(2026, 7, 1, 9, 0),
            LocalDateTime.of(2026, 7, 1, 18, 0));

        when(breakRecordRepository.findById(5L)).thenReturn(Optional.of(breakRecord));
        when(attendanceRecordRepository.findById(1L)).thenReturn(Optional.of(attendance));

        breakService.deleteBreak(1L, 5L);

        verify(breakRecordRepository).delete(breakRecord);
    }

    private AttendanceRecord buildAttendanceRecord(Long id, Long employeeId,
                                                    LocalDateTime clockIn, LocalDateTime clockOut) {
        return AttendanceRecord.builder()
            .id(id)
            .employeeId(employeeId)
            .workDate(clockIn.toLocalDate())
            .clockInTime(clockIn)
            .clockOutTime(clockOut)
            .version(0L)
            .build();
    }
}

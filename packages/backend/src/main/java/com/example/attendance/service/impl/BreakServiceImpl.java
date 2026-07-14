package com.example.attendance.service.impl;

import com.example.attendance.dto.BreakResponse;
import com.example.attendance.dto.CreateBreakRequest;
import com.example.attendance.entity.BreakRecord;
import com.example.attendance.exception.BusinessException;
import com.example.attendance.repository.AttendanceRecordRepository;
import com.example.attendance.repository.BreakRecordRepository;
import com.example.attendance.service.BreakService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BreakServiceImpl implements BreakService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final BreakRecordRepository breakRecordRepository;

    public BreakServiceImpl(AttendanceRecordRepository attendanceRecordRepository,
                            BreakRecordRepository breakRecordRepository) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.breakRecordRepository = breakRecordRepository;
    }

    @Override
    public BreakResponse addBreak(Long employeeId, Long attendanceId, CreateBreakRequest request) {
        var attendance = attendanceRecordRepository.findById(attendanceId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND",
                "指定された勤怠記録が見つかりません"));

        if (!attendance.getEmployeeId().equals(employeeId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                "この勤怠記録を修正する権限がありません");
        }

        validateBreakTimes(request, attendance);
        validateNoOverlap(request, attendanceId);

        var breakRecord = BreakRecord.builder()
            .attendanceRecordId(attendanceId)
            .startTime(request.startTime())
            .endTime(request.endTime())
            .build();

        var saved = breakRecordRepository.save(breakRecord);
        return BreakResponse.from(saved);
    }

    @Override
    public void deleteBreak(Long employeeId, Long breakId) {
        var breakRecord = breakRecordRepository.findById(breakId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND",
                "指定された休憩記録が見つかりません"));

        var attendance = attendanceRecordRepository.findById(breakRecord.getAttendanceRecordId())
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND",
                "指定された勤怠記録が見つかりません"));

        if (!attendance.getEmployeeId().equals(employeeId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                "この勤怠記録を修正する権限がありません");
        }

        breakRecordRepository.delete(breakRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BreakResponse> getBreaks(Long attendanceId) {
        return breakRecordRepository.findByAttendanceRecordId(attendanceId).stream()
            .map(BreakResponse::from)
            .toList();
    }

    private void validateBreakTimes(CreateBreakRequest request,
                                    com.example.attendance.entity.AttendanceRecord attendance) {
        if (!request.startTime().isBefore(request.endTime())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_BREAK_TIME",
                "休憩終了時刻は開始時刻より後にしてください");
        }

        if (request.startTime().isBefore(attendance.getClockInTime())
            || request.endTime().isAfter(attendance.getClockOutTime())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "BREAK_OUTSIDE_WORKING_HOURS",
                "休憩は勤務時間内に設定してください");
        }
    }

    private void validateNoOverlap(CreateBreakRequest request, Long attendanceId) {
        var existingBreaks = breakRecordRepository.findByAttendanceRecordId(attendanceId);
        boolean hasOverlap = existingBreaks.stream().anyMatch(existing ->
            request.startTime().isBefore(existing.getEndTime())
                && request.endTime().isAfter(existing.getStartTime())
        );

        if (hasOverlap) {
            throw new BusinessException(HttpStatus.CONFLICT, "BREAK_OVERLAP",
                "指定の時間帯は既存の休憩と重複しています");
        }
    }
}

package com.example.attendance.service.impl;

import com.example.attendance.dto.AttendanceDetailResponse;
import com.example.attendance.dto.AttendanceResponse;
import com.example.attendance.dto.BreakResponse;
import com.example.attendance.dto.UpdateAttendanceRequest;
import com.example.attendance.entity.AttendanceRecord;
import com.example.attendance.exception.AlreadyClockedInException;
import com.example.attendance.exception.AlreadyClockedOutException;
import com.example.attendance.exception.BusinessException;
import com.example.attendance.exception.NotClockedInException;
import com.example.attendance.repository.AttendanceRecordRepository;
import com.example.attendance.repository.BreakRecordRepository;
import com.example.attendance.service.AttendanceService;
import com.example.attendance.service.WorkingTimeService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final BreakRecordRepository breakRecordRepository;
    private final WorkingTimeService workingTimeService;

    public AttendanceServiceImpl(AttendanceRecordRepository attendanceRecordRepository,
                                 BreakRecordRepository breakRecordRepository,
                                 WorkingTimeService workingTimeService) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.breakRecordRepository = breakRecordRepository;
        this.workingTimeService = workingTimeService;
    }

    @Override
    public AttendanceResponse clockIn(Long employeeId) {
        var today = LocalDate.now();
        var existing = attendanceRecordRepository.findByEmployeeIdAndWorkDate(employeeId, today);

        if (existing.isPresent()) {
            throw new AlreadyClockedInException();
        }

        var record = AttendanceRecord.builder()
            .employeeId(employeeId)
            .workDate(today)
            .clockInTime(LocalDateTime.now())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        var saved = attendanceRecordRepository.save(record);
        return AttendanceResponse.from(saved);
    }

    @Override
    public AttendanceResponse clockOut(Long employeeId) {
        var today = LocalDate.now();
        var record = attendanceRecordRepository.findByEmployeeIdAndWorkDate(employeeId, today)
            .orElseThrow(NotClockedInException::new);

        if (record.getClockOutTime() != null) {
            throw new AlreadyClockedOutException();
        }

        record.setClockOutTime(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        var saved = attendanceRecordRepository.save(record);
        return AttendanceResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AttendanceResponse> getToday(Long employeeId) {
        var today = LocalDate.now();
        return attendanceRecordRepository.findByEmployeeIdAndWorkDate(employeeId, today)
            .map(AttendanceResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getMonthlyHistory(Long employeeId, int year, int month) {
        var start = LocalDate.of(year, month, 1);
        var end = start.withDayOfMonth(start.lengthOfMonth());
        return attendanceRecordRepository.findByEmployeeIdAndWorkDateBetween(employeeId, start, end)
            .stream()
            .map(AttendanceResponse::from)
            .toList();
    }

    @Override
    public AttendanceDetailResponse updateAttendance(Long employeeId, Long recordId, UpdateAttendanceRequest request) {
        var record = attendanceRecordRepository.findById(recordId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND",
                "指定された勤怠記録が見つかりません"));

        if (!record.getEmployeeId().equals(employeeId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                "この勤怠記録を修正する権限がありません");
        }

        if (!record.getVersion().equals(request.version())) {
            throw new BusinessException(HttpStatus.CONFLICT, "CONFLICT",
                "他のユーザーが更新しました。再度お試しください");
        }

        record.setClockInTime(request.clockInTime());
        record.setClockOutTime(request.clockOutTime());
        record.setUpdatedAt(LocalDateTime.now());

        var saved = attendanceRecordRepository.save(record);
        return toDetailResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDetailResponse> getMonthlyDetailHistory(Long employeeId, int year, int month) {
        var start = LocalDate.of(year, month, 1);
        var end = start.withDayOfMonth(start.lengthOfMonth());
        var records = attendanceRecordRepository.findByEmployeeIdAndWorkDateBetween(employeeId, start, end);
        var recordIds = records.stream().map(AttendanceRecord::getId).toList();
        var allBreaks = breakRecordRepository.findByAttendanceRecordIdIn(recordIds);

        return records.stream()
            .map(record -> {
                var recordBreaks = allBreaks.stream()
                    .filter(b -> b.getAttendanceRecordId().equals(record.getId()))
                    .toList();
                var duration = workingTimeService.calculateDuration(record, recordBreaks);
                var breakResponses = recordBreaks.stream().map(BreakResponse::from).toList();
                return new AttendanceDetailResponse(
                    record.getId(),
                    record.getWorkDate(),
                    record.getClockInTime(),
                    record.getClockOutTime(),
                    duration.workingMinutes(),
                    duration.breakMinutes(),
                    duration.overtimeMinutes(),
                    breakResponses,
                    record.getVersion()
                );
            })
            .toList();
    }

    private AttendanceDetailResponse toDetailResponse(AttendanceRecord record) {
        var breaks = breakRecordRepository.findByAttendanceRecordId(record.getId());
        var duration = workingTimeService.calculateDuration(record, breaks);
        var breakResponses = breaks.stream().map(BreakResponse::from).toList();
        return new AttendanceDetailResponse(
            record.getId(),
            record.getWorkDate(),
            record.getClockInTime(),
            record.getClockOutTime(),
            duration.workingMinutes(),
            duration.breakMinutes(),
            duration.overtimeMinutes(),
            breakResponses,
            record.getVersion()
        );
    }
}

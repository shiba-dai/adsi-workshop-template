package com.example.attendance.service.impl;

import com.example.attendance.dto.AttendanceResponse;
import com.example.attendance.entity.AttendanceRecord;
import com.example.attendance.exception.AlreadyClockedInException;
import com.example.attendance.exception.AlreadyClockedOutException;
import com.example.attendance.exception.NotClockedInException;
import com.example.attendance.repository.AttendanceRecordRepository;
import com.example.attendance.service.AttendanceService;
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

    public AttendanceServiceImpl(AttendanceRecordRepository attendanceRecordRepository) {
        this.attendanceRecordRepository = attendanceRecordRepository;
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
}

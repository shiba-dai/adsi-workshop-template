package com.example.attendance.repository;

import com.example.attendance.entity.BreakRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BreakRecordRepository extends JpaRepository<BreakRecord, Long> {

    List<BreakRecord> findByAttendanceRecordId(Long attendanceRecordId);

    List<BreakRecord> findByAttendanceRecordIdIn(List<Long> attendanceRecordIds);
}

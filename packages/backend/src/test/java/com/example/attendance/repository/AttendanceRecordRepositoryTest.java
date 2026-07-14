package com.example.attendance.repository;

import com.example.attendance.entity.AttendanceRecord;
import com.example.attendance.entity.Employee;
import com.example.attendance.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class AttendanceRecordRepositoryTest {

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    private Long employeeId;

    @BeforeEach
    void setUp() {
        var employee = Employee.builder()
            .employeeCode("EMP100")
            .name("テストユーザー")
            .email("test@example.com")
            .passwordHash("$2a$10$dummy")
            .role(Role.EMPLOYEE)
            .enabled(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        employeeId = employeeRepository.save(employee).getId();
    }

    @Test
    @DisplayName("社員IDと日付で検索するとレコードが返される")
    void findByEmployeeIdAndWorkDate_existing_returnsRecord() {
        var record = createRecord(LocalDate.of(2026, 7, 14));
        attendanceRecordRepository.save(record);

        var result = attendanceRecordRepository.findByEmployeeIdAndWorkDate(employeeId, LocalDate.of(2026, 7, 14));

        assertThat(result).isPresent();
        assertThat(result.get().getWorkDate()).isEqualTo(LocalDate.of(2026, 7, 14));
    }

    @Test
    @DisplayName("存在しない日付で検索すると空が返される")
    void findByEmployeeIdAndWorkDate_nonExisting_returnsEmpty() {
        var result = attendanceRecordRepository.findByEmployeeIdAndWorkDate(employeeId, LocalDate.of(2026, 7, 14));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("期間指定で検索すると範囲内のレコードが返される")
    void findByEmployeeIdAndWorkDateBetween_returnsRecordsInRange() {
        attendanceRecordRepository.save(createRecord(LocalDate.of(2026, 7, 1)));
        attendanceRecordRepository.save(createRecord(LocalDate.of(2026, 7, 15)));
        attendanceRecordRepository.save(createRecord(LocalDate.of(2026, 8, 1)));

        var result = attendanceRecordRepository.findByEmployeeIdAndWorkDateBetween(
            employeeId, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("同一社員・同一日付の重複レコードは保存できない")
    void save_duplicateEmployeeAndDate_throwsException() {
        attendanceRecordRepository.save(createRecord(LocalDate.of(2026, 7, 14)));

        var duplicate = createRecord(LocalDate.of(2026, 7, 14));

        assertThatThrownBy(() -> {
            attendanceRecordRepository.saveAndFlush(duplicate);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private AttendanceRecord createRecord(LocalDate workDate) {
        return AttendanceRecord.builder()
            .employeeId(employeeId)
            .workDate(workDate)
            .clockInTime(workDate.atTime(9, 0))
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
}

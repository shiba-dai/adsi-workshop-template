package com.example.attendance.repository;

import com.example.attendance.entity.Employee;
import com.example.attendance.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    @DisplayName("存在するメールアドレスで検索すると社員が返される")
    void findByEmail_existingEmail_returnsEmployee() {
        var employee = createEmployee("EMP100", "test@example.com");
        employeeRepository.save(employee);

        var result = employeeRepository.findByEmail("test@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("テストユーザー");
    }

    @Test
    @DisplayName("存在しないメールアドレスで検索すると空が返される")
    void findByEmail_nonExistingEmail_returnsEmpty() {
        var result = employeeRepository.findByEmail("notfound@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("重複するメールアドレスで保存すると例外が発生する")
    void save_duplicateEmail_throwsException() {
        var employee1 = createEmployee("EMP100", "dup@example.com");
        employeeRepository.save(employee1);

        var employee2 = createEmployee("EMP101", "dup@example.com");

        assertThatThrownBy(() -> {
            employeeRepository.saveAndFlush(employee2);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Employee createEmployee(String code, String email) {
        return Employee.builder()
            .employeeCode(code)
            .name("テストユーザー")
            .email(email)
            .passwordHash("$2a$10$dummy")
            .role(Role.EMPLOYEE)
            .enabled(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
}

package com.example.attendance.controller;

import com.example.attendance.dto.AttendanceDetailResponse;
import com.example.attendance.dto.AttendanceResponse;
import com.example.attendance.dto.EmployeeResponse;
import com.example.attendance.exception.AlreadyClockedInException;
import com.example.attendance.exception.AlreadyClockedOutException;
import com.example.attendance.exception.GlobalExceptionHandler;
import com.example.attendance.exception.NotClockedInException;
import com.example.attendance.security.SecurityConfig;
import com.example.attendance.security.SessionAuthenticationFilter;
import com.example.attendance.service.AttendanceService;
import com.example.attendance.service.BreakService;
import com.example.attendance.service.WorkingTimeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AttendanceController.class)
@Import({SecurityConfig.class, SessionAuthenticationFilter.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttendanceService attendanceService;

    @MockitoBean
    private BreakService breakService;

    @MockitoBean
    private WorkingTimeService workingTimeService;

    private MockHttpSession createAuthenticatedSession() {
        var session = new MockHttpSession();
        session.setAttribute("authenticatedEmployee",
            new EmployeeResponse(1L, "EMP001", "田中太郎", "tanaka@example.com", "EMPLOYEE"));
        return session;
    }

    @Test
    @DisplayName("POST /api/attendance/clock-in 正常系: 201が返る")
    void clockIn_success_returns201() throws Exception {
        var response = new AttendanceResponse(1L, LocalDate.now(), LocalDateTime.now(), null);
        when(attendanceService.clockIn(1L)).thenReturn(response);

        mockMvc.perform(post("/api/attendance/clock-in").session(createAuthenticatedSession()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.workDate").exists());
    }

    @Test
    @DisplayName("POST /api/attendance/clock-in 重複: 409が返る")
    void clockIn_duplicate_returns409() throws Exception {
        when(attendanceService.clockIn(1L)).thenThrow(new AlreadyClockedInException());

        mockMvc.perform(post("/api/attendance/clock-in").session(createAuthenticatedSession()))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/attendance/clock-out 正常系: 200が返る")
    void clockOut_success_returns200() throws Exception {
        var response = new AttendanceResponse(1L, LocalDate.now(), LocalDateTime.now().minusHours(8), LocalDateTime.now());
        when(attendanceService.clockOut(1L)).thenReturn(response);

        mockMvc.perform(post("/api/attendance/clock-out").session(createAuthenticatedSession()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clockOutTime").exists());
    }

    @Test
    @DisplayName("POST /api/attendance/clock-out 未出勤: 400が返る")
    void clockOut_notClockedIn_returns400() throws Exception {
        when(attendanceService.clockOut(1L)).thenThrow(new NotClockedInException());

        mockMvc.perform(post("/api/attendance/clock-out").session(createAuthenticatedSession()))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/attendance/clock-out 既に退勤済: 409が返る")
    void clockOut_alreadyClockedOut_returns409() throws Exception {
        when(attendanceService.clockOut(1L)).thenThrow(new AlreadyClockedOutException());

        mockMvc.perform(post("/api/attendance/clock-out").session(createAuthenticatedSession()))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /api/attendance/today 打刻あり: 200が返る")
    void today_exists_returns200() throws Exception {
        var response = new AttendanceResponse(1L, LocalDate.now(), LocalDateTime.now(), null);
        when(attendanceService.getToday(1L)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/attendance/today").session(createAuthenticatedSession()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.workDate").exists());
    }

    @Test
    @DisplayName("GET /api/attendance/today 打刻なし: 204が返る")
    void today_noRecord_returns204() throws Exception {
        when(attendanceService.getToday(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/attendance/today").session(createAuthenticatedSession()))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/attendance/history 正常系: 200とリストが返る")
    void history_returns200WithList() throws Exception {
        var records = List.of(
            new AttendanceDetailResponse(1L, LocalDate.of(2026, 7, 1),
                LocalDateTime.of(2026, 7, 1, 9, 0), LocalDateTime.of(2026, 7, 1, 18, 0),
                480, 60, 30, List.of(), 0L),
            new AttendanceDetailResponse(2L, LocalDate.of(2026, 7, 2),
                LocalDateTime.of(2026, 7, 2, 9, 0), null,
                0, 0, 0, List.of(), 0L)
        );
        when(attendanceService.getMonthlyDetailHistory(1L, 2026, 7)).thenReturn(records);

        mockMvc.perform(get("/api/attendance/history")
                .param("year", "2026")
                .param("month", "7")
                .session(createAuthenticatedSession()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].workingMinutes").value(480));
    }

    @Test
    @DisplayName("PUT /api/attendance/{id} 正常系: 打刻修正で200が返る")
    void updateAttendance_success_returns200() throws Exception {
        var response = new AttendanceDetailResponse(1L, LocalDate.of(2026, 7, 1),
            LocalDateTime.of(2026, 7, 1, 8, 30), LocalDateTime.of(2026, 7, 1, 17, 30),
            480, 60, 30, List.of(), 1L);
        when(attendanceService.updateAttendance(eq(1L), eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/attendance/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "clockInTime": "2026-07-01T08:30:00",
                        "clockOutTime": "2026-07-01T17:30:00",
                        "version": 0
                    }
                    """)
                .session(createAuthenticatedSession()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clockInTime").value("2026-07-01T08:30:00"));
    }
}

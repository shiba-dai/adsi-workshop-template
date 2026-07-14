package com.example.attendance.exception;

import org.springframework.http.HttpStatus;

public class NotClockedInException extends BusinessException {

    public NotClockedInException() {
        super(HttpStatus.BAD_REQUEST, "NOT_CLOCKED_IN", "出勤打刻がされていません");
    }
}

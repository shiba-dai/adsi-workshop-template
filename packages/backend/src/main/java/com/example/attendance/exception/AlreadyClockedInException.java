package com.example.attendance.exception;

import org.springframework.http.HttpStatus;

public class AlreadyClockedInException extends BusinessException {

    public AlreadyClockedInException() {
        super(HttpStatus.CONFLICT, "ALREADY_CLOCKED_IN", "本日は既に出勤打刻されています");
    }
}

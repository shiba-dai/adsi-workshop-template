package com.example.attendance.exception;

import org.springframework.http.HttpStatus;

public class AlreadyClockedOutException extends BusinessException {

    public AlreadyClockedOutException() {
        super(HttpStatus.CONFLICT, "ALREADY_CLOCKED_OUT", "本日は既に退勤打刻されています");
    }
}

package com.example.attendance.exception;

import org.springframework.http.HttpStatus;

public class AccountDisabledException extends BusinessException {

    public AccountDisabledException() {
        super(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "アカウントが無効化されています");
    }
}

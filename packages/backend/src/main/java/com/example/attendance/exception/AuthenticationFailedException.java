package com.example.attendance.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationFailedException extends BusinessException {

    public AuthenticationFailedException() {
        super(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "メールアドレスまたはパスワードが正しくありません");
    }
}

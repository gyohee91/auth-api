package com.okbank.fintech.global.exception;

import org.springframework.http.HttpStatus;
/**
 * 인증 예외
 */
public class UnauthorizedException extends BusinessException {
    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}

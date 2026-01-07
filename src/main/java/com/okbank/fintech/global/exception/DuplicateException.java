package com.okbank.fintech.global.exception;

import org.springframework.http.HttpStatus; /**
 * 중복된 리소스
 */
public class DuplicateException extends BusinessException {
    public DuplicateException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}

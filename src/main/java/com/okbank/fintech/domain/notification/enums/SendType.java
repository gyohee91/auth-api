package com.okbank.fintech.domain.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SendType {
    IMMEDIATE("즉시 발송"),
    SCHEDULED("예약 발송");

    private final String displayName;
}

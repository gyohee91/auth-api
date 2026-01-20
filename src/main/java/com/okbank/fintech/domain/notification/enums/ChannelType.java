package com.okbank.fintech.domain.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChannelType {
    SMS("SMS", "/send/sms"),
    KAKAOTALK("카카오톡", "/send/kakaotalk"),
    EMAIL("이메일", "/send/email");

    public final String displayName;
    private final String apiPath;
}

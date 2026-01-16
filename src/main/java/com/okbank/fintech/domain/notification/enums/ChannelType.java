package com.okbank.fintech.domain.notification.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ChannelType {
    SMS("SMS"),
    KAKAOTALK("카카오톡"),
    EMAIL("이메일");

    public final String displayName;
}

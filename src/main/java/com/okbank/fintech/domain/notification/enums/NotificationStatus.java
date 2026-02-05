package com.okbank.fintech.domain.notification.enums;

public enum NotificationStatus {
    PENDING,    //생성됨
    QUEUED,     //Kafka에 publish됨
    SENT,       //발송 성공
    FAILED      //발송 실패
}

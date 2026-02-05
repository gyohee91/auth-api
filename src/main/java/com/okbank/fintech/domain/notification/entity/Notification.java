package com.okbank.fintech.domain.notification.entity;

import com.okbank.fintech.domain.notification.dto.event.NotificationCreatedEvent;
import com.okbank.fintech.domain.notification.enums.ChannelType;
import com.okbank.fintech.domain.notification.enums.NotificationStatus;
import com.okbank.fintech.domain.notification.enums.SendType;
import com.okbank.fintech.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Notification extends BaseEntity {
    @Comment("clientId")
    private String clientId;

    @Enumerated(EnumType.STRING)
    @Comment("채널 타입")
    private ChannelType channelType;

    @Enumerated(EnumType.STRING)
    @Comment("발송 타입 ")
    private SendType sendType;

    @Comment("수신자")
    private String recipient;

    @Column(nullable = false, length = 200)
    @Comment("제목")
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    @Comment("내용")
    private String contents;

    @Enumerated(EnumType.STRING)
    @Comment("발송 상태")
    @Builder.Default
    private NotificationStatus notificationStatus = NotificationStatus.PENDING;

    @Column
    @Comment("예약시간")
    private LocalDateTime scheduledAt;

    @Comment("전송 시각")
    private LocalDateTime sentAt;

    @Comment("결과 코드")
    private String resultCode;

    @Comment("에러 메시지")
    private String errorMessage;

    public void markAsQueued() {
        //this.resultCode = resultCode;
        this.notificationStatus = NotificationStatus.QUEUED;
        this.sentAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    public NotificationCreatedEvent toEvent() {
        return NotificationCreatedEvent.builder()
                .notificationId(getId())
                .channelType(channelType)
                .recipient(recipient)
                .title(title)
                .contents(contents)
                .retryCount(0)
                .build();
    }
}

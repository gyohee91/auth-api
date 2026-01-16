package com.okbank.fintech.domain.notification.entity;

import com.okbank.fintech.domain.notification.enums.ChannelType;
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

    @Column(nullable = false, length = 200)
    @Comment("제목")
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    @Comment("내용")
    private String contents;

    @Column
    @Comment("예약시간")
    private LocalDateTime scheduledAt;
}

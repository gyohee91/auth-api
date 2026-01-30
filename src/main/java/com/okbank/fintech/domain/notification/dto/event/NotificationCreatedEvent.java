package com.okbank.fintech.domain.notification.dto.event;

import com.okbank.fintech.domain.notification.enums.ChannelType;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class NotificationCreatedEvent {
    private Long notificationId;
    private String recipient;
    private ChannelType channelType;
    private String content;
    private Integer retryCount;
}

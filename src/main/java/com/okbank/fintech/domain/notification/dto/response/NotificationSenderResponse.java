package com.okbank.fintech.domain.notification.dto.response;

import com.okbank.fintech.domain.notification.entity.Notification;
import com.okbank.fintech.domain.notification.enums.ChannelType;
import com.okbank.fintech.domain.notification.enums.SendType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSenderResponse {

    @Schema(description = "채널 타입")
    private ChannelType channelType;

    @Schema(description = "발송 타입")
    private SendType sendType;

    public static NotificationSenderResponse from(Notification notification) {
        return NotificationSenderResponse.builder().build();
    }
}

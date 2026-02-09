package com.okbank.fintech.domain.notification.util;

import com.okbank.fintech.domain.notification.enums.ChannelType;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class NotificationContextPropagator {
    private static final String MDC_NOTIFICATION_ID = "notificationId";
    private static final String MDC_CHANNEL_TYPE = "channelType";

    /**
     * 알림 컨텍스트 설정
     * @param notificationId
     * @param channelType
     */
    public void setNotificationContext(Long notificationId, ChannelType channelType) {
        if(Objects.nonNull(notificationId))
            MDC.put(MDC_NOTIFICATION_ID, String.valueOf(notificationId));
        if(Objects.nonNull(channelType))
            MDC.put(MDC_CHANNEL_TYPE, channelType.name());
    }

    /**
     * 현재 알림 ID 조회
     * @return
     */
    public Long getCurrentNotificationId() {
        return Long.parseLong(MDC.get(MDC_NOTIFICATION_ID));
    }

    /**
     * 현재 Channel Type 조회
     * @return
     */
    public ChannelType getCurrentChannelType() {
        return ChannelType.valueOf(MDC.get(MDC_CHANNEL_TYPE));
    }

    /**
     * 알림 컨텍스트 정리
     */
    public void clear() {
        MDC.remove(MDC_NOTIFICATION_ID);
        MDC.remove(MDC_CHANNEL_TYPE);
    }
}

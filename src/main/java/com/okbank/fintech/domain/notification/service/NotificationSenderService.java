package com.okbank.fintech.domain.notification.service;

import com.okbank.fintech.domain.notification.dto.external.SendResultResponse;
import com.okbank.fintech.domain.notification.entity.Notification;
import com.okbank.fintech.domain.notification.enums.ChannelType;
import com.okbank.fintech.domain.notification.sender.ChannelSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotificationSenderService {
    private final Map<ChannelType, ChannelSender> senderMap;

    /**
     * 생성자 주입으로 모든 ChannelSender를 자동 수집
     */
    public NotificationSenderService(List<ChannelSender> channelSenders) {
        this.senderMap = channelSenders.stream()
                .collect(Collectors.toMap(
                        ChannelSender::getSupportedChannel,
                        Function.identity()
                ));
    }

    /**
     * 채널별 알림 발송
     * Strategy 패턴으로 채널에 맞는 Sender를 자동 선택
     */
    public SendResultResponse send(Notification notification) {
        ChannelType channelType = notification.getChannelType();

        ChannelSender sender = senderMap.get(channelType);
        if(Objects.isNull(sender)) {
            log.error("No sender found for channel: {}", channelType);
            return SendResultResponse.fail("No sender found for channel: " + channelType);
        }

        try {
            return sender.send(notification);
        } catch (Exception e) {
            return SendResultResponse.fail("Failed to send notification: channelType=" + channelType + ", notificationId=" + notification.getId());
        }
    }
}

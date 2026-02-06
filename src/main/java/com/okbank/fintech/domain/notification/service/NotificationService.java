package com.okbank.fintech.domain.notification.service;

import com.okbank.fintech.domain.notification.dto.event.NotificationCreatedEvent;
import com.okbank.fintech.domain.notification.dto.request.NotificationSenderRequest;
import com.okbank.fintech.domain.notification.dto.response.NotificationSenderResponse;
import com.okbank.fintech.domain.notification.entity.Notification;
import com.okbank.fintech.domain.notification.kafka.producer.NotificationEventProducer;
import com.okbank.fintech.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Producer: 메시지 발행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationSenderService senderService;
    private final NotificationEventProducer eventProducer;

    @Transactional
    public NotificationSenderResponse registerNotification(NotificationSenderRequest request) {

        Notification notification = Notification.builder()
                .channelType(request.getChannelType())
                .title(request.getTitle())
                .contents(request.getContents())
                .build();

        notificationRepository.save(notification);

        try{
            //Kafka로 이벤트 발행
            NotificationCreatedEvent event = notification.toEvent();

            //Kafka Broker가 메시지를 Topic의 Partition에 저장
            // - Key 기반 Partition 분배
            // - Offset 부여()
            eventProducer.publishNotificationCreated(event)
                    .thenAccept(result -> {
                        log.info("Event published successfully: notificationId={}, partition={}, offset={}",
                                event.getNotificationId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset()
                        );
                    })
                    .exceptionally(ex -> {
                        log.error("Failed to publish event: notificationId={}", event.getNotificationId(), ex);
                        return null;
                    });
            //상태 업데이트
            notification.markAsQueued();

        } catch (Exception e) {
            log.error("Unexpected error while publishing: notificationId={}", notification.getId(), e);
            throw new RuntimeException(e);
        }

        return NotificationSenderResponse.from(notification);
    }
}

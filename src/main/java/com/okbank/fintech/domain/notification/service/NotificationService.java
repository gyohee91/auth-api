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

        //SendResultResponse result = senderService.send(notification);
        try{
            NotificationCreatedEvent event = notification.toEvent();
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
            notification.markAsQueued();

        } catch (Exception e) {
            log.error("Unexpected error while publishing: notificationId={}", notification.getId(), e);
            throw new RuntimeException(e);
        }

        return NotificationSenderResponse.from(notification);
    }
}

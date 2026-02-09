package com.okbank.fintech.domain.notification.service;

import com.okbank.fintech.domain.notification.dto.event.NotificationCreatedEvent;
import com.okbank.fintech.domain.notification.dto.request.NotificationSenderRequest;
import com.okbank.fintech.domain.notification.dto.response.NotificationSenderResponse;
import com.okbank.fintech.domain.notification.entity.Notification;
import com.okbank.fintech.domain.notification.kafka.producer.NotificationEventProducer;
import com.okbank.fintech.domain.notification.repository.NotificationRepository;
import com.okbank.fintech.domain.notification.util.NotificationContextPropagator;
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
    private final NotificationContextPropagator notificationContextPropagator;

    @Transactional
    public NotificationSenderResponse registerNotification(NotificationSenderRequest request) {
        //DB 저장
        Notification notification = Notification.builder()
                .channelType(request.getChannelType())
                .title(request.getTitle())
                .contents(request.getContents())
                .build();

        notificationRepository.save(notification);

        //MDC에 알림 Context 설정 (이후 모든 로그에 자동 포함)
        notificationContextPropagator.setNotificationContext(
                notification.getId(),
                notification.getChannelType()
        );

        try{
            log.info("Notification registered");    //자동으로 notificationId, channelType 포함

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
        } finally {
            //Context 정리
            notificationContextPropagator.clear();
        }

        return NotificationSenderResponse.from(notification);
    }
}

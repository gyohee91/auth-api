package com.okbank.fintech.domain.notification.kafka.consumer;

import com.okbank.fintech.domain.notification.dto.event.NotificationCreatedEvent;
import com.okbank.fintech.domain.notification.dto.external.SendResultResponse;
import com.okbank.fintech.domain.notification.entity.Notification;
import com.okbank.fintech.domain.notification.kafka.producer.NotificationEventProducer;
import com.okbank.fintech.domain.notification.repository.NotificationRepository;
import com.okbank.fintech.domain.notification.service.NotificationSenderService;
import com.okbank.fintech.domain.notification.util.NotificationContextPropagator;
import com.okbank.fintech.global.util.RequestIdPropagator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {
    private final RequestIdPropagator requestIdPropagator;
    private final NotificationEventProducer eventProducer;
    private final NotificationRepository notificationRepository;
    private final NotificationSenderService senderService;
    private final NotificationContextPropagator notificationContextPropagator;

    private static final int MAX_RETRY_COUNT = 5;

    /**
     * 최초 알림 이벤트 소비
     * @param record
     * @param ack
     */
    @KafkaListener(
            topics = "notification.created",
            groupId = "notification-group",
            concurrency = "3"
    )
    public void consumerNotificationCreated(
            ConsumerRecord<String, NotificationCreatedEvent> record,
            Acknowledgment ack
    ) {
        NotificationCreatedEvent event = record.value();

        try {
            requestIdPropagator.restoreFromKafka(record);

            notificationContextPropagator.setNotificationContext(
                    event.getNotificationId(),
                    event.getChannelType()
            );

            log.info("Consuming notification"); //requestId, notificationId, channelType 자동 포함

            Notification notification = notificationRepository.findById(event.getNotificationId())
                    .orElseThrow(() -> new RuntimeException("Notification not found: " + event.getNotificationId()));

            SendResultResponse result = senderService.send(notification);

            if(result.isSuccess()) {
                notification.markAsSent(result.getResultCode());
                log.info("Notification sent successfully: notificationId={}, channelType={}",
                        notification.getId(), notification.getChannelType());
            }

            //수동 커밋
            ack.acknowledge();

            log.info("Successfully processed and acknowledged - ID: {}", event.getNotificationId());

        } catch (Exception e) {
            //실패 시 재시도 토픽으로 발행
            if(event.getRetryCount() + 1 <= MAX_RETRY_COUNT) {
                //재시도
                NotificationCreatedEvent retryEvent = event.increaseRetryCount();
                eventProducer.publishRetryEvent(event.getChannelType().name(), retryEvent);

                log.warn("Published to retry topic - ID: {}", event.getNotificationId());
            }
            else {
                notificationRepository.findById(event.getNotificationId())
                                .ifPresent(notification -> {
                                    notification.markAsFail(e.getMessage());
                                });

                log.error("Max Retry exceeded: id={}", event.getNotificationId());
            }
            ack.acknowledge();  //실패해도 커밋 (재시도 토픽으로 전송)
        } finally {
            //모든 Context 정리
            requestIdPropagator.clear();
            notificationContextPropagator.clear();
        }

    }

}

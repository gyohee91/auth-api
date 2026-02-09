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
            //requestId 복원
            requestIdPropagator.restoreFromKafka(record);

            //알림 Context 설정
            notificationContextPropagator.setNotificationContext(
                    event.getNotificationId(),
                    event.getChannelType()
            );

            log.info("Consuming notification: partition={}, offset={}",
                    record.partition(),
                    record.offset()
            ); //requestId, notificationId, channelType 자동 포함

            //알림 처리
            this.processNotification(event);

            //수동 커밋
            ack.acknowledge();

            log.info("Successfully processed and acknowledged - ID: {}", event.getNotificationId());

        } catch (Exception e) {
            //실패 시 재시도 토픽으로 발행
            this.handleFailure(event, e);
            ack.acknowledge();  //실패해도 커밋 (재시도 토픽으로 전송)
        } finally {
            //모든 Context 정리
            requestIdPropagator.clear();
            notificationContextPropagator.clear();
        }

    }

    /**
     * SMS 재시도 이벤트 소비
     */
    @KafkaListener(
            topics = "notification.retry.sms",
            groupId = "notification-retry-group",
            concurrency = "2"
    )
    public void consumerRetrySms(
            ConsumerRecord<String, NotificationCreatedEvent> record,
            Acknowledgment ack
    ) {
        this.processRetry(record, ack);
    }

    /**
     * Email 재시도 이벤트 소비
     */
    @KafkaListener(
            topics = "notification.retry.email",
            groupId = "notification-retry-email",
            concurrency = "2"
    )
    public void consumerRetryEmail(
            ConsumerRecord<String, NotificationCreatedEvent> record,
            Acknowledgment ack
    ) {
        this.processRetry(record, ack);
    }

    /**
     * 알림 처리 (실제 발송)
     */
    private void processNotification(NotificationCreatedEvent event) {
        //DB에서 알림 조회
        Notification notification = notificationRepository.findById(event.getNotificationId())
                .orElseThrow(() -> new RuntimeException("Notification not found: " + event.getNotificationId()));

        //실제 발송
        SendResultResponse result = senderService.send(notification);

        //발송 성공 처리
        if("SUCCCESS".equals(result.getResultCode())) {
            notification.markAsSent(result.getResultCode());
        }

        log.info("Notification sent successfully");
    }

    /**
     * 재시도 처리
     */
    private void processRetry(ConsumerRecord<String, NotificationCreatedEvent> record, Acknowledgment ack) {
        NotificationCreatedEvent event = record.value();

        try {
            //requestId 복원
            requestIdPropagator.restoreFromKafka(record);

            //알림 Context 설정
            notificationContextPropagator.setNotificationContext(
                    event.getNotificationId(),
                    event.getChannelType()
            );

            log.info("processing retry: retryCount={}/{}",
                    event.getRetryCount(),
                    MAX_RETRY_COUNT
            );

            //지수 백오프 적용
            Thread.sleep((long) (Math.pow(2, event.getRetryCount()) * 1000));

            //재시도 실행
            this.processNotification(event);

            //수동 커밋
            ack.acknowledge();

            log.info("Retry successful");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Retry interrupted: ", e);
            this.handleFailure(event, e);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Retry failed: ", e);
            this.handleFailure(event, e);
            ack.acknowledge();
        } finally {
            //Context 정리
            requestIdPropagator.clear();
            notificationContextPropagator.clear();
        }
    }

    /**
     * 실패 처리 (재시도 또는 최종 실패)
     */
    private void handleFailure(NotificationCreatedEvent event, Exception e) {
        int nextRetryCount = event.getRetryCount() + 1;

        if(nextRetryCount <= MAX_RETRY_COUNT) {
            //재시도
            NotificationCreatedEvent retryEvent = event.increaseRetryCount();
            eventProducer.publishRetryEvent(retryEvent.getChannelType().name(), retryEvent);
        } else {
            //최종 실패 - DB 업데이트
            notificationRepository.findById(event.getNotificationId())
                    .ifPresent(notification -> notification.markAsFail(e.getMessage()));
            log.error("Max retry exceeded, notification failed: totalAttempts={}",
                    nextRetryCount);
        }
    }

}

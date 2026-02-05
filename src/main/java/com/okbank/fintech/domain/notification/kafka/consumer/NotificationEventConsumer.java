package com.okbank.fintech.domain.notification.kafka.consumer;

import com.okbank.fintech.domain.notification.dto.event.NotificationCreatedEvent;
import com.okbank.fintech.domain.notification.kafka.producer.NotificationEventProducer;
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

    private static final int MAX_RETRY_COUNT = 5;

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

            //수동 커밋
            ack.acknowledge();

            log.info("Successfully processed and acknowledged - ID: {}", event.getNotificationId());

        } catch (Exception e) {
            //실패 시 재시도 토픽으로 발행
            if(event.getRetryCount() + 1 <= MAX_RETRY_COUNT) {
                eventProducer.publishRetryEvent(event.getChannelType().name(), event);
                log.warn("Published to retry topic - ID: {}", event.getNotificationId());
            }
            else {
                log.error("Max Retry exceeded: id={}", event.getNotificationId());
            }
            ack.acknowledge();
        }

    }

}

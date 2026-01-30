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

    @KafkaListener(
            topics = "notification.created",
            groupId = "notification-consumer-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumerNotificationCreated(
            ConsumerRecord<String, NotificationCreatedEvent> record,
            Acknowledgment ack
    ) {
        requestIdPropagator.restoreFromKafka(record);

        NotificationCreatedEvent event = record.value();

        boolean success = true;

        //실패 시 재시도 토픽으로 발행
        if(!success) {
            eventProducer.publishRetryEvent(event.getChannelType().name(), event);
            log.warn("Published to retry topic - ID: {}", event.getNotificationId());
        }


        //수동 커밋
        ack.acknowledge();
        log.info("Successfully processed and acknowledged - ID: {}", event.getNotificationId());
    }

}

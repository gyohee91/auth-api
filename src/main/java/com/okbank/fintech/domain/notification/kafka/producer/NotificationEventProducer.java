package com.okbank.fintech.domain.notification.kafka.producer;

import com.okbank.fintech.domain.notification.dto.event.NotificationCreatedEvent;
import com.okbank.fintech.global.util.RequestIdPropagator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RequestIdPropagator requestIdPropagator;

    public void publishNotificationCreated(NotificationCreatedEvent event) {
        String topic = "notification.created";
        String key = event.getNotificationId().toString();

        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, event);

        //requestId 전파
        requestIdPropagator.propagateToKafka(record);

        try {
            kafkaTemplate.send(record)
                    .whenComplete((result, throwable) -> {
                        log.info("Successfully published event to topic: {}, key: {}, partition: {}",
                                topic, key, result.getRecordMetadata().partition());
                    });
        } catch (Exception e) {
            log.error("Error while sending event to kafka", e);
        }
    }

    public void publishRetryEvent(String channel, NotificationCreatedEvent event) {
        String topic = "notification.retry." + channel.toLowerCase();
        String key = event.getNotificationId().toString();
        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, event);
        requestIdPropagator.propagateToKafka(record);

        kafkaTemplate.send(record);
        log.info("Published retry event to topic: {}", topic);
    }
}

package com.okbank.fintech.domain.notification.kafka.producer;

import com.okbank.fintech.domain.notification.dto.event.NotificationCreatedEvent;
import com.okbank.fintech.global.util.RequestIdPropagator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RequestIdPropagator requestIdPropagator;

    private static final String TOPIC_NOTIFICATION_CREATED = "notification.created";

    public void publishNotificationCreated(NotificationCreatedEvent event) {
        String key = event.getNotificationId().toString();

        ProducerRecord<String, Object> record = new ProducerRecord<>(
                TOPIC_NOTIFICATION_CREATED,
                null,
                key,
                event
        );

        //requestId 전파
        requestIdPropagator.propagateToKafka(record);

        try {
            kafkaTemplate.send(record)
                    .thenApply(result -> {
                        RecordMetadata metadata = result.getRecordMetadata();
                        log.info("Successfully published: id={}, key={}, partition={}, offset={}",
                                event.getNotificationId(),
                                key,
                                metadata.partition(),
                                metadata.offset()
                        );
                        return result;
                    })
                    .exceptionally(ex ->{
                        log.error("Failed to publish notification: id={}, key: {}",
                                event.getNotificationId(), key, ex);
                        throw new RuntimeException(ex);
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

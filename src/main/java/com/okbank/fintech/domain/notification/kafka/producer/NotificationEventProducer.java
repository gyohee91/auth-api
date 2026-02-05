package com.okbank.fintech.domain.notification.kafka.producer;

import com.okbank.fintech.domain.notification.dto.event.NotificationCreatedEvent;
import com.okbank.fintech.domain.notification.enums.ChannelType;
import com.okbank.fintech.global.util.RequestIdPropagator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventProducer {
    private final KafkaTemplate<String, NotificationCreatedEvent> kafkaTemplate;
    private final RequestIdPropagator requestIdPropagator;

    private static final String TOPIC_NOTIFICATION_CREATED = "notification.created";

    public CompletableFuture<SendResult<String, NotificationCreatedEvent>> publishNotificationCreated(NotificationCreatedEvent event) {
        String key = event.getNotificationId().toString();

        ProducerRecord<String, NotificationCreatedEvent> record = new ProducerRecord<>(
                TOPIC_NOTIFICATION_CREATED,
                null,   //partition은 key 기반 자동 분배
                key,
                event
        );

        //requestId 전파
        requestIdPropagator.propagateToKafka(record);

        record.headers().add("X-Channel-Type", event.getChannelType().name().getBytes(StandardCharsets.UTF_8));
        record.headers().add("X-Created-At", String.valueOf(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));

        return kafkaTemplate.send(record);
    }

    public void publishRetryEvent(String channel, NotificationCreatedEvent event) {
        String topic = "notification.retry." + channel.toLowerCase();
        String key = event.getNotificationId().toString();

        ProducerRecord<String, NotificationCreatedEvent> record = new ProducerRecord<>(topic, key, event);
        requestIdPropagator.propagateToKafka(record);

        record.headers().add("X-Retry-Count", String.valueOf(event.getRetryCount()).getBytes(StandardCharsets.UTF_8));

        kafkaTemplate.send(record)
                        .thenAccept(result ->
                                log.info("Retry event published: id={}, retryCount={}",
                                        event.getNotificationId(),
                                        event.getRetryCount()
                        ))
                .exceptionally(ex -> {
                    log.error("Failed to publish retry event: id={}",
                            event.getNotificationId(), ex);
                    return null;
                });
    }

    private String getRetryTopic(ChannelType channelType) {
        return switch (channelType) {
            case SMS -> "notification.retry.sms";
            case EMAIL -> "notification.retry.email";
            case KAKAOTALK -> "notification.retry.kakaotalk";
        };
    }
}

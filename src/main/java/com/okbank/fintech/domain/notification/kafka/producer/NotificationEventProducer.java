package com.okbank.fintech.domain.notification.kafka.producer;

import com.okbank.fintech.global.util.RequestIdPropagator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RequestIdPropagator requestIdPropagator;

    public void publishNotificationCreated() {

    }
}

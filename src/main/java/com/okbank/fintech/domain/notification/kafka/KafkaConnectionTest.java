package com.okbank.fintech.domain.notification.kafka;

import com.okbank.fintech.domain.notification.dto.event.NotificationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConnectionTest implements CommandLineRunner {
    private final KafkaTemplate<String, ?> kafkaTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            kafkaTemplate.getProducerFactory().createProducer().close();
            log.info("Kafka Connection successful: {}",
                    kafkaTemplate.getProducerFactory().getConfigurationProperties().get("bootstrap.servers")
            );
        } catch (Exception e) {
            log.error("Kafka connection failed", e);
        }
    }
}

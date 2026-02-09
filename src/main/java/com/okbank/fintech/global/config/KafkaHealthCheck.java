package com.okbank.fintech.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.health-check.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaHealthCheck implements CommandLineRunner {
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

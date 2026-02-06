package com.okbank.fintech.global.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfig {
    @Bean
    public KafkaAdmin kafkaAdmin(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic notificationCreatedTopic() {
        return TopicBuilder.name("notification.created")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000")
                .config("compression.type", "gzip")
                .build();
    }

    @Bean
    public NewTopic notificationRetrySmsTopic() {
        return TopicBuilder.name("notification.retry.sms")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationRetryEmailTopic() {
        return TopicBuilder.name("notification.retry.email")
                .partitions(3)
                .replicas(1)
                .build();
    }
}

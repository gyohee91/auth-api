package com.okbank.fintech.global.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic notificationCreatedTopic() {
        return TopicBuilder.name("notification.created")
                .partitions(3)
                .replicas(1)
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

package com.okbank.fintech.global.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
public class RequestIdPropagator {
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    /**
     * Kafka Producer에서 호출: MDC의 requestId를 Kafka Header에 넣기
     */
    public void propagateToKafka(ProducerRecord<String, ?> record) {
        String requestId = MDC.get(REQUEST_ID_HEADER);
        if(Objects.nonNull(requestId)) {
            record.headers().add(REQUEST_ID_HEADER, requestId.getBytes(StandardCharsets.UTF_8));
            log.debug("Propagated requestId to Kafka: {}", requestId);
        } else {
            log.warn("No requestId found in MDC for Kafka propagation");
        }
    }

    /**
     * Kafka Consumer에서 호출: Kafka Header의 requestId를 MDC에 복원
     */
    public void restoreFromKafka(ConsumerRecord<String, ?> record) {
        Header header = record.headers().lastHeader(REQUEST_ID_HEADER);

        if(Objects.nonNull(header)) {
            String requestId = new String(header.value(), StandardCharsets.UTF_8);
            MDC.put(REQUEST_ID_HEADER, requestId);
            log.debug("Restored requestId from Kafka: {}", requestId);
        } else {
            String newRequestId = UUID.randomUUID().toString();
            MDC.put(REQUEST_ID_HEADER, newRequestId);
            log.debug("Generated new requestId for Kafka consumer: {}", newRequestId);
        }
    }

    /**
     * 처리 완료 후 MDC 정리
     */
    public void clear() {
        MDC.clear();
    }
}

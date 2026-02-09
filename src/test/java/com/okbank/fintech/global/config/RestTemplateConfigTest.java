package com.okbank.fintech.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RestTemplateConfigTest {

    @Autowired
    private RetryTemplate retryTemplate;

    @Test
    @DisplayName("SocketTimeoutException은 재시도한다")
    void testRetryOnSocketTimeout() {
        AtomicInteger attempts = new AtomicInteger(0);

        assertThrows(SocketTimeoutException.class, () -> {
            retryTemplate.execute(context -> {
                attempts.incrementAndGet();
                throw new SocketTimeoutException("timeout");
            });
        });

        assertThat(attempts.get()).isEqualTo(2);    //원본 1회 + 재시도 1회
    }

    @Test
    @DisplayName("HttpClientErrorException은 재시도하지 않는다")
    void testNoRetryOnClientError() {
        AtomicInteger attempts = new AtomicInteger(0);

        assertThrows(HttpClientErrorException.class, () -> {
            retryTemplate.execute(context -> {
                attempts.incrementAndGet();
                throw new HttpClientErrorException(HttpStatus.BAD_REQUEST);
            });
        });

        assertThat(attempts.get()).isEqualTo(1);    //재시도 안함
    }
}
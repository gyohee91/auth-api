package com.okbank.fintech.global.config;

import com.okbank.fintech.global.interceptor.LoggingInterceptor;
import com.okbank.fintech.global.interceptor.RequestIdPropagationInterceptor;
import com.okbank.fintech.global.listener.RetryLoggingListener;
import com.okbank.fintech.global.util.RequestIdPropagator;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {
    private final RequestIdPropagator requestIdPropagator;
    private final RetryLoggingListener retryLoggingListener;

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_REQUEST_ID_KEY = "requestId";

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .additionalInterceptors(
                        new RequestIdPropagationInterceptor(requestIdPropagator),
                        new LoggingInterceptor()
                )
                .requestFactory(() -> new BufferingClientHttpRequestFactory(
                        new SimpleClientHttpRequestFactory()
                ))
                .build();
    }

    /**
     * RetryTemplate (5회, 500ms 간격)
     */
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        //재시도 정책
        Map<Class<? extends Throwable>, Boolean> retryableException = new HashMap<>();
        retryableException.put(HttpServerErrorException.class, true);
        retryableException.put(ResourceAccessException.class, true);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(5, retryableException);
        retryTemplate.setRetryPolicy(retryPolicy);

        //백오프 정책
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(500);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        //RetryListener 등록
        retryTemplate.registerListener(retryLoggingListener);

        return retryTemplate;
    }
}

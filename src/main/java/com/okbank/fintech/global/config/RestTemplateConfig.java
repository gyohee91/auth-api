package com.okbank.fintech.global.config;

import com.okbank.fintech.global.interceptor.LoggingInterceptor;
import com.okbank.fintech.global.interceptor.RequestIdPropagationInterceptor;
import com.okbank.fintech.global.listener.RetryLoggingListener;
import com.okbank.fintech.global.util.RequestIdPropagator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {
    private final RetryLoggingListener retryLoggingListener;

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_REQUEST_ID_KEY = "requestId";

    @Bean
    public RestTemplate restTemplate(
            RestTemplateBuilder builder,
            RequestIdPropagator requestIdPropagator
    ) {
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
     * RestTemplate 재시도 전략
     * - 대상: 네트워크 순간 오류만
     * - 횟수: 2회(원본 1회 + 재시도 1회)
     * - 백오프: 100ms 고정
     * - 5xx 에러는 Kafka 재시도로 위임
     */
    @Bean
    public RetryTemplate retryTemplate() {
        return RetryTemplate.builder()
                //네트워크 오류만 재시도
                .retryOn(SocketTimeoutException.class)
                .retryOn(ConnectException.class)
                .retryOn(NoRouteToHostException.class)
                //최대 2회 시도
                .maxAttempts(2)
                //100ms 고정 백오프
                .fixedBackoff(100)
                //리스너
                .withListener(retryLoggingListener)
                .build();
    }
}

package com.okbank.fintech.global.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

@Configuration
public class FeignConfig {
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_REQUEST_ID_KEY = "requestId";

    @Bean
    public RequestInterceptor requestIdInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate requestTemplate) {
                String requestId = MDC.get(MDC_REQUEST_ID_KEY);
                if(Objects.nonNull(requestId)) {
                    requestTemplate.header(REQUEST_ID_HEADER, requestId);
                }
            }
        };
    }
}

package com.okbank.fintech.global.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * 역할: 외부 API 호출 로깅
 * 목적: 모니터링, 디버깅, 성능 측정
 */
@Slf4j
@RequiredArgsConstructor
public class LoggingInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

        this.logRequest(request, body);

        long startTime = System.currentTimeMillis();
        ClientHttpResponse response;
        try {
            //실제 요청 실행
            response = execution.execute(request, body);

            long duration = System.currentTimeMillis() - startTime;

            this.logResponse(request, response, duration);

            return response;
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("<<< External API Error: {} {} - error={}, duration={}ms",
                    request.getMethod(),
                    request.getURI(),
                    e.getMessage(),
                    duration
            );
            throw e;
        }

    }

    /**
     * 요청 로깅
     */
    private void logRequest(HttpRequest request, byte[] body) {
        log.info(">>> External API Request: {} {}",
                request.getMethod(),
                request.getURI()
        );

        log.debug("Request Body: {}",
                new String(body, StandardCharsets.UTF_8)
        );
    }

    /**
     * 응답 로깅
     */
    private void logResponse(HttpRequest request, ClientHttpResponse response, long duration) throws IOException {
        log.info("<<< External API Response: {} {}- status={}, duration={}ms",
                request.getMethod(),
                request.getURI(),
                response.getStatusCode().value(),
                duration
        );

        if(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()) {
            String responseBody = "";
            try(BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.getBody(), StandardCharsets.UTF_8)
            )) {
                responseBody = reader.lines().collect(Collectors.joining());
            }
            log.error("Error Response Body: status={}, body={}",
                    response.getStatusCode().value(),
                    responseBody
            );
        }
    }
}

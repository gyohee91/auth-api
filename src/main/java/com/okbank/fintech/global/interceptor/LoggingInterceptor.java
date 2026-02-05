package com.okbank.fintech.global.interceptor;

import com.okbank.fintech.global.util.RequestIdPropagator;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class LoggingInterceptor implements ClientHttpRequestInterceptor {
    private final RequestIdPropagator requestIdPropagator;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        String requestId = Optional.ofNullable(requestIdPropagator.getCurrentRequestId())
                .orElse("UNKNOWN");

        long startTime = System.currentTimeMillis();

        this.logRequest(request, body, requestId);

        ClientHttpResponse response;
        try {
            //실제 요청 실행
            response = execution.execute(request, body);

            long duration = System.currentTimeMillis() - startTime;

            this.logResponse(request, response, duration, requestId);

            return response;
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("<<< External API Error [{}]: {} {} - error={}, duration={}ms",
                    requestId,
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
    private void logRequest(HttpRequest request, byte[] body, String requestId) {
        log.info(">>> External API Request [{}]: {} {}",
                requestId,
                request.getMethod(),
                request.getURI()
        );

        log.debug("Request Body [{}]: {}",
                requestId,
                new String(body, StandardCharsets.UTF_8)
        );
    }

    /**
     * 응답 로깅
     */
    private void logResponse(HttpRequest request, ClientHttpResponse response, long duration, String requestId) throws IOException {
        log.info("<<< External API Response [{}]: {} {}- status={}, duration={}ms",
                requestId,
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
            log.error("Error Response Body [{}]: status={}, body={}",
                    requestId,
                    response.getStatusCode().value(),
                    responseBody
            );
        }
    }
}

package com.okbank.fintech.global.interceptor;

import com.okbank.fintech.global.util.RequestIdPropagator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * 역할: requestId를 외부 API Header에 전파
 * 목적: 분산 추적
 */
@RequiredArgsConstructor
public class RequestIdPropagationInterceptor implements ClientHttpRequestInterceptor {
    private final RequestIdPropagator requestIdPropagator;

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        //MDC에서 RequestId 가져오기(없으면 생성)
        String requestId = Optional.ofNullable(requestIdPropagator.getCurrentRequestId())
                .filter(id -> !id.isBlank())
                .orElseGet(() -> {
                    String newRequestId = "EXTERNAL-" + UUID.randomUUID();
                    requestIdPropagator.setRequestId(newRequestId);
                    return newRequestId;
                });

        //요청 Header에 requestId 추가
        request.getHeaders().add(REQUEST_ID_HEADER, requestId);

        return execution.execute(request, body);
    }
}

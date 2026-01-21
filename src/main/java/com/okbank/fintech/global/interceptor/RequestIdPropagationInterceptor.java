package com.okbank.fintech.global.interceptor;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * requestId 전파 Interceptor
 */
public class RequestIdPropagationInterceptor implements ClientHttpRequestInterceptor {
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_REQUEST_ID_KEY = "requestId";

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        //MDC에서 RequestId 가져오기(없으면 생성)
        String requestId = Optional.ofNullable(MDC.get(MDC_REQUEST_ID_KEY))
                .filter(id -> !id.isBlank())
                .orElseGet(() -> {
                    String newRequestId = "EXTERNAL-" + UUID.randomUUID();
                    MDC.put(MDC_REQUEST_ID_KEY, newRequestId);
                    return newRequestId;
                });

        //요청 Header에 requestId 추가
        request.getHeaders().add(REQUEST_ID_HEADER, requestId);

        return execution.execute(request, body);
    }
}

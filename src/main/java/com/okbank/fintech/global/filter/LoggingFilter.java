package com.okbank.fintech.global.filter;

import com.okbank.fintech.global.util.RequestIdPropagator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class LoggingFilter extends OncePerRequestFilter {
    private final RequestIdPropagator requestIdPropagator;

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "/v3/api-docs",
            "/swagger-ui",
            "/h2-console",
            "/actuator/health",
            "/actuator/prometheus",
            "/favicon.ico"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //Wrapper 적용
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            //RequestId 처리
            String requestId = Optional.ofNullable(request.getHeader(REQUEST_ID_HEADER))
                    .filter(id -> !id.isBlank())
                    .orElse(UUID.randomUUID().toString());

            requestIdPropagator.setRequestId(requestId);
            responseWrapper.setHeader(REQUEST_ID_HEADER, requestId);

            //요청 로깅
            this.logRequest(requestWrapper, requestId);

            long startTime = System.currentTimeMillis();

            //실제 요청 처리
            filterChain.doFilter(requestWrapper, responseWrapper);

            long duration = System.currentTimeMillis() - startTime;

            //응답 로깅
            this.logResponse(requestWrapper, responseWrapper, duration);

            //응답 복사
            responseWrapper.copyBodyToResponse();
        } finally {
            requestIdPropagator.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return EXCLUDE_PATHS.stream().anyMatch(request.getRequestURI()::startsWith);
    }

    private void logRequest(ContentCachingRequestWrapper request, String requestId) {
        log.info(">>> [{}] {} {} - Client: {}",
                requestId,
                request.getMethod(),
                Objects.nonNull(request.getQueryString()) ?
                        request.getRequestURI() + "?" + request.getQueryString() : request.getRequestURI(),
                request.getRemoteAddr()
        );

        log.debug("Request Body: {}", new String(request.getContentAsByteArray(), StandardCharsets.UTF_8));
    }

    private void logResponse(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, long duration) {
        log.info("<<< {} {} - Status: {} - {}ms",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration
        );

        if(response.getStatus() >= 400) {
            log.debug("Error Request Body: {}", new String(response.getContentAsByteArray(), StandardCharsets.UTF_8));
        }
    }
}

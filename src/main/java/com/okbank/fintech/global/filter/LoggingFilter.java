package com.okbank.fintech.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
public class LoggingFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Request/Response를 여러 번 읽을 수 있도록 래핑
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        //트랜잭션ID 생성
        String transactionId = UUID.randomUUID().toString();
        MDC.put("transactionId", transactionId);
        MDC.put("clientIP", this.getClientIp(request));

        long startTime = System.currentTimeMillis();

        try {
            //요청 로깅
            this.logRequest(requestWrapper, transactionId);

            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            //응답 로깅
            long duration = System.currentTimeMillis() - startTime;
            this.logResponse(responseWrapper, transactionId, duration);

            //Response body를 실제 응답으로 복사
            responseWrapper.copyBodyToResponse();

            //MDC 정리
            MDC.clear();
        }

    }

    private void logRequest(ContentCachingRequestWrapper request, String transactionId) {
        String queryString = request.getQueryString();
        String requestUrl = Objects.nonNull(queryString)
                ? request.getRequestURI() + "?" + queryString
                : request.getRequestURI();

        log.info("[REQUEST] [{}] {} {} - User-Agent: {}",
                transactionId,
                request.getMethod(),
                requestUrl,
                request.getHeader("User-Agent")
        );

        //Request Body 로깅
        byte[] content = request.getContentAsByteArray();
        if(content.length > 0) {
            String requestBody = new String(content, StandardCharsets.UTF_8);

            //패스워드, 토큰 등 민감정보 마스킹
            requestBody = this.maskSensitiveData(requestBody);
            log.debug("[REQUEST BODY] [{}] {}", transactionId, requestBody);
        }
    }

    private void logResponse(ContentCachingResponseWrapper response, String transactionId, long duration) {
        int status = response.getStatus();

        log.info("[RESPONSE] [{}] Status: {} - Duration: {}ms",
                transactionId,
                status,
                duration
        );

        //Request Body 로깅
        byte[] content = response.getContentAsByteArray();
        if(content.length > 0) {
            String responseBody = new String(content, StandardCharsets.UTF_8);
            log.debug("[RESPONSE BODY] [{}] {}", transactionId, responseBody);
        }

    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 다중 IP인 경우 첫 번째 IP 반환
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String maskSensitiveData(String data) {
        //패스워드, 토큰 등 민감정보 마스킹
        return data
                .replaceAll("(\"password\"\\s*:\\s*\")[^\"]*(\")", "$1****$2")
                .replaceAll("(\"token\"\\s*:\\s*\")[^\"]*(\")", "$1****$2")
                .replaceAll("(\"accessToken\"\\s*:\\s*\")[^\"]*(\")", "$1****$2")
                .replaceAll("(\"refreshToken\"\\s*:\\s*\")[^\"]*(\")", "$1****$2");
    }
}

package com.okbank.fintech.global.filter;

import com.okbank.fintech.domain.user.entity.UserStatus;
import com.okbank.fintech.global.security.CustomUserDetails;
import com.okbank.fintech.global.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JWT 인증 필터
 * - 요청 헤더에서 JWT 토큰을 추출하고 유효성을 검사하여 Security Context에 인증 정보를 설정
 * - CustomUserDetails를 활용하여 사용자 정보를 Security Context에 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> EXCLUDE_URL_PATTERNS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/signup",
            "/api/v1/public",
            "/actuator",
            "/h2-console",
            "/favicon.ico",
            "/error"
    );

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try{
            //1. Request Header에서 토큰 추출
            String token = this.resolveToken(request);
            String uri = request.getRequestURI();

            //2. 토큰 유효성 검사
            if(StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                //3. Access Token인지 확인
                if(!jwtTokenProvider.isAccessToken(token)) {
                    log.warn("Access Token이 아닙니다. URI: {}", uri);
                    filterChain.doFilter(request, response);
                    return;
                }

                //4. Authentication 객체 생성(CustomUserDetails 포함)
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

                //5. 사용자 상태 확인
                if(!isUserActive(userDetails)) {
                    log.warn("비활성 상태의 사용자가 접근을 시도했습니다. URI: {}, 사용자: {} 상태: {}", uri, userDetails.getUsername(), userDetails.getUserStatus());
                    filterChain.doFilter(request, response);
                    return;
                }

                //6. Security Context에 인증 정보 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (Exception e) {
            logger.error("Security Context에 인증 정보를 저장할 수 없습니다", e);
            //예외 발생 시에도 다음 필터로 진행(Security에서 처리)
        } finally {
            filterChain.doFilter(request, response);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String requestURI = request.getRequestURI();

        // 제외할 URL 패턴과 일치하는지 확인
        boolean shouldExclude = EXCLUDE_URL_PATTERNS.stream()
                .anyMatch(requestURI::startsWith);
        if(shouldExclude) {
            log.debug("JWT 인증 필터 제외 URL: {}", requestURI);
        }
        return shouldExclude;
    }

    /**
     * Request Header에서 토큰 정보를 꺼내오기
     *
     * @param request
     * @return
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return bearerToken;
    }

    /**
     * 사용자 활성 상태 확인
     *
     * @param userDetails
     * @return
     */
    private boolean isUserActive(CustomUserDetails userDetails) {
        // 삭제된 사용자
        if(userDetails.getMember().isDeleted()) {
            return false;
        }

        //비활성 or 정지된 사용자
        UserStatus status = userDetails.getUserStatus();
        return status == UserStatus.ACTIVE;
    }
}
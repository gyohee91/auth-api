package com.okbank.fintech.domain.auth.service;

import com.okbank.fintech.global.security.CustomUserDetails;
import com.okbank.fintech.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    private static final String REFRESH_TOKEN_PREFIX = "RT:";

    public void save(String refreshToken, CustomUserDetails userDetails) {
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + userDetails.getUsername(),
                refreshToken,
                jwtTokenProvider.getRefreshTokenValidity(),
                TimeUnit.MILLISECONDS
        );
        log.debug("Refresh Token 저장: mobile={}", userDetails.getUsername());
    }

    public String getRefreshToken(String mobile) {
        return redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + mobile);
    }

    public void delete(String mobile) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + mobile);
        log.debug("Refresh Token 삭제: mobile={}", mobile);
    }
}

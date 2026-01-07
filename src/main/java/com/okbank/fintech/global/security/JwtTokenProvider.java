package com.okbank.fintech.global.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtProperties jwtProperties;

    private SecretKey key;

    @PostConstruct
    protected void init() {
        // Initialization logic here
        this.key = Keys.hmacShaKeyFor(jwtProperties.secretKey().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Access Token 생성(오버로딩 - 하위 호환성)
     *
     * @param mobile
     * @param authorities
     * @return
     */
    public String createAccessToken(String mobile, Collection<? extends GrantedAuthority> authorities) {
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(mobile);
        return this.createAccessToken((CustomUserDetails) userDetails);
    }

    /**
     * Access Token 생성
     *
     * @param userId
     * @param authorities
     * @return
     */
    public String createAccessToken(CustomUserDetails userDetails) {
        String authoritiesStr = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = System.currentTimeMillis();
        Date validity = new Date(now + jwtProperties.accessTokenValidity()); // 1 hour validity

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("auth", authoritiesStr)
                .claim("type", "access")
                .issuedAt(new Date(now))
                .expiration(validity)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Refresh Token 생성
     *
     * @param userId
     * @return
     */
    public String createRefreshToken(String userId) {
        long now = System.currentTimeMillis();
        Date validity = new Date(now + jwtProperties.refreshTokenValidity()); // 7 days validity

        return Jwts.builder()
                .subject(userId)
                .claim("type", "refresh")
                .issuedAt(new Date(now))
                .expiration(validity)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Authentication 객체 생성
     * CustomUserDetails를 로드하여 사용자 정보 포함
     *
     * @param token
     * @return
     */
    public Authentication getAuthentication(String token) {
        // Implement token parsing and return Authentication object
        try {
            Claims claims = this.parseClaims(token);
            String mobile = claims.getSubject();

            CustomUserDetails userDetails = (CustomUserDetails) customUserDetailsService.loadUserByUsername(mobile);

            return new UsernamePasswordAuthenticationToken(
                    userDetails,
                    token,
                    userDetails.getAuthorities()
            );
        } catch (Exception e) {
            log.error("Authentication 객체를 생성할 수 없습니다: {}", e.getMessage());
            throw new RuntimeException("유효하지 않은 토큰");
        }
    }

    public long getRefreshTokenValidity() {
        return jwtProperties.refreshTokenValidity();
    }

    /**
     * 토큰 유효성 검사
     *
     * @param token
     * @return
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);

            // 토큰의 만료 시간 검사
            Date expiration = this.parseClaims(token).getExpiration();
            if (expiration.before(new Date())) {
                log.error("만료된 JWT 토큰입니다.");
                return false;
            }

            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("잘못된 JWT 서명입니다.", e);
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT 토큰입니다.", e);
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다.", e);
        } catch (IllegalArgumentException e) {
            log.error("JWT 토큰이 잘못되었습니다.", e);
        }
        return false;
    }

    /**
     * Access Token 여부 확인
     *
     * @param token
     * @return
     */
    public boolean isAccessToken(String token) {
        Claims claims = this.parseClaims(token);
        String type = claims.get("type", String.class);
        return "access".equals(type);
    }

    /**
     * Token에서 mobile 추출
     *
     * @param token
     * @return
     */
    public String getMobile(String token) {
        return this.parseClaims(token).getSubject();
    }

    /**
     * 토큰에서 Claims 파싱
     *
     * @param token
     * @return
     */
    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

}

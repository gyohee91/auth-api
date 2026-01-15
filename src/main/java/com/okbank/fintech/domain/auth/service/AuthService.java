package com.okbank.fintech.domain.auth.service;

import com.okbank.fintech.domain.auth.dto.LoginRequest;
import com.okbank.fintech.domain.auth.dto.TokenResponse;
import com.okbank.fintech.domain.user.entity.UserStatus;
import com.okbank.fintech.global.exception.UnauthorizedException;
import com.okbank.fintech.global.security.CustomUserDetails;
import com.okbank.fintech.global.security.CustomUserDetailsService;
import com.okbank.fintech.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public TokenResponse login(LoginRequest request) {
        try {
            //1. 사용자 인증
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getMobile(),
                            request.getPassword()
                    )
            );

            //2. CustomUserDetails 추출
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            //3. 사용자 상태 확인
            this.validateUserStatus(userDetails);

            //4. SecurityContext에 인증 정보 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);

            //5. Jwt 토큰 생성
            String accessToken = jwtTokenProvider.createAccessToken(userDetails);
            String refreshToken = jwtTokenProvider.createRefreshToken(userDetails.getUsername());

            //6. Refresh Token을 Redis에 저장
            refreshTokenService.save(refreshToken, userDetails);

            return TokenResponse.from(userDetails, accessToken, refreshToken);

        } catch(Exception e) {
            log.error("로그인 실패: mobile={}, reason={}", request.getMobile(), e.getMessage(), e);
            throw new RuntimeException("Mobile Number 또는 password가 일치하지 않습니다.");
        }
    }

    @Transactional
    public TokenResponse refreshToken(String refreshToken) {
        log.info("토큰 재발급 시도");

        try {
            //1. Refresh Token 검증
            if(!jwtTokenProvider.validateToken(refreshToken)) {
                throw new UnauthorizedException("유효하지 않은 Refresh Token입니다");
            }

            //2. Refresh Token에서 mobile 추출
            String mobile = jwtTokenProvider.getMobile(refreshToken);

            //3. Redis에서 저장된 Refresh Token 조회 및 비교
            String storedRefreshToken = refreshTokenService.getRefreshToken(mobile);
            if(Objects.isNull(storedRefreshToken))
                throw new UnauthorizedException("Refresh Token을 찾을 수 없습니다. 다시 로그인해주세요");
            if(!Objects.equals(refreshToken, storedRefreshToken))
                throw new UnauthorizedException("유효하지 않은 Refresh Token 입니다");

            //4. CustomUserDetails 로드(최신 사용자 정보)
            CustomUserDetails userDetails = (CustomUserDetails) customUserDetailsService.loadUserByUsername(mobile);

            //5. 사용자 상태 확인
            this.validateUserStatus(userDetails);

            //6. 새로운 Access Token 생성
            String newAccessToken = jwtTokenProvider.createAccessToken(userDetails);
            String newRefreshToken = jwtTokenProvider.createRefreshToken(userDetails.getUsername());

            log.debug("토큰 재발급 완료: mobile={}", mobile);

            //7. 재발급된 Refresh Token을 Redis에 저장
            refreshTokenService.save(newRefreshToken, userDetails);

            return TokenResponse.from(userDetails, newAccessToken, newRefreshToken);
        } catch (UnauthorizedException e) {
            log.error("토큰 재발급 실패: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("토큰 재발급 중 오류 발생", e);
            throw new UnauthorizedException("토큰 재발급에 실패했습니다");
        }
    }

    @Transactional
    public void logout(String mobile) {
        log.info("로그아웃: mobile={}", mobile);

        try {
            refreshTokenService.delete(mobile);

            //SecurityContext 초기화
            SecurityContextHolder.clearContext();

            log.info("로그아웃 완료: mobile={}", mobile);
        } catch (Exception e) {
            log.error("로그아웃 처리 중 오류 발생: mobile={}", mobile);
            throw new RuntimeException("로그아웃 처리 중 오류가 발생했습니다");
        }
    }

    /**
     * 사용자 상태 검증
     */
    private void validateUserStatus(CustomUserDetails userDetails) {
        if (userDetails.getMember().isDeleted()) {
            throw new UnauthorizedException("탈퇴한 사용자입니다.");
        }

        UserStatus status = userDetails.getUserStatus();
        if (status == UserStatus.SUSPENDED) {
            throw new UnauthorizedException("정지된 계정입니다.");
        }
        if (status == UserStatus.INACTIVE) {
            throw new UnauthorizedException("비활성화된 계정입니다.");
        }
        if (status == UserStatus.WITHDRAWN) {
            throw new UnauthorizedException("탈퇴한 계정입니다.");
        }
    }
}

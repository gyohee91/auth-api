package com.okbank.fintech.domain.auth.dto;

import com.okbank.fintech.domain.user.entity.UserRole;
import com.okbank.fintech.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TokenResponse {
    @Schema(description = "Access Token")
    private String accessToken;

    @Schema(description = "Refresh Token")
    private String refreshToken;
    private String mobile;
    private String name;
    private UserRole role;

    /**
     * CustomUserDetails와 토큰으로 TokenResponse 생성
     *
     * @param userDetails
     * @param accessToken
     * @param refreshToken
     * @return
     */
    public static TokenResponse from(
            CustomUserDetails userDetails,
            String accessToken,
            String refreshToken
    ) {
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .mobile(userDetails.getUsername())
                .name(userDetails.getName())
                .role(userDetails.getUserRole())
                .build();
    }
}

package com.okbank.fintech.domain.auth.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RefreshTokenRequest {
    private String refreshToken;
}

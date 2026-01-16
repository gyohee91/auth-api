package com.okbank.fintech.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Schema(description = "Token 재발급(요청)")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RefreshTokenRequest {
    @Schema(description = "refresh token")
    @NotNull(message = "refresh token은 필수입니다")
    private String refreshToken;
}

package com.okbank.fintech.domain.auth.controller;

import com.okbank.fintech.domain.auth.dto.LoginRequest;
import com.okbank.fintech.domain.auth.dto.RefreshTokenRequest;
import com.okbank.fintech.domain.auth.dto.TokenResponse;
import com.okbank.fintech.domain.auth.dto.*;
import com.okbank.fintech.domain.auth.service.AuthService;
import com.okbank.fintech.global.common.DataResponse;
import com.okbank.fintech.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "인증 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "로그인", description = "Mobile과 password로 로그인하여 JWT 토큰 발급")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - Mobile 또는 password 불일치"
            )
    })
    @PostMapping("/login")
    public ResponseEntity<DataResponse<TokenResponse>> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "로그인 요청 정보",
                    required = true,
                    content = @Content(schema = @Schema(implementation = LoginRequest.class))
            )
            @Valid @RequestBody LoginRequest request
    ) {
        log.info("로그인 요청: {}", request.getMobile());

        TokenResponse response = authService.login(request);

        return ResponseEntity.ok(DataResponse.success("로그인 성공", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<DataResponse<TokenResponse>> refreshToken(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "토큰 재발급 요청",
                    required = true,
                    content = @Content(schema = @Schema(implementation = RefreshTokenRequest.class))
            )
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        log.info("토큰 재발급 요청");

        TokenResponse response = authService.refreshToken(request.getRefreshToken());

        return ResponseEntity.ok(DataResponse.success("토큰 재발급 성공", response));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("로그아웃 요청: {}", userDetails.getUsername());

        authService.logout(userDetails.getUsername());

        return ResponseEntity.ok(DataResponse.success("로그아웃 성공"));
    }

}

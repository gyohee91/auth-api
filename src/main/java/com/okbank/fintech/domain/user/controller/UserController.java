package com.okbank.fintech.domain.user.controller;

import com.okbank.fintech.domain.auth.dto.UserResponse;
import com.okbank.fintech.domain.user.dto.UserCreateRequest;
import com.okbank.fintech.domain.user.service.UserService;
import com.okbank.fintech.global.common.DataResponse;
import com.okbank.fintech.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @Operation(
            summary = "회원가입",
            description = "새로운 사용자를 등록합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "회원가입 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "유효성 검증 실패"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 존재하는 사용자"
            )
    })
    @PostMapping("/signup")
    public ResponseEntity<DataResponse<UserResponse>> signup(
            @Valid @RequestBody UserCreateRequest request
    ) {
        log.info("회원가입 요청: {}", request.getMobile());

        UserResponse response = userService.createUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(DataResponse.success("회원가입이 완료되었습니다", response));
    }

    @Operation(summary = "내 정보 조회", description = "내 정보를 조회합니다")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "내 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - Access Token 없음 또는 만료"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "접근 권한 없음"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자 정보 없음"
            )
    })
    @GetMapping("/me")
    public ResponseEntity<DataResponse<UserResponse>> me(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UserResponse response = userService.getMe(userDetails.getUsername());

        return ResponseEntity.ok(DataResponse.success("조회 성공", response));
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "현재 로그인한 사용자의 계정을 탈퇴 처리함"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "탈퇴 처리 완료"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음"
            )
    })
    @DeleteMapping("/withdraw")
    public ResponseEntity<Void> withdraw(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        userService.withdraw(userDetails.getUsername());

        return ResponseEntity.noContent().build();
    }
}

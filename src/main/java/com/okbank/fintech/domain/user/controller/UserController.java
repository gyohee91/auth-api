package com.okbank.fintech.domain.user.controller;

import com.okbank.fintech.domain.user.dto.UserCreateRequest;
import com.okbank.fintech.domain.auth.dto.UserResponse;
import com.okbank.fintech.domain.user.service.UserService;
import com.okbank.fintech.global.common.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}

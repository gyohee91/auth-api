package com.okbank.fintech.domain.notification.controller;

import com.okbank.fintech.domain.notification.dto.request.NotificationSenderRequest;
import com.okbank.fintech.domain.notification.dto.response.NotificationSenderResponse;
import com.okbank.fintech.domain.notification.service.NotificationService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "알림 API", description = "알림 발송 등록 및 내역 조회")
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @Operation(
            summary = "알림 발송 등록",
            description = "즉시 발송 또는 예약 발송 알림을 등록합니다" +
                    "scheduledAt이 없으면 즉시 발송, 있으면 예약 발송입니다"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "알림 등록 성공",
                    content = @Content(schema = @Schema(implementation = NotificationSenderResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - Access Token 없음 또는 만료"
            )
    })
    @PostMapping("/send")
    public ResponseEntity<DataResponse<NotificationSenderResponse>> sendNotification(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody NotificationSenderRequest request
    ) {
        NotificationSenderResponse response = notificationService.registerNotification(request);

        return ResponseEntity.ok(DataResponse.success("발송 성공", response));
    }

}

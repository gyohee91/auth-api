package com.okbank.fintech.domain.notification.dto.external;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SendResultResponse {
    /**
     * 결과 코드 (SUCCESS, FAIL)
     */
    private String resultCode;

    /**
     * 결과 메시지
     */
    private String message;

    /**
     * 응답 시간
     */
    @Builder.Default
    private LocalDateTime responseTime = LocalDateTime.now();

    public static SendResultResponse success() {
        return SendResultResponse.builder()
                .resultCode("SUCCESS")
                .message("Send successful")
                .build();
    }

    public static SendResultResponse fail(String message) {
        return SendResultResponse.builder()
                .resultCode("FAIL")
                .message(message)
                .build();
    }

    public boolean isSuccess() {
        return "SUCCESS".equals(resultCode);
    }
}

package com.okbank.fintech.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API 에러 응답
 *
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private String requestId;
    private int status;
    private String error;
    private String message;
    private String path;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}

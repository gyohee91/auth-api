package com.okbank.fintech.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Api 성공 응답
 *
 * @param <T>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataResponse<T> {
    @Builder.Default
    private boolean success = true;
    private String message;
    private T data;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> DataResponse<T> success(String message, T data) {
        return DataResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> DataResponse<T> success(String message) {
        return DataResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }
}

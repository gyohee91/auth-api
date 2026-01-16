package com.okbank.fintech.domain.notification.dto.request;

import com.okbank.fintech.domain.notification.enums.ChannelType;
import com.okbank.fintech.domain.notification.enums.SendType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "알림발송등록(요청)")
@Getter
public class NotificationSenderRequest {
    @Schema(description = "채널 타입", example = "SMS")
    @NotNull(message = "채널 타입은 필수입니다")
    private ChannelType channelType;

    @Schema(description = "제목", example = "테스트")
    @NotNull(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자 이내여야 합니다")
    private String title;

    @Schema(description = "내용", example = "테스트 내용")
    @NotNull(message = "내용은 필수입니다")
    @Size(max = 5000, message = "내용은 5000자 이내여야 합니다")
    private String contents;

    @Schema(description = "예약시간", example = "202601161250")
    @Pattern(regexp = "^$|^\\d{12}$", message = "예약시간은 yyyyMMddHHmm 이어야 합니다")
    private LocalDateTime scheduledAt;
}

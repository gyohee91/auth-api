package com.okbank.fintech.domain.notification.sender;

import com.okbank.fintech.domain.notification.dto.external.SendResultResponse;
import com.okbank.fintech.domain.notification.entity.Notification;
import com.okbank.fintech.domain.notification.enums.ChannelType;

/**
 * 채널별 발송 인터페이스
 * Strategy 패턴을 사용하여 채널 추가 시 기존 코드 수정 없이 확장 가능
 */
public interface ChannelSender {
    /**
     * 지원하는 타입 반환
     */
    ChannelType getSupportedChannel();

    /**
     * 알림 발송
     * @param notification  발송 정보(Entity)
     * @return  발송 타입(resultCode)
     */
    SendResultResponse send(Notification notification);
}

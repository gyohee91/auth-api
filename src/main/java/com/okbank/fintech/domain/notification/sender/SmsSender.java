package com.okbank.fintech.domain.notification.sender;

import com.okbank.fintech.domain.notification.dto.external.SendResultResponse;
import com.okbank.fintech.domain.notification.dto.external.SmsRequest;
import com.okbank.fintech.domain.notification.entity.Notification;
import com.okbank.fintech.domain.notification.enums.ChannelType;
import com.okbank.fintech.domain.notification.util.NotificationContextPropagator;
import com.okbank.fintech.global.listener.RetryLoggingListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.RetryListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsSender implements ChannelSender {
    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;
    private final RetryLoggingListener retryLoggingListener;
    private final NotificationContextPropagator notificationContextPropagator;

    private static final int MAX_RETRY_ATTEMPTS = 2;

    @Value("${notification.sender.base-url}")
    private String baseUrl;

    @Override
    public ChannelType getSupportedChannel() {
        return ChannelType.SMS;
    }

    @Override
    public SendResultResponse send(Notification notification) {
        SmsRequest request = SmsRequest.builder()
                .phoneNumber(notification.getRecipient())
                .title(notification.getTitle())
                .contents(notification.getContents())
                .build();

        try {
            //알림 Context 설정
            notificationContextPropagator.setNotificationContext(
                    notification.getId(),
                    notification.getChannelType()
            );

            //재시도 컨텍스트 설정 (재시도 횟수만)
            retryLoggingListener.setRetryContext( MAX_RETRY_ATTEMPTS);

            SendResultResponse response = retryTemplate.execute(
                    retryContext -> {
                        log.debug("SMS send attempt: notificationId={}, attempt={}/{}",
                                notification.getId(),
                                retryContext.getRetryCount() + 1,
                                MAX_RETRY_ATTEMPTS
                        );

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        HttpEntity<SmsRequest> entity = new HttpEntity<>(request, headers);

                        ResponseEntity<SendResultResponse> responseEntity = restTemplate.exchange(
                                baseUrl + ChannelType.SMS.getApiPath(),
                                HttpMethod.POST,
                                entity,
                                SendResultResponse.class
                        );

                        return responseEntity.getBody();
                    }
            );


            return response;
        } finally {
            //재시도 컨텍스트만 정리 (알림 컨텍스트는 상위에서 관리)
            retryLoggingListener.clearRetryContext();
        }
    }
}

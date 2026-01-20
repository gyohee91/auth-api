package com.okbank.fintech.domain.notification.sender;

import com.okbank.fintech.domain.notification.dto.external.SendResultResponse;
import com.okbank.fintech.domain.notification.dto.external.SmsRequest;
import com.okbank.fintech.domain.notification.entity.Notification;
import com.okbank.fintech.domain.notification.enums.ChannelType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsSender implements ChannelSender {
    private final WebClient webClient;

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
            SendResultResponse response = webClient.post()
                    .uri(ChannelType.SMS.getApiPath())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(SendResultResponse.class)
                    .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(1))
                            .filter(this::isRetryableException))
                    .timeout(Duration.ofSeconds(30))
                    .onErrorResume(e -> {
                        log.error("SMS send failed: notificationId={}", notification.getId(), e);
                        return Mono.just(new SendResultResponse("FAIL"));
                    })
                    .block();

            return response;
        } catch (Exception e) {
            log.error("Unexpected error in {}", ChannelType.SMS, e);
            throw e;
        }
    }

    private boolean isRetryableException(Throwable throwable) {
        return throwable instanceof WebClientResponseException
                && ((WebClientResponseException) throwable).getStatusCode().is5xxServerError();
    }
}

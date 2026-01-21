package com.okbank.fintech.domain.notification.sender;

import com.okbank.fintech.domain.notification.dto.external.EmailRequest;
import com.okbank.fintech.domain.notification.dto.external.SendResultResponse;
import com.okbank.fintech.domain.notification.entity.Notification;
import com.okbank.fintech.domain.notification.enums.ChannelType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSender implements ChannelSender {
    private final WebClient webClient;

    private static final String MDC_REQUEST_ID_KEY = "requestId";

    private static final int MAX_DELAY = 5;
    private static final Duration PER_ATTEMPT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration RETRY_DELAY = Duration.ofMillis(500);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @Value("${notification.sender.base-url}")
    private String baseUrl;

    @Override
    public ChannelType getSupportedChannel() {
        return ChannelType.EMAIL;
    }

    @Override
    public SendResultResponse send(Notification notification) {
        EmailRequest request = EmailRequest.builder()
                .emailAddress(notification.getRecipient())
                .title(notification.getTitle())
                .contents(notification.getContents())
                .build();

        try {
            return webClient.post()
                    .uri(baseUrl + ChannelType.EMAIL.getApiPath())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(SendResultResponse.class)
                    .timeout(PER_ATTEMPT_TIMEOUT)
                    .retryWhen(
                            Retry.backoff(MAX_DELAY, RETRY_DELAY)
                                    .maxBackoff(Duration.ofSeconds(3))
                                    .filter(this::isRetryableException)
                                    .doBeforeRetry(retrySignal -> {
                                        log.warn("Retrying send. ChannelType:{}, Attempt: {}/{}, Reason: {}",
                                                ChannelType.EMAIL,
                                                retrySignal.totalRetries() + 1,
                                                MAX_DELAY,
                                                retrySignal.failure().getClass().getSimpleName()
                                        );
                                    })
                                    .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                        log.error("Max retry attempts ({}) exhausted for {} send", ChannelType.EMAIL, MAX_DELAY);
                                        return retrySignal.failure();
                                    })
                    )
                    .timeout(TIMEOUT)
                    .doOnSuccess(result -> {
                        if(Objects.nonNull(result) && result.isSuccess()) {
                            log.info("Sent successfully: ChannelType={}, notificationId={}",
                                    ChannelType.EMAIL,
                                    notification.getId()
                            );
                        }
                    })
                    .doOnError(e -> log.error("Send encountered error: ChannelType={}, notificationId={}", ChannelType.EMAIL, notification.getId(), e))
                    .onErrorResume(e -> {
                        log.error("Send failed: ChannelType={}, notificationId={}", ChannelType.EMAIL, notification.getId(), e);
                        return Mono.just(SendResultResponse.fail("Send failed " + e.getMessage()));
                    })
                    .block();

        } catch (Exception e) {
            log.error("Unexpected error in {}", ChannelType.EMAIL, e);
            throw e;
        }
    }

    private boolean isRetryableException(Throwable throwable) {
        if(throwable instanceof TimeoutException) {
            return true;
        }

        if(throwable instanceof WebClientRequestException) {
            return true;
        }

        if(throwable instanceof WebClientResponseException
                && ((WebClientResponseException) throwable).getStatusCode().is5xxServerError()) {
            return true;
        }

        return false;
    }

}

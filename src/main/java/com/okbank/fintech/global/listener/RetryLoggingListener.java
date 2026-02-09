package com.okbank.fintech.global.listener;

import com.okbank.fintech.domain.notification.enums.ChannelType;
import com.okbank.fintech.domain.notification.util.NotificationContextPropagator;
import com.okbank.fintech.global.util.RequestIdPropagator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * ThreadLocal 기반 Retry 로깅 리스너
 * - 멀티스레드 환경에서 안전
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetryLoggingListener implements RetryListener {
    private final RequestIdPropagator requestIdPropagator;
    private final NotificationContextPropagator notificationContextPropagator;

    //재시도 관련 정보만 MDC에 저장
    private static final String MDC_RETRY_ATTEMPT = "retry.attempt";
    private static final String MDC_RETRY_MAX_ATTEMPTS = "retry.maxAttempts";

    /**
     * Retry 시작 전 컨텍스트 설정
     */
    public void setRetryContext(int maxAttempts) {
        MDC.put(MDC_RETRY_MAX_ATTEMPTS, String.valueOf(maxAttempts));
    }

    /**
     * Retry 완료 후 컨텍스트 정리
     */
    public void clearRetryContext() {
        MDC.remove(MDC_RETRY_ATTEMPT);
        MDC.remove(MDC_RETRY_MAX_ATTEMPTS);
    }

    /**
     * Retry 시작 시 호출
     * @param context the current {@link RetryContext}.
     * @param callback the current {@link RetryCallback}.
     * @return
     * @param <T>
     * @param <E>
     */
    @Override
    public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
        //첫 시도 시작
        log.debug("[{}] Starting retry process: notificationId={}, channelType={}",
                requestIdPropagator.getCurrentRequestId(),
                notificationContextPropagator.getCurrentNotificationId(),
                notificationContextPropagator.getCurrentChannelType()
        );

        return true;
    }

    /**
     * Retry 종료 시 호출
     * @param context the current {@link RetryContext}.
     * @param callback the current {@link RetryCallback}.
     * @param throwable the last exception that was thrown by the callback.
     * @param <T>
     * @param <E>
     */
    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        String requestId = requestIdPropagator.getCurrentRequestId();
        Long notificationId = notificationContextPropagator.getCurrentNotificationId();
        ChannelType channelType = notificationContextPropagator.getCurrentChannelType();

        String maxAttemptsStr = MDC.get(MDC_RETRY_MAX_ATTEMPTS);
        Integer maxAttempts = Objects.nonNull(maxAttemptsStr) ? Integer.parseInt(maxAttemptsStr) : 0;

        //성공
        int totalAttempts = context.getRetryCount() + 1;

        //최종 실패 시에만 로깅
        if(Objects.nonNull(throwable)) {
            log.error("[{}] Retry exhausted: notificationId={}, channelType={}, totalAttempts: {}",
                    requestId,
                    notificationId,
                    channelType,
                    context.getRetryCount() + 1
            );
        }
        else {
            if(totalAttempts > 1) {
                log.info("[{}] Retry succeeded: notificationId={}, channelType={}, succeededAttempt={}/{}",
                        requestId,
                        notificationId,
                        channelType,
                        totalAttempts,
                        maxAttempts
                );
            }

        }
    }

    /**
     * 각 시도 실패 시 호출
     * @param context the current {@link RetryContext}.
     * @param callback the current {@link RetryCallback}.
     * @param throwable the last exception that was thrown by the callback.
     * @param <T>
     * @param <E>
     */
    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        String requestId = requestIdPropagator.getCurrentRequestId();
        Long notificationId = notificationContextPropagator.getCurrentNotificationId();
        ChannelType channelType = notificationContextPropagator.getCurrentChannelType();

        String maxAttemptsStr = MDC.get(MDC_RETRY_MAX_ATTEMPTS);
        Integer maxAttempts = Objects.nonNull(maxAttemptsStr) ? Integer.parseInt(maxAttemptsStr) : 0;

        int currentAttempt = context.getRetryCount() + 1;

        //현재 시도 횟수 MDC 업데이트
        MDC.put(MDC_RETRY_ATTEMPT, String.valueOf(currentAttempt));

        //재시도가 남아있을 때만 로깅
        if(currentAttempt < maxAttempts) {
            log.warn("[{}] Retrying scheduled: notificationId={}, channelType={}, attempt={}/{}, error={}",
                    requestId,
                    notificationId,
                    channelType,
                    currentAttempt,    //다음 시도 번호
                    maxAttempts,
                    throwable.getMessage()
            );
        }
        else {
            log.error("[{}] Retrying attempt failed: notificationId={}, channelType={}, attempt={}/{}, error={}",
                    requestId,
                    notificationId,
                    channelType,
                    currentAttempt,
                    maxAttempts,
                    throwable.getMessage()
            );
        }
    }
}

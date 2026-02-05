package com.okbank.fintech.global.listener;

import com.okbank.fintech.domain.notification.enums.ChannelType;
import com.okbank.fintech.global.util.RequestIdPropagator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ThreadLocal 기반 Retry 로깅 리스너
 * - 멀티스레드 환경에서 안전
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetryLoggingListener implements RetryListener {
    private final RequestIdPropagator requestIdPropagator;

    private final Map<Thread, Long> notificationIdMap = new ConcurrentHashMap<>();
    private final Map<Thread, ChannelType> channelTypeMap = new ConcurrentHashMap<>();
    private final Map<Thread, Integer> maxAttemptMap = new ConcurrentHashMap<>();

    /**
     * Retry 시작 전 컨텍스트 설정
     */
    public void setContext(Long notificationId, ChannelType channelType, int maxAttempts) {
        Thread currentThread = Thread.currentThread();
        notificationIdMap.put(currentThread, notificationId);
        channelTypeMap.put(currentThread, channelType);
        maxAttemptMap.put(currentThread, maxAttempts);
    }

    /**
     * Retry 완료 후 컨텍스트 정리
     */
    public void clearContext() {
        Thread currentThread = Thread.currentThread();
        notificationIdMap.remove(currentThread);
        channelTypeMap.remove(currentThread);
        maxAttemptMap.remove(currentThread);
    }

    @Override
    public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
        //첫 시도 시작
        return true;
    }

    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        Thread currentThread = Thread.currentThread();
        Long notificationId = notificationIdMap.get(currentThread);
        ChannelType channelType = channelTypeMap.get(currentThread);
        Integer maxAttempts = maxAttemptMap.get(currentThread);

        String requestId = requestIdPropagator.getCurrentRequestId();

        //최종 실패 시에만 로깅
        if(Objects.nonNull(throwable) && context.getRetryCount() >= (Objects.nonNull(maxAttempts) ? maxAttempts : 0)) {
            log.error("[{}] {} send failed after {} attempts: notificationId={}",
                    requestId,
                    channelType,
                    context.getRetryCount(),
                    notificationId
            );
        }
    }

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        Thread currentThread = Thread.currentThread();
        Long notificationId = notificationIdMap.get(currentThread);
        ChannelType channelType = channelTypeMap.get(currentThread);
        Integer maxAttempts = maxAttemptMap.get(currentThread);

        String requestId = requestIdPropagator.getCurrentRequestId();

        //재시도가 남아있을 때만 로깅
        if(context.getRetryCount() < (Objects.nonNull(maxAttempts) ? maxAttempts : 0)) {
            log.error("[{}] Retrying {} send: notificationId={}, attempt={}/{}, reason={}",
                    requestId,
                    channelType,
                    notificationId,
                    context.getRetryCount() + 1,    //다음 시도 번호
                    maxAttempts,
                    throwable.getClass().getSimpleName()
            );
        }
    }
}

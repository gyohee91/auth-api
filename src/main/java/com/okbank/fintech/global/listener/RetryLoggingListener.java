package com.okbank.fintech.global.listener;

import com.okbank.fintech.domain.notification.enums.ChannelType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;

import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class RetryLoggingListener implements RetryListener {
    private final Long notificationId;
    private final ChannelType channelType;
    private final int maxAttempts;

    @Override
    public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
        //첫 시도 시작
        return true;
    }

    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        if(Objects.nonNull(throwable)) {
            log.error("{} send failed after {} attempts: notificationId={}",
                    channelType,
                    context.getRetryCount(),
                    notificationId
            );
        }
    }

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        //첫 시도는 로깅 안함 (실패 시에만 로딩)
        if(context.getRetryCount() > 0) {
            log.error("Retrying {} send: notificationId={}, attempt={}/{}, reason={}",
                    channelType,
                    notificationId,
                    context.getRetryCount() + 1,    //다음 시도 번호
                    maxAttempts,
                    throwable.getClass().getSimpleName()
            );
        }
    }
}

package com.okbank.fintech.global.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;
import java.util.Objects;

public class MdcTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
        // 현재 스레드(부모)의 MDC 컨텍스트 복사
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            try {
                if(Objects.nonNull(contextMap)) {
                    MDC.setContextMap(contextMap);
                }
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}

package com.okbank.fintech.domain.notification.service;

import com.okbank.fintech.domain.notification.dto.request.NotificationSenderRequest;
import com.okbank.fintech.domain.notification.dto.response.NotificationSenderResponse;
import com.okbank.fintech.domain.notification.entity.Notification;
import com.okbank.fintech.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    @Transactional
    public NotificationSenderResponse registerNotification(NotificationSenderRequest request) {

        Notification notification = Notification.builder()
                .channelType(request.getChannelType())
                .title(request.getTitle())
                .contents(request.getContents())
                .build();

        notificationRepository.save(notification);

        return NotificationSenderResponse.from(notification);
    }
}

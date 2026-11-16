package com.smartdesk.notification_service.service;

import com.smartdesk.notification_service.entity.Notification;
import com.smartdesk.notification_service.entity.NotificationStatus;
import com.smartdesk.notification_service.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRetryScheduler {

    private static final int MAX_RETRIES = 3;

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @Scheduled(fixedDelay = 60000)
    public void retryFailedNotifications() {

        List<Notification> failedNotifications =
                notificationRepository
                        .findTop50ByStatusAndRetryCountLessThanAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
                                NotificationStatus.FAILED,
                                MAX_RETRIES,
                                LocalDateTime.now()
                        );

        for (Notification notification : failedNotifications) {

            log.info(
                    "Retrying email notification ID: {}",
                    notification.getId()
            );

            notificationService.retryNotification(notification);
        }
    }
}
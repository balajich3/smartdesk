package com.smartdesk.notification_service.dto;

import com.smartdesk.notification_service.entity.Notification;
import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String type,
        String subject,
        String message,
        boolean read,
        LocalDateTime createdAt,
        LocalDateTime sentAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType().name(),
                notification.getSubject(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getSentAt()
        );
    }
}
package com.smartdesk.notification_service.service;

import com.smartdesk.notification_service.entity.Notification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketNotificationService(
            SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendToUser(
            String recipientEmail,
            Notification notification) {

        messagingTemplate.convertAndSendToUser(
                recipientEmail,
                "/queue/notifications",
                notification
        );
    }
}
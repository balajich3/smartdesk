package com.smartdesk.notification_service.event;

import lombok.Data;

import java.util.UUID;

@Data
public class EmailOtpEvent {

    private UUID eventId;
    private String eventType;

    private String recipientEmail;
    private String recipientName;

    private String otp;
    private int expiryMinutes;
}
package com.smartdesk.ticket_service.event;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class EmailOtpEvent {

    private UUID eventId;
    private String eventType;

    private String recipientEmail;
    private String recipientName;

    private String otp;
    private int expiryMinutes;
}
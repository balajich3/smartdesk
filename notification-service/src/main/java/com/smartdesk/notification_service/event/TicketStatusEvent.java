package com.smartdesk.notification_service.event;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TicketStatusEvent {

    private UUID eventId;
    private Long ticketId;
    private String ticketNumber;
    private String title;

    private String status;
    private String recipientEmail;
    private String recipientName;

    private LocalDateTime updatedAt;
}
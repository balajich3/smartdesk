package com.smartdesk.ticket_service.event;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TicketCreatedEvent {

    private UUID eventId;
    private String eventType;

    private Long ticketId;
    private String ticketNumber;

    private String title;
    private String summary;

    private String priority;
    private String category;
    private String department;

    private String requesterName;
    private String requesterEmail;

    private LocalDateTime createdAt;

    private List<String> recipientEmails;
}
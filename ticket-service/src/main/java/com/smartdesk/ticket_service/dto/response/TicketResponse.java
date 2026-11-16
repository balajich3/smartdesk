package com.smartdesk.ticket_service.dto.response;

import com.smartdesk.ticket_service.entity.TicketPriority;
import com.smartdesk.ticket_service.entity.TicketStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TicketResponse {

    private Long id;
    private String ticketNumber;
    private String title;
    private String description;
    private String aiSummary;
    private String category;
    private TicketPriority priority;
    private TicketStatus status;
    private String department;
    private String requester;
    private String requesterEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String assignedAgent;
}
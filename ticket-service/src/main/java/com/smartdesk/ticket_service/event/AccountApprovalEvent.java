package com.smartdesk.ticket_service.event;
import lombok.Data;
import java.util.UUID;
@Data
public class AccountApprovalEvent {
    private UUID eventId;
    private String recipientEmail;
    private String recipientName;
    private String role;
    private String department;
}

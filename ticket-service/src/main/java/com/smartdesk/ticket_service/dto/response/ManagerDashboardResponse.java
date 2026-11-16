package com.smartdesk.ticket_service.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ManagerDashboardResponse {

    private long totalTickets;
    private long openTickets;
    private long unassignedTickets;
    private long inProgressTickets;
    private long waitingForRequesterTickets;
    private long resolvedTickets;
    private long closedTickets;
}
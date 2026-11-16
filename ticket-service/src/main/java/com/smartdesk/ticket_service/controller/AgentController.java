package com.smartdesk.ticket_service.controller;

import com.smartdesk.ticket_service.dto.request.UpdateTicketStatusRequest;
import com.smartdesk.ticket_service.dto.response.TicketResponse;
import com.smartdesk.ticket_service.service.TicketQueueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final TicketQueueService ticketQueueService;

    @GetMapping("/queue")
    @PreAuthorize("hasRole('SUPPORT_AGENT')")
    public Page<TicketResponse> getDepartmentQueue(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Invalid pagination parameters");
        }

        Pageable pageable = PageRequest.of(page, size);

        return ticketQueueService.getDepartmentQueue(
                authentication.getName(),
                pageable
        );
    }
    @PostMapping("/tickets/{ticketId}/claim")
    @PreAuthorize("hasRole('SUPPORT_AGENT')")
    public ResponseEntity<String> claimTicket(
            @PathVariable Long ticketId,
            Authentication authentication
    ) {

        ticketQueueService.claimTicket(
                authentication.getName(),
                ticketId
        );

        return ResponseEntity.ok("Ticket claimed successfully");
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasRole('SUPPORT_AGENT')")
    public Page<TicketResponse> getAssignedTickets(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Invalid pagination parameters");
        }

        return ticketQueueService.getAssignedTickets(
                authentication.getName(),
                PageRequest.of(page, size)
        );
    }

    @PatchMapping("/tickets/{ticketId}/status")
    @PreAuthorize("hasRole('SUPPORT_AGENT')")
    public ResponseEntity<String> updateTicketStatus(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateTicketStatusRequest request,
            Authentication authentication
    ) {

        ticketQueueService.updateTicketStatus(
                authentication.getName(),
                ticketId,
                request.getStatus()
        );

        return ResponseEntity.ok("Ticket status updated successfully");
    }
}
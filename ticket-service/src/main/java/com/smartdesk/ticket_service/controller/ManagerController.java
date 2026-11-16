package com.smartdesk.ticket_service.controller;

import com.smartdesk.ticket_service.dto.response.AdminUserResponse;
import com.smartdesk.ticket_service.dto.response.ManagerDashboardResponse;
import com.smartdesk.ticket_service.dto.response.TicketResponse;
import com.smartdesk.ticket_service.service.ManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class ManagerController {

    private final ManagerService managerService;

    @GetMapping("/dashboard")
    public ManagerDashboardResponse getDashboard(
            Authentication authentication
    ) {
        return managerService.getDashboard(authentication.getName());
    }

    @GetMapping("/tickets")
    public Page<TicketResponse> getDepartmentTickets(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Invalid pagination parameters"
            );
        }

        return managerService.getDepartmentTickets(
                authentication.getName(),
                PageRequest.of(page, size)
        );
    }

    @GetMapping("/agents/pending")
    @PreAuthorize("hasRole('MANAGER')")
    public List<AdminUserResponse> getPendingAgents(
            Authentication authentication) {
        return managerService.getPendingAgents(authentication.getName());
    }

    @PatchMapping("/agents/{agentId}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<String> approveAgent(
            @PathVariable Long agentId,
            Authentication authentication) {
        managerService.approveAgent(authentication.getName(), agentId);
        return ResponseEntity.ok("Agent approved successfully");
    }

    @PatchMapping("/agents/{agentId}/reject")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<String> rejectAgent(
            @PathVariable Long agentId,
            Authentication authentication) {
        managerService.rejectAgent(authentication.getName(), agentId);
        return ResponseEntity.ok("Agent rejected successfully");
    }
}
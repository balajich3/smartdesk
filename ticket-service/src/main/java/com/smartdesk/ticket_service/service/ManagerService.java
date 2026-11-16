package com.smartdesk.ticket_service.service;

import com.smartdesk.ticket_service.dto.response.AdminUserResponse;
import com.smartdesk.ticket_service.dto.response.ManagerDashboardResponse;
import com.smartdesk.ticket_service.dto.response.TicketResponse;
import com.smartdesk.ticket_service.entity.ApprovalStatus;
import com.smartdesk.ticket_service.entity.Ticket;
import com.smartdesk.ticket_service.entity.TicketStatus;
import com.smartdesk.ticket_service.entity.User;
import com.smartdesk.ticket_service.repository.TicketRepository;
import com.smartdesk.ticket_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import com.smartdesk.ticket_service.entity.Role;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManagerService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ManagerDashboardResponse getDashboard(String email) {

        User manager = getManager(email);

        Long departmentId = manager.getDepartment().getId();

        return ManagerDashboardResponse.builder()
                .totalTickets(
                        ticketRepository.countByDepartmentId(departmentId)
                )
                .openTickets(
                        ticketRepository.countByDepartmentIdAndStatus(
                                departmentId, TicketStatus.OPEN
                        )
                )
                .unassignedTickets(
                        ticketRepository
                                .countByDepartmentIdAndAssignedAgentIsNullAndStatus(
                                        departmentId, TicketStatus.OPEN
                                )
                )
                .inProgressTickets(
                        ticketRepository.countByDepartmentIdAndStatus(
                                departmentId, TicketStatus.IN_PROGRESS
                        )
                )
                .waitingForRequesterTickets(
                        ticketRepository.countByDepartmentIdAndStatus(
                                departmentId, TicketStatus.WAITING_FOR_REQUESTER
                        )
                )
                .resolvedTickets(
                        ticketRepository.countByDepartmentIdAndStatus(
                                departmentId, TicketStatus.RESOLVED
                        )
                )
                .closedTickets(
                        ticketRepository.countByDepartmentIdAndStatus(
                                departmentId, TicketStatus.CLOSED
                        )
                )
                .build();
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> getDepartmentTickets(
            String email,
            Pageable pageable
    ) {

        User manager = getManager(email);

        return ticketRepository
                .findByDepartmentIdOrderByCreatedAtDesc(
                        manager.getDepartment().getId(),
                        pageable
                )
                .map(this::mapToResponse);
    }

    private User getManager(String email) {

        User manager = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Manager not found"
                        )
                );

        if (!manager.isActive() || manager.getDepartment() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Invalid manager account"
            );
        }

        return manager;
    }

    private TicketResponse mapToResponse(Ticket ticket) {

        return TicketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .aiSummary(ticket.getAiSummary())
                .category(ticket.getCategory())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .department(ticket.getDepartment().getName())
                .requester(ticket.getRequester().getFullName())
                .assignedAgent(
                        ticket.getAssignedAgent() != null
                                ? ticket.getAssignedAgent().getFullName()
                                : null
                )
                .createdAt(ticket.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getPendingAgents(String email) {

        User manager = getManager(email);

        return userRepository.findAgentsByDepartmentAndApprovalStatus(
                manager.getDepartment().getId(),
                ApprovalStatus.PENDING
        ).stream().map(this::mapAgentResponse).toList();
    }

    @Transactional
    public void approveAgent(String email, Long agentId) {
        updateAgentApproval(email, agentId, ApprovalStatus.APPROVED);
    }

    @Transactional
    public void rejectAgent(String email, Long agentId) {
        updateAgentApproval(email, agentId, ApprovalStatus.REJECTED);
    }

    private void updateAgentApproval(
            String email,
            Long agentId,
            ApprovalStatus status
    ) {
        User manager = getManager(email);

        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Agent not found"));

        boolean isSupportAgent = agent.getRoles().stream()
                .anyMatch(role -> role.getName().equals("SUPPORT_AGENT"));

        if (!isSupportAgent
                || !manager.getDepartment().getId()
                .equals(agent.getDepartment().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only manage agents in your department"
            );
        }

        if (agent.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Agent is not awaiting approval"
            );
        }

        agent.setApprovalStatus(status);
    }

    private AdminUserResponse mapAgentResponse(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return AdminUserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .department(user.getDepartment().getName())
                .roles(roles)
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
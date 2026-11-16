package com.smartdesk.ticket_service.service;

import com.smartdesk.ticket_service.client.NotificationClient;
import com.smartdesk.ticket_service.dto.response.TicketResponse;
import com.smartdesk.ticket_service.entity.ApprovalStatus;
import com.smartdesk.ticket_service.entity.Ticket;
import com.smartdesk.ticket_service.entity.TicketStatus;
import com.smartdesk.ticket_service.entity.User;
import com.smartdesk.ticket_service.event.TicketStatusEvent;
import com.smartdesk.ticket_service.repository.TicketRepository;
import com.smartdesk.ticket_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketQueueService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final NotificationClient notificationClient;
    private final TicketHistoryService ticketHistoryService;

    @Transactional(readOnly = true)
    public Page<TicketResponse> getDepartmentQueue(
            String email,
            Pageable pageable
    ) {

        User agent = getApprovedAgent(email);

        Page<Ticket> tickets =
                ticketRepository
                        .findByDepartmentIdAndAssignedAgentIsNullAndStatusOrderByCreatedAtAsc(
                                agent.getDepartment().getId(),
                                TicketStatus.OPEN,
                                pageable
                        );

        return tickets.map(this::mapToResponse);
    }

    @Transactional
    public void claimTicket(String email, Long ticketId) {

        User agent = getApprovedAgent(email);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket not found"
                ));

        if (ticket.getDepartment() == null ||
                !ticket.getDepartment().getId()
                        .equals(agent.getDepartment().getId())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot claim tickets from another department"
            );
        }

        if (ticket.getAssignedAgent() != null ||
                ticket.getStatus() != TicketStatus.OPEN) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ticket is already assigned or unavailable"
            );
        }

        ticket.setAssignedAgent(agent);
        ticket.setStatus(TicketStatus.ASSIGNED);
        ticket.setClaimedAt(LocalDateTime.now());

        ticketRepository.save(ticket);

        ticketHistoryService.record(
                ticket,
                agent,
                "CLAIMED",
                TicketStatus.OPEN.name(),
                TicketStatus.ASSIGNED.name()
        );
        notifyEmployee(ticket);
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> getAssignedTickets(
            String email,
            Pageable pageable
    ) {

        User agent = getApprovedAgent(email);

        return ticketRepository
                .findByAssignedAgentIdOrderByUpdatedAtDesc(
                        agent.getId(),
                        pageable
                )
                .map(this::mapToResponse);
    }

    @Transactional
    public void updateTicketStatus(String email, Long ticketId, TicketStatus newStatus) {
        User agent = getApprovedAgent(email);
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        if (ticket.getAssignedAgent() == null ||
                !ticket.getAssignedAgent().getId().equals(agent.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not assigned to this ticket");
        }

        TicketStatus oldStatus = ticket.getStatus();
        boolean valid = switch (oldStatus) {
            case ASSIGNED -> newStatus == TicketStatus.IN_PROGRESS;
            case IN_PROGRESS -> newStatus == TicketStatus.WAITING_FOR_REQUESTER;
            case REQUESTER_APPROVED -> newStatus == TicketStatus.RESOLVED;
            default -> false;
        };
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Invalid status transition. Agent flow: Assigned → In progress → Waiting for employee → employee approves → Resolved.");
        }

        ticket.setStatus(newStatus);
        if (newStatus == TicketStatus.RESOLVED) ticket.setResolvedAt(LocalDateTime.now());
        ticketRepository.save(ticket);
        ticketHistoryService.record(ticket, agent, "STATUS_CHANGED", oldStatus.name(), newStatus.name());
        notifyEmployee(ticket);
    }

    private User getApprovedAgent(String email) {

        User agent = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        if (!agent.isActive()
                || agent.getDepartment() == null
                || agent.getApprovalStatus() != ApprovalStatus.APPROVED) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Agent account is inactive or not approved"
            );
        }

        return agent;
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
                .requesterEmail(ticket.getRequester().getEmail())
                .assignedAgent(
                        ticket.getAssignedAgent() != null
                                ? ticket.getAssignedAgent().getFullName()
                                : null
                )
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }
    private void notifyEmployee(Ticket ticket) {

        TicketStatusEvent event = new TicketStatusEvent();

        event.setEventId(UUID.randomUUID());
        event.setTicketId(ticket.getId());
        event.setTicketNumber(ticket.getTicketNumber());
        event.setTitle(ticket.getTitle());
        event.setStatus(ticket.getStatus().name());

        event.setRecipientEmail(ticket.getRequester().getEmail());
        event.setRecipientName(ticket.getRequester().getFullName());

        event.setUpdatedAt(LocalDateTime.now());

        notificationClient.sendTicketStatusNotification(event);
    }

    @Transactional
    public TicketResponse acknowledgeResolution(String email, Long ticketId, boolean accepted) {
        Ticket ticket = ticketRepository.findByIdAndRequesterEmail(ticketId, email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        if (ticket.getStatus() != TicketStatus.WAITING_FOR_REQUESTER) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You can respond only when the ticket is waiting for your response");
        }

        TicketStatus oldStatus = ticket.getStatus();
        TicketStatus newStatus = accepted
                ? TicketStatus.REQUESTER_APPROVED
                : TicketStatus.IN_PROGRESS;

        ticket.setStatus(newStatus);
        ticketRepository.save(ticket);
        ticketHistoryService.record(ticket, ticket.getRequester(),
                accepted ? "REQUESTER_APPROVED" : "REQUESTER_REJECTED",
                oldStatus.name(), newStatus.name());
        notifyEmployee(ticket);
        return mapToResponse(ticket);
    }

}
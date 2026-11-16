package com.smartdesk.ticket_service.service;

import com.smartdesk.ticket_service.dto.request.CreateTicketRequest;
import com.smartdesk.ticket_service.dto.response.AiTriageResponse;
import com.smartdesk.ticket_service.dto.response.TicketResponse;
import com.smartdesk.ticket_service.entity.*;
import com.smartdesk.ticket_service.event.TicketCreatedEvent;
import com.smartdesk.ticket_service.repository.DepartmentRepository;
import com.smartdesk.ticket_service.repository.TicketRepository;
import com.smartdesk.ticket_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartdesk.ticket_service.client.NotificationClient;
import org.springframework.web.server.ResponseStatusException;


import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final TicketPersistenceService ticketPersistenceService;
    private final AiTriageService aiTriageService;
    private final NotificationClient notificationClient;

    public TicketResponse createTicket(
            CreateTicketRequest request,
            String email,
            String idempotencyKey
    ) {

        User requester = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        // Check whether this request was already processed.
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key is required");
        }

        var existing = ticketRepository.findByRequesterIdAndIdempotencyKey(
                requester.getId(),
                idempotencyKey
        );

        if (existing.isPresent()) {
            Ticket previousTicket = existing.get();

            if (!previousTicket.getTitle().equals(request.getTitle().trim())
                    || !previousTicket.getDescription().equals(request.getDescription().trim())) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "Idempotency key was already used for a different request"
                );
            }

            return mapToResponse(previousTicket);
        }

        // Call AI Triage Service.
        AiTriageResponse aiResponse = aiTriageService.analyze(
                request.getTitle(),
                request.getDescription()
        );

        // Convert AI category and priority.
        String departmentCode = aiResponse.getCategory()
                .trim()
                .toUpperCase(Locale.ROOT);

        Department department = departmentRepository
                .findByCode(departmentCode)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "AI returned an unsupported department: "
                                        + departmentCode
                        )
                );

        TicketPriority priority;

        try {
            priority = TicketPriority.valueOf(
                    aiResponse.getPriority().trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "AI returned an unsupported priority"
            );
        }

        // Generate ticket.
        Ticket ticket = Ticket.builder()
                .ticketNumber("SD-" +
                        UUID.randomUUID().toString()
                                .substring(0, 8).toUpperCase())
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .aiSummary(aiResponse.getSummary())
                .category(aiResponse.getCategory())
                .priority(priority)
                .status(TicketStatus.OPEN)
                .requester(requester)
                .department(department)
                .idempotencyKey(
                        idempotencyKey == null || idempotencyKey.isBlank()
                                ? null : idempotencyKey
                )
                .build();

        Ticket savedTicket = ticketPersistenceService.save(ticket, requester);

        List<String> recipientEmails =
                userRepository.findActiveSupportAgentsByDepartment(
                                savedTicket.getDepartment().getId()
                        )
                        .stream()
                        .filter(agent -> !agent.getEmail()
                                .equalsIgnoreCase(requester.getEmail()))
                        .map(User::getEmail)
                        .toList();

        TicketCreatedEvent event = TicketCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("TICKET_CREATED")
                .ticketId(savedTicket.getId())
                .ticketNumber(savedTicket.getTicketNumber())
                .title(savedTicket.getTitle())
                .summary(savedTicket.getAiSummary())
                .priority(savedTicket.getPriority().name())
                .category(savedTicket.getCategory())
                .department(savedTicket.getDepartment().getName())
                .requesterName(requester.getFullName())
                .requesterEmail(requester.getEmail())
                .createdAt(savedTicket.getCreatedAt())
                .recipientEmails(recipientEmails)
                .build();

        notificationClient.sendTicketCreatedNotification(event);


        return mapToResponse(savedTicket);
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> getMyTickets(
            String email,
            Pageable pageable
    ) {

        User requester = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        if (!requester.isActive()) {
            throw new IllegalArgumentException("Account is disabled");
        }

        return ticketRepository
                .findByRequesterIdOrderByCreatedAtDesc(
                        requester.getId(),
                        pageable
                )
                .map(this::mapToResponse);
    }

    @Transactional
    public TicketResponse getTicket(Long ticketId, String email) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Ticket not found"));
        User viewer = userRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "User not found"));
        boolean requester = ticket.getRequester().getId().equals(viewer.getId());
        boolean assignedAgent = ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getId().equals(viewer.getId());
        boolean sameDepartment = viewer.getDepartment() != null && viewer.getDepartment().getId().equals(ticket.getDepartment().getId());
        boolean agent = viewer.getRoles().stream().anyMatch(r -> r.getName().equals("SUPPORT_AGENT"));
        boolean admin = viewer.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN"));
        boolean manager = viewer.getRoles().stream().anyMatch(r -> r.getName().equals("MANAGER"));
        if (!(requester || assignedAgent || admin || ((manager || agent) && sameDepartment))) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "You cannot view this ticket");
        return mapToResponse(ticket);
    }

    @Transactional
    public TicketResponse withdrawTicket(Long ticketId, String email) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Ticket not found"));
        if (!ticket.getRequester().getEmail().equalsIgnoreCase(email)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "You can withdraw only your own tickets");

        if (ticket.getStatus() != TicketStatus.OPEN
                || ticket.getAssignedAgent() != null) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ticket cannot be withdrawn after an agent claims it"
            );
        }
        ticket.setStatus(TicketStatus.CANCELLED);
        ticketRepository.save(ticket);
        return mapToResponse(ticket);
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
}
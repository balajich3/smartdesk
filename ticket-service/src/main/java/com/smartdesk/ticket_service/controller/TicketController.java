package com.smartdesk.ticket_service.controller;

import com.smartdesk.ticket_service.dto.request.CreateCommentRequest;
import com.smartdesk.ticket_service.dto.request.CreateTicketRequest;
import com.smartdesk.ticket_service.dto.response.CommentResponse;
import com.smartdesk.ticket_service.dto.response.TicketResponse;
import com.smartdesk.ticket_service.service.TicketCommentService;
import com.smartdesk.ticket_service.service.TicketQueueService;
import com.smartdesk.ticket_service.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {
    private static final String ALL_USER_ROLES = "hasAnyRole('EMPLOYEE','SUPPORT_AGENT','MANAGER','ADMIN')";
    private final TicketService ticketService;
    private final TicketQueueService ticketQueueService;
    private final TicketCommentService ticketCommentService;

    @PostMapping
    @PreAuthorize(ALL_USER_ROLES)
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody CreateTicketRequest request,
                                                       @RequestHeader("Idempotency-Key") String idempotencyKey, Authentication authentication) {
        TicketResponse response = ticketService.createTicket(request, authentication.getName(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{ticketId}")
    @PreAuthorize(ALL_USER_ROLES)
    public ResponseEntity<TicketResponse> getTicket(@PathVariable Long ticketId, Authentication authentication) {
        return ResponseEntity.ok(ticketService.getTicket(ticketId, authentication.getName()));
    }

    @PostMapping("/{ticketId}/withdraw")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<TicketResponse> withdrawTicket(@PathVariable Long ticketId, Authentication authentication) {
        return ResponseEntity.ok(ticketService.withdrawTicket(ticketId, authentication.getName()));
    }

    @GetMapping("/my")
    @PreAuthorize(ALL_USER_ROLES)
    public Page<TicketResponse> getMyTickets(Authentication authentication,
                                             @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid pagination parameters");
        return ticketService.getMyTickets(authentication.getName(), PageRequest.of(page, size));
    }

    @GetMapping("/{ticketId}/comments")
    @PreAuthorize(ALL_USER_ROLES)
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long ticketId, Authentication authentication) {
        return ResponseEntity.ok(ticketCommentService.getComments(ticketId, authentication.getName()));
    }

    @PostMapping("/{ticketId}/comments")
    @PreAuthorize(ALL_USER_ROLES)
    public ResponseEntity<CommentResponse> addComment(@PathVariable Long ticketId,
                                                      @Valid @RequestBody CreateCommentRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketCommentService.addComment(ticketId, authentication.getName(), request));
    }

    @PostMapping("/{ticketId}/acknowledgement")
    @PreAuthorize("hasAnyRole('EMPLOYEE','SUPPORT_AGENT','MANAGER','ADMIN')")
    public ResponseEntity<TicketResponse> acknowledgeResolution(@PathVariable Long ticketId,
                                                                @RequestParam boolean accepted, Authentication authentication) {
        return ResponseEntity.ok(ticketQueueService.acknowledgeResolution(authentication.getName(), ticketId, accepted));
    }
}

package com.smartdesk.ticket_service.service;

import com.smartdesk.ticket_service.dto.request.CreateCommentRequest;
import com.smartdesk.ticket_service.dto.response.CommentResponse;
import com.smartdesk.ticket_service.entity.*;
import com.smartdesk.ticket_service.repository.TicketCommentRepository;
import com.smartdesk.ticket_service.repository.TicketRepository;
import com.smartdesk.ticket_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketCommentService {

    private final TicketCommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketHistoryService ticketHistoryService;

    @Transactional
    public CommentResponse addComment(
            Long ticketId,
            String email,
            CreateCommentRequest request
    ) {

        User user = getUser(email);
        Ticket ticket = getTicket(ticketId);

        boolean isRequester = ticket.getRequester().getId()
                .equals(user.getId());

        boolean isAssignedAgent = ticket.getAssignedAgent() != null
                && ticket.getAssignedAgent().getId().equals(user.getId());

        boolean isSupportAgent = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("SUPPORT_AGENT"));

        if (!isRequester && !isAssignedAgent) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot comment on this ticket"
            );
        }

        if (request.isInternalNote() && !isAssignedAgent) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the assigned support agent can add internal notes"
            );
        }

        if (request.isInternalNote() && !isSupportAgent) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Internal notes are restricted to support agents"
            );
        }

        TicketComment comment = TicketComment.builder()
                .ticket(ticket)
                .author(user)
                .message(request.getMessage().trim())
                .internalNote(request.isInternalNote())
                .build();

        TicketComment savedComment = commentRepository.save(comment);

        ticketHistoryService.record(
                ticket,
                user,
                request.isInternalNote()
                        ? "INTERNAL_NOTE_ADDED"
                        : "COMMENT_ADDED",
                null,
                savedComment.getId().toString()
        );

        return mapToResponse(savedComment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(
            Long ticketId,
            String email
    ) {

        User user = getUser(email);
        Ticket ticket = getTicket(ticketId);

        boolean isRequester = ticket.getRequester().getId()
                .equals(user.getId());

        boolean isAssignedAgent = ticket.getAssignedAgent() != null
                && ticket.getAssignedAgent().getId().equals(user.getId());

        if (!isRequester && !isAssignedAgent) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot view comments for this ticket"
            );
        }

        List<TicketComment> comments =
                commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);

        return comments.stream()
                .filter(comment ->
                        !comment.isInternalNote() || isAssignedAgent
                )
                .map(this::mapToResponse)
                .toList();
    }

    private User getUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "User not found"
                        )
                );

        if (!user.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Account is disabled"
            );
        }

        return user;
    }

    private Ticket getTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Ticket not found"
                        )
                );
    }

    private CommentResponse mapToResponse(TicketComment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .message(comment.getMessage())
                .internalNote(comment.isInternalNote())
                .authorName(comment.getAuthor().getFullName())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
package com.smartdesk.ticket_service.service;

import com.smartdesk.ticket_service.entity.Ticket;
import com.smartdesk.ticket_service.entity.User;
import com.smartdesk.ticket_service.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketPersistenceService {

    private final TicketRepository ticketRepository;
    private final TicketHistoryService ticketHistoryService;

    @Transactional
    public Ticket save(Ticket ticket, User requester) {

        Ticket savedTicket = ticketRepository.saveAndFlush(ticket);

        ticketHistoryService.record(
                savedTicket,
                requester,
                "CREATED",
                null,
                "OPEN"
        );

        return savedTicket;
    }
}
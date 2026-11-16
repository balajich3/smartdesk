package com.smartdesk.ticket_service.service;

import com.smartdesk.ticket_service.entity.Ticket;
import com.smartdesk.ticket_service.entity.TicketHistory;
import com.smartdesk.ticket_service.entity.User;
import com.smartdesk.ticket_service.repository.TicketHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketHistoryService {

    private final TicketHistoryRepository historyRepository;

    public void record(
            Ticket ticket,
            User actor,
            String eventType,
            String oldValue,
            String newValue
    ) {
        TicketHistory history = TicketHistory.builder()
                .ticket(ticket)
                .actor(actor)
                .eventType(eventType)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();

        historyRepository.save(history);
    }
}
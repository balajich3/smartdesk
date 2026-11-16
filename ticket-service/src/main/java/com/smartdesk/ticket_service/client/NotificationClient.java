package com.smartdesk.ticket_service.client;

import com.smartdesk.ticket_service.event.EmailOtpEvent;
import com.smartdesk.ticket_service.event.TicketCreatedEvent;

import com.smartdesk.ticket_service.event.TicketStatusEvent;
import com.smartdesk.ticket_service.event.AccountApprovalEvent;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/api/notifications/otp")
    void sendOtp(@RequestBody EmailOtpEvent event);

    @PostMapping("/api/notifications/ticket-created")
    void sendTicketCreatedNotification(
            @RequestBody TicketCreatedEvent event
    );
    @PostMapping("/api/notifications/ticket-status")
    void sendTicketStatusNotification(
            @RequestBody TicketStatusEvent event
    );

    @PostMapping("/api/notifications/account-approved")
    void sendAccountApprovedNotification(@RequestBody AccountApprovalEvent event);
}

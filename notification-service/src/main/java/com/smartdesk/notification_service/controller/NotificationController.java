package com.smartdesk.notification_service.controller;

import com.smartdesk.notification_service.dto.NotificationResponse;
import com.smartdesk.notification_service.event.EmailOtpEvent;
import com.smartdesk.notification_service.event.TicketCreatedEvent;
import com.smartdesk.notification_service.event.TicketStatusEvent;
import com.smartdesk.notification_service.event.AccountApprovalEvent;
import com.smartdesk.notification_service.service.NotificationService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Send OTP email
    @PostMapping("/otp")
    public ResponseEntity<String> sendOtp(
            @RequestBody EmailOtpEvent event) {

        notificationService.sendOtpNotification(event);

        return ResponseEntity.ok("OTP email processed successfully");
    }

    // Send ticket-created notification emails
    @PostMapping("/ticket-created")
    public ResponseEntity<String> sendTicketCreatedNotification(
            @RequestBody TicketCreatedEvent event) {

        notificationService.sendTicketCreatedNotification(event);

        return ResponseEntity.ok(
                "Ticket notifications processed successfully"
        );
    }

    @PostMapping("/ticket-status")
    public ResponseEntity<String> sendTicketStatusNotification(
            @RequestBody TicketStatusEvent event) {

        notificationService.sendTicketStatusNotification(event);

        return ResponseEntity.ok("Ticket status notification processed");
    }

    @PostMapping("/account-approved")
    public ResponseEntity<String> sendAccountApprovedNotification(@RequestBody AccountApprovalEvent event) {
        notificationService.sendAccountApprovedNotification(event);
        return ResponseEntity.ok("Approval email processed");
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            Authentication authentication) {

        return ResponseEntity.ok(
                notificationService.getNotifications(authentication.getName())
        );
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            Authentication authentication) {

        return ResponseEntity.ok(
                Map.of("unreadCount",
                        notificationService.getUnreadCount(authentication.getName()))
        );
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {

        notificationService.markAsRead(id, authentication.getName());

        return ResponseEntity.noContent().build();
    }
}
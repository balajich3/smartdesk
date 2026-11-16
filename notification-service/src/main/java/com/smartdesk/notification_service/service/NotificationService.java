package com.smartdesk.notification_service.service;

import com.smartdesk.notification_service.dto.NotificationResponse;
import com.smartdesk.notification_service.entity.Notification;
import com.smartdesk.notification_service.entity.NotificationStatus;
import com.smartdesk.notification_service.entity.NotificationType;
import com.smartdesk.notification_service.event.EmailOtpEvent;
import com.smartdesk.notification_service.repository.NotificationRepository;
import com.smartdesk.notification_service.event.TicketCreatedEvent;

import org.springframework.stereotype.Service;
import com.smartdesk.notification_service.event.TicketStatusEvent;
import com.smartdesk.notification_service.event.AccountApprovalEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private static final int MAX_RETRIES = 3;

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final WebSocketNotificationService webSocketNotificationService;

    public NotificationService(
            NotificationRepository notificationRepository,
            EmailService emailService,
            WebSocketNotificationService webSocketNotificationService) {

        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
        this.webSocketNotificationService = webSocketNotificationService;
    }

    public void sendNotification(
            UUID eventId,
            String recipientEmail,
            String recipientName,
            NotificationType type,
            String subject,
            String message) {

        // Prevent duplicate notifications.
        if (notificationRepository.existsByEventIdAndRecipientEmail(
                eventId, recipientEmail)) {
            return;
        }

        Notification notification = new Notification();

        notification.setEventId(eventId);
        notification.setRecipientEmail(recipientEmail);
        notification.setRecipientName(recipientName);
        notification.setType(type);
        notification.setSubject(subject);
        notification.setMessage(message);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setRetryCount(0);

        notification = notificationRepository.save(notification);

        deliverEmail(notification, false);

        webSocketNotificationService.sendToUser(
                recipientEmail,
                notification
        );
    }

    public void retryNotification(Notification notification) {

        deliverEmail(notification, true);
    }

    private void deliverEmail(
            Notification notification,
            boolean isRetry) {

        try {

            emailService.sendEmail(
                    notification.getRecipientEmail(),
                    notification.getSubject(),
                    notification.getMessage()
            );

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notification.setFailureReason(null);
            notification.setNextRetryAt(null);

        } catch (Exception exception) {

            notification.setStatus(NotificationStatus.FAILED);

            notification.setFailureReason(
                    exception.getMessage() != null
                            ? exception.getMessage()
                            : "Email delivery failed"
            );

            if (isRetry) {
                notification.setRetryCount(
                        notification.getRetryCount() + 1
                );
            }

            if (notification.getRetryCount() < MAX_RETRIES) {

                long delayMinutes = switch (notification.getRetryCount()) {
                    case 0 -> 1;
                    case 1 -> 2;
                    default -> 5;
                };

                notification.setNextRetryAt(
                        LocalDateTime.now().plusMinutes(delayMinutes)
                );

            } else {
                notification.setNextRetryAt(null);
            }
        }

        notificationRepository.save(notification);
    }

    public void sendOtpNotification(EmailOtpEvent event) {

        String subject = "SmartDesk - Login Verification Code";

        String message = """
            Hello %s,

            Your SmartDesk login verification code is:

            %s

            This OTP expires in %d minutes.

            Do not share this code with anyone.

            Regards,
            SmartDesk Security Team
            """.formatted(
                event.getRecipientName(),
                event.getOtp(),
                event.getExpiryMinutes()
        );

        sendNotification(
                event.getEventId(),
                event.getRecipientEmail(),
                event.getRecipientName(),
                NotificationType.LOGIN_OTP,
                subject,
                message
        );
    }
    public void sendTicketCreatedNotification(TicketCreatedEvent event) {

        String subject = "SmartDesk - New Support Ticket: "
                + event.getTicketNumber();

        String message = """
            Hello,

            A new support ticket has been created in your department.

            Ticket Number: %s
            Title: %s
            Summary: %s
            Priority: %s
            Category: %s
            Department: %s

            Requested By: %s
            Created At: %s

            Please log in to SmartDesk to review the ticket.

            Regards,
            SmartDesk Support Team
            """.formatted(
                event.getTicketNumber(),
                event.getTitle(),
                event.getSummary(),
                event.getPriority(),
                event.getCategory(),
                event.getDepartment(),
                event.getRequesterName(),
                event.getCreatedAt()
        );

        for (String recipientEmail : event.getRecipientEmails()) {

            sendNotification(
                    event.getEventId(),
                    recipientEmail,
                    null,
                    NotificationType.TICKET_CREATED,
                    subject,
                    message
            );
        }
        // Notify the employee who created the ticket
        String employeeSubject = "SmartDesk - Ticket Created: "
                + event.getTicketNumber();

        String employeeMessage = """
        Hello %s,

        Your support ticket has been successfully created.

        Ticket Number: %s
        Title: %s
        Summary: %s
        Priority: %s
        Department: %s
        Current Status: OPEN

        Our support team will review your request.

        Regards,
        SmartDesk Support Team
        """.formatted(
                event.getRequesterName(),
                event.getTicketNumber(),
                event.getTitle(),
                event.getSummary(),
                event.getPriority(),
                event.getDepartment()
        );

        sendNotification(
                event.getEventId(),
                event.getRequesterEmail(),
                event.getRequesterName(),
                NotificationType.TICKET_CREATED,
                employeeSubject,
                employeeMessage
        );
    }
    public void sendTicketStatusNotification(TicketStatusEvent event) {

        String status = event.getStatus();

        String subject = "SmartDesk - Ticket "
                + event.getTicketNumber() + " updated";

        String actionMessage = switch (status) {
            case "ASSIGNED" ->
                    "A support agent has accepted your ticket.";

            case "IN_PROGRESS" ->
                    "A support agent has started working on your ticket.";

            case "WAITING_FOR_REQUESTER" ->
                    "The support team needs your response. Please open SmartDesk and approve or reject the request.";

            case "REQUESTER_APPROVED" ->
                    "You approved the support team's request. The agent can now mark the ticket as resolved.";

            case "RESOLVED" ->
                    "The support agent has marked your ticket as resolved.";

            case "CLOSED" ->
                    "Your ticket has been closed.";

            default ->
                    "Your support ticket status has been updated.";
        };

        String message = """
            Hello %s,

            Your support ticket has been updated.

            Ticket Number: %s
            Title: %s
            Current Status: %s

            %s

            Updated At: %s

            Please log in to SmartDesk to view the ticket.

            Regards,
            SmartDesk Support Team
            """.formatted(
                event.getRecipientName(),
                event.getTicketNumber(),
                event.getTitle(),
                status,
                actionMessage,
                event.getUpdatedAt()
        );

        NotificationType type = "RESOLVED".equals(status)
                ? NotificationType.TICKET_RESOLVED
                : NotificationType.STATUS_CHANGED;

        sendNotification(
                event.getEventId(),
                event.getRecipientEmail(),
                event.getRecipientName(),
                type,
                subject,
                message
        );
    }

    public void sendAccountApprovedNotification(AccountApprovalEvent event) {
        String subject = "SmartDesk - Your account has been approved";
        String message = """
                Hello %s,

                Your SmartDesk %s account has been approved by the administrator.

                Department: %s

                You can now sign in to SmartDesk and access your workspace.

                Regards,
                SmartDesk Administration Team
                """.formatted(event.getRecipientName(),
                event.getRole().replace('_', ' ').toLowerCase(),
                event.getDepartment() == null ? "Not assigned" : event.getDepartment());
        sendNotification(event.getEventId(), event.getRecipientEmail(),
                event.getRecipientName(), NotificationType.ACCOUNT_APPROVED, subject, message);
    }

    public List<NotificationResponse> getNotifications(String email) {
        return notificationRepository
                .findTop50ByRecipientEmailOrderByCreatedAtDesc(email)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public long getUnreadCount(String email) {
        return notificationRepository
                .countByRecipientEmailAndReadFalse(email);
    }

    public void markAsRead(Long notificationId, String email) {

        Notification notification = notificationRepository
                .findByIdAndRecipientEmail(notificationId, email)
                .orElseThrow(() ->
                        new IllegalArgumentException("Notification not found"));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }
}
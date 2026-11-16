package com.smartdesk.notification_service.repository;

import com.smartdesk.notification_service.entity.Notification;
import com.smartdesk.notification_service.entity.NotificationStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    boolean existsByEventIdAndRecipientEmail(
            UUID eventId,
            String recipientEmail
    );

    List<Notification>
    findTop50ByStatusAndRetryCountLessThanAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
            NotificationStatus status,
            int retryCount,
            LocalDateTime currentTime
    );
    List<Notification> findTop50ByRecipientEmailOrderByCreatedAtDesc(
            String recipientEmail
    );

    long countByRecipientEmailAndReadFalse(String recipientEmail);

    Optional<Notification> findByIdAndRecipientEmail(
            Long id,
            String recipientEmail
    );
}
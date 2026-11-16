package com.smartdesk.ticket_service.repository;

import com.smartdesk.ticket_service.entity.EmailOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailOtpRepository
        extends JpaRepository<EmailOtp, Long> {

    Optional<EmailOtp>
    findTopByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(
            Long userId
    );
}
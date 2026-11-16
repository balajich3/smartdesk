package com.smartdesk.ticket_service.service;

import com.smartdesk.ticket_service.client.NotificationClient;
import com.smartdesk.ticket_service.entity.EmailOtp;
import com.smartdesk.ticket_service.entity.User;
import com.smartdesk.ticket_service.event.EmailOtpEvent;
import com.smartdesk.ticket_service.repository.EmailOtpRepository;
import com.smartdesk.ticket_service.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailOtpService {

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private final EmailOtpRepository emailOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final NotificationClient notificationClient;

    private final SecureRandom secureRandom = new SecureRandom();

    public void generateAndSendOtp(User user) {

        LocalDateTime now = LocalDateTime.now();

        // Find the latest unused OTP.
        emailOtpRepository
                .findTopByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(
                        user.getId()
                )
                .ifPresent(previousOtp -> {

                    // Prevent frequent OTP generation.
                    if (previousOtp.getLastSentAt()
                            .plusSeconds(RESEND_COOLDOWN_SECONDS)
                            .isAfter(now)) {

                        throw new IllegalStateException(
                                "Please wait before requesting another OTP"
                        );
                    }

                    // Invalidate previous OTP.
                    previousOtp.setConsumedAt(now);
                    emailOtpRepository.save(previousOtp);
                });

        // Generate six-digit OTP.
        String otp = String.format(
                "%06d",
                secureRandom.nextInt(1000000)
        );

        // Store only the hashed OTP.
        EmailOtp emailOtp = new EmailOtp();

        emailOtp.setUser(user);
        emailOtp.setOtpHash(passwordEncoder.encode(otp));
        emailOtp.setExpiresAt(now.plusMinutes(OTP_EXPIRY_MINUTES));
        emailOtp.setAttempts(0);
        emailOtp.setLastSentAt(now);

        emailOtpRepository.save(emailOtp);

        // Send OTP directly to Notification Service.
        EmailOtpEvent event = EmailOtpEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("LOGIN_OTP")
                .recipientEmail(user.getEmail())
                .recipientName(user.getFullName())
                .otp(otp)
                .expiryMinutes(OTP_EXPIRY_MINUTES)
                .build();

        notificationClient.sendOtp(event);
    }

    public void verifyOtp(String email, String submittedOtp) {

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid OTP")
                );

        EmailOtp otp = emailOtpRepository
                .findTopByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(
                        user.getId()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid OTP")
                );

        if (otp.getConsumedAt() != null) {
            throw new IllegalArgumentException("OTP already used");
        }

        if (!otp.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP has expired");
        }

        if (otp.getAttempts() >= 5) {
            throw new IllegalArgumentException(
                    "Maximum OTP attempts exceeded"
            );
        }

        if (!passwordEncoder.matches(submittedOtp, otp.getOtpHash())) {

            otp.setAttempts(otp.getAttempts() + 1);
            emailOtpRepository.saveAndFlush(otp);

            throw new IllegalArgumentException("Invalid OTP");
        }

        otp.setConsumedAt(LocalDateTime.now());
        emailOtpRepository.save(otp);
    }
}
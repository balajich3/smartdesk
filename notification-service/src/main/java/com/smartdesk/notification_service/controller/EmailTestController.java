package com.smartdesk.notification_service.controller;

import com.smartdesk.notification_service.service.EmailService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test/email")
public class EmailTestController {

    private final EmailService emailService;

    public EmailTestController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping
    public String sendTestEmail(@RequestParam String to) {

        emailService.sendEmail(
                to,
                "SmartDesk SMTP Test",
                "Congratulations! Your SmartDesk Notification Service email integration is working."
        );

        return "Test email sent successfully";
    }
}
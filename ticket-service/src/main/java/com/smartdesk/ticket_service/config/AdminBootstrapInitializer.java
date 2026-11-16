package com.smartdesk.ticket_service.config;

import com.smartdesk.ticket_service.service.AdminBootstrapService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import org.springframework.core.annotation.Order;

@Component
@RequiredArgsConstructor
public class AdminBootstrapInitializer implements ApplicationRunner {

    private final AdminBootstrapService adminBootstrapService;

    @Value("${app.bootstrap.admin-name:}")
    private String adminName;

    @Value("${app.bootstrap.admin-email:}")
    private String adminEmail;

    @Value("${app.bootstrap.admin-password:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {

        boolean allMissing = adminName.isBlank()
                && adminEmail.isBlank()
                && adminPassword.isBlank();

        if (allMissing) {
            System.out.println(
                    "Initial admin bootstrap skipped: environment variables not configured."
            );
            return;
        }

        if (adminName.isBlank() || adminEmail.isBlank()
                || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "Configure ADMIN_NAME, ADMIN_EMAIL and ADMIN_PASSWORD together."
            );
        }

        if (adminPassword.length() < 12 || adminPassword.length() > 72) {
            throw new IllegalStateException(
                    "Initial admin password must contain 12 to 72 characters."
            );
        }

        adminBootstrapService.createInitialAdmin(
                adminName,
                adminEmail,
                adminPassword
        );
    }
}
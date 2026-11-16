package com.smartdesk.ticket_service.service;

import com.smartdesk.ticket_service.entity.Department;
import com.smartdesk.ticket_service.entity.Role;
import com.smartdesk.ticket_service.entity.User;
import com.smartdesk.ticket_service.repository.DepartmentRepository;
import com.smartdesk.ticket_service.repository.RoleRepository;
import com.smartdesk.ticket_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminBootstrapService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void createInitialAdmin(
            String fullName,
            String email,
            String password
    ) {

        if (userRepository.existsByRoles_Name("ADMIN")) {
            return;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalStateException(
                    "Admin email already belongs to another account"
            );
        }

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() ->
                        new IllegalStateException("ADMIN role not initialized")
                );

        Department department = departmentRepository.findByCode("IT")
                .orElseThrow(() ->
                        new IllegalStateException("IT department not initialized")
                );

        User admin = User.builder()
                .fullName(fullName.trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(password))
                .department(department)
                .active(true)
                .roles(Set.of(adminRole))
                .build();

        userRepository.save(admin);
    }
}
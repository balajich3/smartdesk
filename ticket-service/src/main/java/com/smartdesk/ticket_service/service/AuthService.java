package com.smartdesk.ticket_service.service;

import com.smartdesk.ticket_service.dto.request.EmailOtpRequest;
import com.smartdesk.ticket_service.dto.request.LoginRequest;
import com.smartdesk.ticket_service.dto.request.RegisterRequest;
import com.smartdesk.ticket_service.dto.request.VerifyOtpRequest;
import com.smartdesk.ticket_service.dto.response.AuthResponse;
import com.smartdesk.ticket_service.entity.ApprovalStatus;
import com.smartdesk.ticket_service.entity.Department;
import com.smartdesk.ticket_service.entity.Role;
import com.smartdesk.ticket_service.entity.User;
import com.smartdesk.ticket_service.repository.DepartmentRepository;
import com.smartdesk.ticket_service.repository.RoleRepository;
import com.smartdesk.ticket_service.repository.UserRepository;
import com.smartdesk.ticket_service.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailOtpService emailOtpService;
    private final JwtService jwtService;

    @Transactional
    public void register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        String requestedRole = request.getRole().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("EMPLOYEE", "SUPPORT_AGENT", "MANAGER").contains(requestedRole)) {
            throw new IllegalArgumentException("Invalid registration role. Choose Employee, Support Agent or Manager.");
        }

        Department department = null;
        boolean departmentRequired = requestedRole.equals("SUPPORT_AGENT") || requestedRole.equals("MANAGER");
        if (departmentRequired) {
            if (request.getDepartmentCode() == null || request.getDepartmentCode().isBlank()) {
                throw new IllegalArgumentException("Department is required for Support Agent and Manager accounts");
            }
            String departmentCode = request.getDepartmentCode().trim().toUpperCase(Locale.ROOT);
            department = departmentRepository.findByCode(departmentCode)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid department"));
            if (!department.isActive()) throw new IllegalArgumentException("Department is inactive");
        }

        Role role = roleRepository.findByName(requestedRole)
                .orElseThrow(() -> new IllegalStateException("Role not initialized: " + requestedRole));
        ApprovalStatus approval = departmentRequired ? ApprovalStatus.PENDING : ApprovalStatus.NOT_REQUIRED;

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .department(department)
                .active(true)
                .approvalStatus(approval)
                .roles(Set.of(role))
                .build();
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse loginWithPassword(LoginRequest request) {
        User user = getActiveUser(request.getEmail());
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        validateApproval(user);
        return createAuthResponse(user);
    }

    @Transactional
    public String requestLoginOtp(EmailOtpRequest request) {
        User user = getActiveUser(request.getEmail());
        validateApproval(user);
        emailOtpService.generateAndSendOtp(user);
        return "OTP sent to your registered email address";
    }

    // Backward compatibility for clients using the old email+password+OTP endpoint.
    @Transactional
    public String login(LoginRequest request) {
        User user = getActiveUser(request.getEmail());
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        validateApproval(user);
        emailOtpService.generateAndSendOtp(user);
        return "OTP sent to your registered email address";
    }

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        User user = getActiveUser(request.getEmail());
        validateApproval(user);
        emailOtpService.verifyOtp(user.getEmail(), request.getOtp());
        return createAuthResponse(user);
    }

    private User getActiveUser(String rawEmail) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or account not found"));
        if (!user.isActive()) throw new IllegalArgumentException("Account is disabled");
        return user;
    }

    private AuthResponse createAuthResponse(User user) {
        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        String token = jwtService.generateToken(user.getId(), user.getEmail(), roles);
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .department(user.getDepartment() == null ? null : user.getDepartment().getName())
                .roles(roles)
                .build();
    }

    private void validateApproval(User user) {
        boolean approvalRequired = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("SUPPORT_AGENT") || role.getName().equals("MANAGER"));
        if (!approvalRequired) return;
        if (user.getApprovalStatus() == ApprovalStatus.PENDING) {
            throw new IllegalArgumentException("Your account is waiting for administrator approval");
        }
        if (user.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new IllegalArgumentException("Your account has not been approved");
        }
    }
}

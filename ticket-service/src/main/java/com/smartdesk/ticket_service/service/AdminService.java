package com.smartdesk.ticket_service.service;

import com.smartdesk.ticket_service.dto.request.UpdateUserRolesRequest;
import com.smartdesk.ticket_service.dto.request.UpdateUserStatusRequest;
import com.smartdesk.ticket_service.dto.request.UpdateUserDepartmentRequest;
import com.smartdesk.ticket_service.dto.response.AdminUserResponse;
import com.smartdesk.ticket_service.dto.response.DepartmentResponse;
import com.smartdesk.ticket_service.client.NotificationClient;
import com.smartdesk.ticket_service.event.AccountApprovalEvent;
import com.smartdesk.ticket_service.repository.DepartmentRepository;
import com.smartdesk.ticket_service.entity.ApprovalStatus;
import com.smartdesk.ticket_service.entity.Role;
import com.smartdesk.ticket_service.entity.User;
import com.smartdesk.ticket_service.entity.Department;
import com.smartdesk.ticket_service.repository.RoleRepository;
import com.smartdesk.ticket_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final NotificationClient notificationClient;

    private static final Set<String> MANAGEABLE_ROLES = Set.of("EMPLOYEE", "SUPPORT_AGENT", "MANAGER");

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getPendingApprovals() {
        return userRepository.findAllByApprovalStatusOrderByCreatedAtAsc(ApprovalStatus.PENDING)
                .stream()
                .filter(user -> hasRole(user, "SUPPORT_AGENT") || hasRole(user, "MANAGER"))
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public AdminUserResponse approveRegistration(Long userId) {
        return updateRegistrationApproval(userId, ApprovalStatus.APPROVED);
    }

    @Transactional
    public AdminUserResponse rejectRegistration(Long userId) {
        return updateRegistrationApproval(userId, ApprovalStatus.REJECTED);
    }

    private AdminUserResponse updateRegistrationApproval(Long userId, ApprovalStatus decision) {
        User user = getUser(userId);
        boolean eligible = hasRole(user, "SUPPORT_AGENT") || hasRole(user, "MANAGER");
        if (!eligible) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only manager and support-agent registrations require approval");
        if (user.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This account is not awaiting approval");
        }
        user.setApprovalStatus(decision);
        if (decision == ApprovalStatus.APPROVED) {
            try {
                AccountApprovalEvent event = new AccountApprovalEvent();
                event.setEventId(UUID.randomUUID());
                event.setRecipientEmail(user.getEmail());
                event.setRecipientName(user.getFullName());
                event.setRole(hasRole(user, "MANAGER") ? "MANAGER" : "SUPPORT_AGENT");
                event.setDepartment(user.getDepartment() == null ? null : user.getDepartment().getName());
                notificationClient.sendAccountApprovedNotification(event);
            } catch (Exception exception) {
                // Do not undo administrator approval if the notification service is temporarily unavailable.
                org.slf4j.LoggerFactory.getLogger(AdminService.class)
                        .error("Account approved, but approval email could not be sent to {}", user.getEmail(), exception);
            }
        }
        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartments() {
        return departmentRepository.findAll().stream()
                .filter(department -> department.isActive())
                .map(department -> DepartmentResponse.builder()
                        .id(department.getId())
                        .code(department.getCode())
                        .name(department.getName())
                        .build())
                .toList();
    }

    @Transactional
    public AdminUserResponse updateUserDepartment(Long userId, UpdateUserDepartmentRequest request) {
        User user = getUser(userId);
        boolean managerOrAgent = hasRole(user, "MANAGER") || hasRole(user, "SUPPORT_AGENT");
        if (!managerOrAgent || user.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only approved managers and support agents can be reassigned");
        }
        Department department = departmentRepository.findByCode(request.getDepartmentCode().trim().toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
        if (!department.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department is inactive");
        }
        user.setDepartment(department);
        return mapToResponse(user);
    }

    @Transactional
    public AdminUserResponse updateUserRoles(Long userId, UpdateUserRolesRequest request) {
        User user = getUser(userId);
        if (hasRole(user, "ADMIN")) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator roles cannot be modified through this API");
        Set<String> requestedRoles = request.getRoles().stream().map(role -> role.trim().toUpperCase()).collect(Collectors.toSet());
        if (!MANAGEABLE_ROLES.containsAll(requestedRoles)) throw new IllegalArgumentException("Invalid role supplied");
        requestedRoles.add("EMPLOYEE");
        Set<Role> roles = requestedRoles.stream().map(name -> roleRepository.findByName(name)
                .orElseThrow(() -> new IllegalStateException("Role not initialized: " + name))).collect(Collectors.toSet());
        user.setRoles(roles);
        return mapToResponse(user);
    }

    @Transactional
    public AdminUserResponse updateUserStatus(Long userId, UpdateUserStatusRequest request, String adminEmail) {
        User user = getUser(userId);
        if (hasRole(user, "ADMIN")) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator accounts cannot be modified through this API");
        if (user.getEmail().equalsIgnoreCase(adminEmail) && !request.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot deactivate your own account");
        }
        user.setActive(request.isActive());
        return mapToResponse(user);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName().equals(roleName));
    }

    private AdminUserResponse mapToResponse(User user) {
        Set<String> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        return AdminUserResponse.builder()
                .id(user.getId()).fullName(user.getFullName()).email(user.getEmail())
                .department(user.getDepartment() == null ? null : user.getDepartment().getName())
                .roles(roles).active(user.isActive()).approvalStatus(user.getApprovalStatus())
                .createdAt(user.getCreatedAt()).build();
    }
}

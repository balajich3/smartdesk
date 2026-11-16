package com.smartdesk.ticket_service.controller;

import com.smartdesk.ticket_service.dto.request.UpdateUserRolesRequest;
import com.smartdesk.ticket_service.dto.request.UpdateUserStatusRequest;
import com.smartdesk.ticket_service.dto.request.UpdateUserDepartmentRequest;
import com.smartdesk.ticket_service.dto.response.DepartmentResponse;
import com.smartdesk.ticket_service.dto.response.AdminUserResponse;
import com.smartdesk.ticket_service.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/departments")
    public ResponseEntity<List<DepartmentResponse>> getDepartments() {
        return ResponseEntity.ok(adminService.getDepartments());
    }

    @PatchMapping("/users/{userId}/department")
    public ResponseEntity<AdminUserResponse> updateUserDepartment(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserDepartmentRequest request) {
        return ResponseEntity.ok(adminService.updateUserDepartment(userId, request));
    }

    @GetMapping("/approvals/pending")
    public ResponseEntity<List<AdminUserResponse>> getPendingApprovals() {
        return ResponseEntity.ok(adminService.getPendingApprovals());
    }

    @PatchMapping("/approvals/{userId}/approve")
    public ResponseEntity<AdminUserResponse> approveRegistration(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.approveRegistration(userId));
    }

    @PatchMapping("/approvals/{userId}/reject")
    public ResponseEntity<AdminUserResponse> rejectRegistration(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.rejectRegistration(userId));
    }

    @PatchMapping("/users/{userId}/roles")
    public ResponseEntity<AdminUserResponse> updateUserRoles(@PathVariable Long userId,
                                                             @Valid @RequestBody UpdateUserRolesRequest request) {
        return ResponseEntity.ok(adminService.updateUserRoles(userId, request));
    }

    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<AdminUserResponse> updateUserStatus(@PathVariable Long userId,
                                                              @RequestBody UpdateUserStatusRequest request, Authentication authentication) {
        return ResponseEntity.ok(adminService.updateUserStatus(userId, request, authentication.getName()));
    }
}

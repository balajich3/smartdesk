package com.smartdesk.ticket_service.dto.response;

import com.smartdesk.ticket_service.entity.ApprovalStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
public class AdminUserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String department;
    private Set<String> roles;
    private boolean active;
    private ApprovalStatus approvalStatus;
    private LocalDateTime createdAt;
}

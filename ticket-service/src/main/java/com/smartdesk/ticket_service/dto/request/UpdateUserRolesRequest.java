package com.smartdesk.ticket_service.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UpdateUserRolesRequest {

    @NotEmpty(message = "At least one role is required")
    private Set<String> roles;
}
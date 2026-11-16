package com.smartdesk.ticket_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserDepartmentRequest {
    @NotBlank
    private String departmentCode;
}

package com.smartdesk.ticket_service.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepartmentResponse {
    private Long id;
    private String code;
    private String name;
}

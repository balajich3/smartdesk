package com.smartdesk.ticket_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AuthResponse {

    private String accessToken;
    private String tokenType;
    private Long userId;
    private String fullName;
    private String email;
    private String department;
    private List<String> roles;
}
package com.smartdesk.ticket_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OtpVerificationResponse {

    private String message;
}
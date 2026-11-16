package com.smartdesk.ai_triage_service.dto.request;

public record GroqMessage(
        String role,
        String content
) {}
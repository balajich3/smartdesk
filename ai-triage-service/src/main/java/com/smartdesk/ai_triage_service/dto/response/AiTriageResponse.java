package com.smartdesk.ai_triage_service.dto.response;

public record AiTriageResponse(
        String summary,
        String category,
        String priority
) {
}
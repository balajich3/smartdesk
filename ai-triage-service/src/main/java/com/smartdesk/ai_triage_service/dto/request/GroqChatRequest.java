package com.smartdesk.ai_triage_service.dto.request;

import java.util.List;

public record GroqChatRequest(
        String model,
        List<GroqMessage> messages,
        GroqResponseFormat response_format,
        Integer max_completion_tokens
) {
}

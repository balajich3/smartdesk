package com.smartdesk.ai_triage_service.client;

import com.smartdesk.ai_triage_service.dto.request.GroqChatRequest;
import com.smartdesk.ai_triage_service.dto.response.GroqChatResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "groq-client",
        url = "${groq.api.url}"
)
public interface GroqClient {

    @PostMapping("/chat/completions")
    GroqChatResponse generateResponse(
            @RequestHeader("Authorization") String authorization,
            @RequestBody GroqChatRequest request
    );
}
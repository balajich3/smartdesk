package com.smartdesk.ticket_service.client;

import com.smartdesk.ticket_service.dto.request.AiTriageRequest;
import com.smartdesk.ticket_service.dto.response.AiTriageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ai-triage-service")
public interface AiTriageClient {

    @PostMapping("/api/triage")
    AiTriageResponse analyzeTicket(
            @RequestBody AiTriageRequest request
    );
}
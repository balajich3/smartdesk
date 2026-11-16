package com.smartdesk.ticket_service.service;

import com.smartdesk.ticket_service.client.AiTriageClient;
import com.smartdesk.ticket_service.dto.request.AiTriageRequest;
import com.smartdesk.ticket_service.dto.response.AiTriageResponse;
import com.smartdesk.ticket_service.exception.AiTriageException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiTriageService {

    private final AiTriageClient aiTriageClient;

    public AiTriageResponse analyze(String title, String description) {

        AiTriageRequest request = new AiTriageRequest();
        request.setTitle(title);
        request.setDescription(description);

        try {
            AiTriageResponse response = aiTriageClient.analyzeTicket(request);

            if (response == null
                    || response.getSummary() == null
                    || response.getSummary().isBlank()
                    || response.getCategory() == null
                    || response.getCategory().isBlank()
                    || response.getPriority() == null
                    || response.getPriority().isBlank()) {

                throw new AiTriageException("AI returned an incomplete response");
            }

            return response;

        } catch (FeignException exception) {
            throw new AiTriageException(
                    "AI Triage Service is currently unavailable",
                    exception
            );
        }
    }
}
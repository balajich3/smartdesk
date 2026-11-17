package com.smartdesk.ai_triage_service.controller;

import com.smartdesk.ai_triage_service.dto.request.AiTriageRequest;
import com.smartdesk.ai_triage_service.dto.response.AiTriageResponse;
import com.smartdesk.ai_triage_service.service.AiTriageService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/triage")
public class AiTriageController {

    private final AiTriageService aiTriageService;

    public AiTriageController(AiTriageService aiTriageService) {
        this.aiTriageService = aiTriageService;
    }

    @PostMapping
    public ResponseEntity<AiTriageResponse> analyzeTicket(@Valid @RequestBody AiTriageRequest request) {
        AiTriageResponse response = aiTriageService.analyzeTicket(request);

        return ResponseEntity.ok(response);
    }
}
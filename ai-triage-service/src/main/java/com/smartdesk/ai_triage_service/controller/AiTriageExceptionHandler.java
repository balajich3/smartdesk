package com.smartdesk.ai_triage_service.controller;

import com.smartdesk.ai_triage_service.service.AiProviderUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class AiTriageExceptionHandler {

    @ExceptionHandler(AiProviderUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleAiProviderUnavailable(
            AiProviderUnavailableException exception
    ) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "AI_SERVICE_UNAVAILABLE",
                        "message", exception.getMessage()
                ));
    }
}

package com.smartdesk.ai_triage_service.service;

public class AiProviderUnavailableException extends RuntimeException {

    public AiProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

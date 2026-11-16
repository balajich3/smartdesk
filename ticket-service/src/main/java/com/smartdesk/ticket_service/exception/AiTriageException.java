package com.smartdesk.ticket_service.exception;

public class AiTriageException extends RuntimeException {

    public AiTriageException(String message) {
        super(message);
    }

    public AiTriageException(String message, Throwable cause) {
        super(message, cause);
    }
}
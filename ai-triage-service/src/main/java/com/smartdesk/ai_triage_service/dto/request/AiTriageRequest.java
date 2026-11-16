package com.smartdesk.ai_triage_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiTriageRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200)
        String title,

        @NotBlank(message = "Description is required")
        @Size(max = 5000)
        String description

) {
}
package com.smartdesk.ticket_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCommentRequest {

    @NotBlank(message = "Comment cannot be empty")
    @Size(max = 5000)
    private String message;

    private boolean internalNote;
}
package com.smartdesk.ticket_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentResponse {

    private Long id;
    private String message;
    private boolean internalNote;
    private String authorName;
    private LocalDateTime createdAt;
}
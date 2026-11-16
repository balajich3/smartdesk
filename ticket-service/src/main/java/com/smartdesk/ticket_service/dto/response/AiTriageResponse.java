package com.smartdesk.ticket_service.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiTriageResponse {

    private String summary;
    private String category;
    private String priority;
}
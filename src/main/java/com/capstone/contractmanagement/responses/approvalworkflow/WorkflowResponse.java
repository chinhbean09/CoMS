package com.capstone.contractmanagement.responses.approvalworkflow;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WorkflowResponse {
    private Long id;
    private String comment;
    private Long createdBy;
    private LocalDateTime createdAt;
}

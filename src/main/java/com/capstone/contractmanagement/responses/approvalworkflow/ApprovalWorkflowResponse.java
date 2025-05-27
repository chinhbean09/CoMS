package com.capstone.contractmanagement.responses.approvalworkflow;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ApprovalWorkflowResponse {
    private Long id;
    private String name;
    private int customStagesCount;
    private List<ApprovalStageResponse> stages;
    private LocalDateTime createdAt;
    private int reSubmitVersion;
}

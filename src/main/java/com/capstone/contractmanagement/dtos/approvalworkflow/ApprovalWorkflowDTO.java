package com.capstone.contractmanagement.dtos.approvalworkflow;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ApprovalWorkflowDTO {
    private Long contractId;
    private Long contractTypeId;
    private String name;
    private List<ApprovalStageDTO> stages;
}

package com.capstone.contractmanagement.dtos.approvalworkflow;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AddendumApprovalWorkflowDTO {
    private Long addendumTypeId;
    private String name;
    private List<ApprovalStageDTO> stages;
}

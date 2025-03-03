package com.capstone.contractmanagement.dtos.approvalworkflow;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApprovalStageDTO {
    private int stageOrder;
    private Long approverId;
}

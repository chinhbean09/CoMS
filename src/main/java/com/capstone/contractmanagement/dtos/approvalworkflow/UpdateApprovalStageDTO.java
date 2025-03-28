package com.capstone.contractmanagement.dtos.approvalworkflow;

import com.capstone.contractmanagement.enums.ApprovalStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateApprovalStageDTO {
    private ApprovalStatus status;
}

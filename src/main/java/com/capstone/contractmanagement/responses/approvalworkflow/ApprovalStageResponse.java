package com.capstone.contractmanagement.responses.approvalworkflow;

import com.capstone.contractmanagement.enums.ApprovalStatus;
import com.capstone.contractmanagement.enums.DepartmentList;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApprovalStageResponse {
    private Long stageId;
    private Integer stageOrder;
    private Long approver;
    private String approverName;
    private DepartmentList department;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private ApprovalStatus status;
    private LocalDateTime approvedAt;
    private String comment;
}

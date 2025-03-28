package com.capstone.contractmanagement.dtos.approvalworkflow;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDTO {
    private String comment;
}

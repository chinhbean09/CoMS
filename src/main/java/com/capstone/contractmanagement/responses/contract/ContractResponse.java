package com.capstone.contractmanagement.responses.contract;

import com.capstone.contractmanagement.enums.ContractStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractResponse {
    private Long id;
    private String title;
    private String contractNumber;
    private String description;
    private ContractStatus status;
    private LocalDateTime startDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}

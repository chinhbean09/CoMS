package com.capstone.contractmanagement.responses.template;

import com.capstone.contractmanagement.entities.contract.ContractType;
import com.capstone.contractmanagement.enums.ContractTemplateStatus;
import com.capstone.contractmanagement.responses.User.UserContractResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ContractTemplateSimpleResponse {
    private Long id;
    private String contractTitle;
    private String specialTermsA;
    private String specialTermsB;
    private Boolean appendixEnabled;
    private Boolean transferEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean violate;
    private Boolean suspend;
    private String suspendContent;
    private String contractContent;
    private Boolean autoAddVAT;
    private Integer vatPercentage;
    private Boolean isDateLateChecked;
    private Integer maxDateLate;
    private Boolean autoRenew;
    private ContractType contractType;
    private UserContractResponse user;
    private ContractTemplateStatus status;
}

package com.capstone.contractmanagement.responses.template;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ContractTemplateSimpleResponse {
    private Long id;
    private String contractTitle;
    private String partyInfo;
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
    private Long contractTypeId;
}

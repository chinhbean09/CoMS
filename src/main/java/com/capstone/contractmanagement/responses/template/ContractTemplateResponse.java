package com.capstone.contractmanagement.responses.template;

import com.capstone.contractmanagement.dtos.term.TermSimpleDTO;
import com.capstone.contractmanagement.responses.term.TermResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ContractTemplateResponse {

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
    private List<TermResponse> legalBasisTerms;
    private List<TermResponse> generalTerms;
    private List<TermResponse> otherTerms;
    private List<TermResponse> additionalTerms;
    private Long contractTypeId;
    // Thêm field cho additionalTermConfigs

    // Instead of a list of additionalTermConfigs, we now use a map:
    // Key: typeTermId (as String)
    // Value: Map with keys "Common", "A", "B" and values are lists of TermResponseDTO
    private Map<String, Map<String, List<TermResponse>>> additionalConfig;

}

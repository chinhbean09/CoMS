package com.capstone.contractmanagement.responses.template;

import com.capstone.contractmanagement.responses.term.TermResponse;
import com.capstone.contractmanagement.responses.term.TypeTermResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ContractTemplateResponseIds {
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
    private List<Long> legalBasisTermIds;
    private List<Long> generalTermIds;
    private List<Long> otherTermIds;
    private List<Long> additionalTermIds;
    private List<Long> additionalTerms;
    private Long contractTypeId;
    // Thêm field cho additionalTermConfigs

    // Instead of a list of additionalTermConfigs, we now use a map:
    // Key: typeTermId (as String)
    // Value: Map with keys "Common", "A", "B" and values are lists of TermResponseDTO
    private Map<String, Map<String, List<Long>>> additionalConfig;
}

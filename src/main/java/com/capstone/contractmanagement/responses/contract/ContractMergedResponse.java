package com.capstone.contractmanagement.responses.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractMergedResponse {
    // Các trường của hợp đồng
    private Long id;
    private String title;
    private String contractNumber;
    private Long partyId;
    private Long userId;

    // Các trường của mẫu hợp đồng
    private String contractTitle;
    private String partyInfo;
    private List<TermSimpleResponse> legalBasisTerms;
    private List<TermSimpleResponse> generalTerms;
    private List<TermSimpleResponse> otherTerms;
    private List<AdditionalTermResponse> additionalTerms;
    private Map<String, Map<String, List<AdditionalConfigTermResponse>>> additionalConfig;
    private String specialTermsA;
    private String specialTermsB;
    private String contractContent;
    private Boolean autoAddVAT;
    private Integer vatPercentage;
    private Boolean isDateLateChecked;
    private Integer maxDateLate;
    private Boolean autoRenew;
    private Long contractTypeId;
}

package com.capstone.contractmanagement.dtos.contract;

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
public class TemplateSnapshotDTO {
    private Long id;
    private String contractTitle;
    private String partyInfo;
    private String specialTermsA;
    private String specialTermsB;
    private Boolean appendixEnabled;
    private Boolean transferEnabled;
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

    // Các danh sách điều khoản
    private List<TermSnapshotDTO> legalBasisTerms;
    private List<TermSnapshotDTO> generalTerms;
    private List<TermSnapshotDTO> otherTerms;

    // Danh sách additional term (base) chọn sẵn
    private List<AdditionalTermDTO> additionalTerms;

    // additionalConfig: cấu trúc map
    private Map<String, Map<String, List<TermSnapshotDTO>>> additionalConfig;

}

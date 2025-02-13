package com.capstone.contractmanagement.dtos.template;

import com.capstone.contractmanagement.dtos.IdDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ContractTemplateDTO {
    @NotBlank(message = "Contract title is required")
    @Size(max = 200, message = "Contract title must be less than 200 characters")
    private String contractTitle;

    @NotBlank(message = "Party info is required")
    private String partyInfo;

    // Mỗi phần tử chứa id của type term (ví dụ: 1, 2)
    private List<IdDTO> legalBasis;

    private Boolean appendixEnabled;
    private Boolean transferEnabled;
    private Boolean violate;
    private Boolean suspend;
    private String suspendContent;

    //  type term của nhóm general
    private List<Long> generalTerms;

    //  type term của nhóm additional
    private List<Long> additionalTerms;

    /**
     * type term thuộc additionalTerms.
     * Đây là 1 map với key là indentifier cho từng type term (additional,
     RightsAndObligation,AnotherTypeTerm, ...),
     * và value là map chứa các nhóm con (Common, A, B) với danh sách các IdDTO.
     */

    //key = String, value = Map < key = String, value = List<IdDTO>>
    private Map<String, Map<String, List<IdDTO>>> additionalConfig;

    private Long specialTermsA;  // term id
    private Long specialTermsB;  // term id

    private String contractContent;
    private Boolean autoAddVAT;
    private Integer vatPercentage;
    private Boolean isDateLateChecked;
    private Integer maxDateLate;
    private Boolean autoRenew;
}

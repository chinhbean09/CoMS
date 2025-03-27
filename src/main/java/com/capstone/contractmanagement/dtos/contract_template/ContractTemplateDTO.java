package com.capstone.contractmanagement.dtos.contract_template;

import com.capstone.contractmanagement.dtos.IdDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ContractTemplateDTO {

    @NotBlank(message = "Contract title is required")
    private String contractTitle;

    // Mỗi phần tử chứa id của type term (ví dụ: 1, 2)
    private List<Long> legalBasisTerms;

    private Boolean appendixEnabled;

    private Boolean transferEnabled;

    private Boolean violate;

    private Boolean suspend;

    private String suspendContent;

    //  type term của nhóm general
    private List<Long> generalTerms;

    //  type term của nhóm additional
    private List<Long> additionalTerms;

    private List<Long> otherTerms;

    /**
     * type term thuộc additionalTerms.
     * Đây là 1 map với key là indentifier cho từng type term (additional,
     RightsAndObligation,AnotherTypeTerm, ...),
     * và value là map chứa các nhóm con (Common, A, B) với danh sách các IdDTO.
     */

    //key = String, value = Map < key = String, value = List<IdDTO>>
    private Map<String, Map<String, List<IdDTO>>> additionalConfig;

    private String specialTermsA;

    private String specialTermsB;

    private String contractContent;

    private Boolean autoAddVAT;

    private Integer vatPercentage;

    private Boolean isDateLateChecked;

    private Integer maxDateLate;

    private Boolean autoRenew;

    private Long contractTypeId;

    private LocalDateTime updateAt;

}

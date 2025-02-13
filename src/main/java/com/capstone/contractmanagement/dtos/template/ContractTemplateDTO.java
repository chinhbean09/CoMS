package com.capstone.contractmanagement.dtos.template;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ContractTemplateDTO {
    @NotBlank(message = "Contract title is required")
    @JsonProperty("contract-title")
    @Size(max = 200, message = "Contract title must be less than 200 characters")
    private String contractTitle;


    @NotBlank(message = "Party info is required")
    private String partyInfo;

    private String specialTermsA;
    private String specialTermsB;
    private Boolean appendixEnabled;
    private Boolean transferEnabled;
    private String violate;
    private String suspend;
    private String suspendContent;
    private String contractContent;
    private Boolean autoAddVAT;
    private Integer vatPercentage;
    private Boolean isDateLateChecked;
    private Integer maxDateLate;
    private Boolean autoRenew;

    @NotNull(message = "Terms list cannot be null")
    private List<Long> termIds;
}

package com.capstone.contractmanagement.responses.contract;

import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.responses.term.TermResponse;
import com.capstone.contractmanagement.responses.term.TypeTermResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private List<TermResponse> legalBasisTerms;
    private List<TermResponse> generalTerms;
    private List<TermResponse> otherTerms;
    private List<TypeTermResponse> additionalTerms;
    private Map<String, Map<String, List<TermResponse>>> additionalConfig;

}

package com.capstone.contractmanagement.responses.contract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractTermResponse {
    private Long id;
    private Long originalTermId;
    private String termLabel;
    private String termValue;
    private String termType;         // Ví dụ: LEGAL_BASIS, GENERAL_TERMS, OTHER_TERMS, ADDITIONAL

}

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
    private String termContent;
    private String termType;         // Ví dụ: LEGAL_BASIS, GENERAL_TERMS, OTHER_TERMS, ADDITIONAL
    private String additionalGroup;  // Nếu term thuộc loại bổ sung (Common, A, B), ngược lại null

}

package com.capstone.contractmanagement.dtos.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignContractRequest {
    private Long contractId;
    private String fileName;
    private String fileBase64;
    private String signedBy;
    private String signedAt;

}

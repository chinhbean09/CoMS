package com.capstone.contractmanagement.responses.contract_partner;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractPartnerItemResponse {
    private Long id;
    private Double amount;
    private String description;
}

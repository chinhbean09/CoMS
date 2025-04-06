package com.capstone.contractmanagement.responses.contract_partner;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerContractItemResponse {
    private Long id;
    private Double amount;
    private String description;
}

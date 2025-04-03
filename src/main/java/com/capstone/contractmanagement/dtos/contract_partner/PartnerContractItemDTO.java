package com.capstone.contractmanagement.dtos.contract_partner;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerContractItemDTO {
    private String description;
    private Double amount;
}

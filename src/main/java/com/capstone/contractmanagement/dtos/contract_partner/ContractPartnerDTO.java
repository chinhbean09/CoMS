package com.capstone.contractmanagement.dtos.contract_partner;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ContractPartnerDTO {
    private Double amount;
    private String contractNumber;
    private List<Integer> effectiveDate;  // [2025, 3, 20, 0, 0, 0]
    private List<Integer> expiryDate;     // [2025, 4, 26, 0, 0, 0]
    private String partnerName;
    private List<ContractPartnerPaymentDTO> paymentSchedules;
    private List<Integer> signingDate;    // [2025, 3, 19, 0, 0, 0]
    private String title;
}

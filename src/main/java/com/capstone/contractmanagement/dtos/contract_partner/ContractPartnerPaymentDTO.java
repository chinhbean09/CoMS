package com.capstone.contractmanagement.dtos.contract_partner;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractPartnerPaymentDTO {
    private Double amountItem;
    private List<Integer> paymentDate;    // [2025, 3, 27, 0, 0, 0]
    private String paymentMethod;
    private Integer paymentOrder;
    private Integer paymentPercentage;
}

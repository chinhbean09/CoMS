package com.capstone.contractmanagement.responses.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractLiquidationResponse {
    private Long contractId;
    private String liquidateContent;
    private LocalDateTime liquidateAt;
    private List<String> urls;
}

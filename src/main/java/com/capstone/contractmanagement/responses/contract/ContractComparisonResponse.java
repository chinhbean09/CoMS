package com.capstone.contractmanagement.responses.contract;

import com.capstone.contractmanagement.dtos.contract.ContractComparisonDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractComparisonResponse {
    private List<ContractComparisonDTO> basicInfoDifferences;
    private List<ContractComparisonDTO> termsDifferences;
    private List<ContractComparisonDTO> additionalTermsDifferences;

}

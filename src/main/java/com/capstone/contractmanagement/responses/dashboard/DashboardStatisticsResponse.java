package com.capstone.contractmanagement.responses.dashboard;

import com.capstone.contractmanagement.enums.ContractStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatisticsResponse {
    private Map<ContractStatus, Long> statusCounts;
    private List<MonthlyContractCount> monthlyCounts;
}
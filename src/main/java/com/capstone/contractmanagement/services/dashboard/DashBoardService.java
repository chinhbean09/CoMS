package com.capstone.contractmanagement.services.dashboard;

import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.repositories.IContractRepository;
import com.capstone.contractmanagement.responses.dashboard.DashboardStatisticsResponse;
import com.capstone.contractmanagement.responses.dashboard.MonthlyContractCount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashBoardService implements IDashBoardService {
    private final IContractRepository contractRepository;

    @Override
    public DashboardStatisticsResponse getDashboardData() {
        // 1. Trạng thái (chỉ lấy phiên bản mới nhất)
        Map<ContractStatus, Long> statusCounts = new HashMap<>();
        for (ContractStatus status : ContractStatus.values()) {
            long count = contractRepository.countByStatusAndIsLatestVersionTrue(status);
            statusCounts.put(status, count);
        }

        // 2. Theo tháng (chỉ lấy phiên bản mới nhất)
        List<Object[]> monthCountsRaw = contractRepository.countLatestContractsByMonth();
        List<MonthlyContractCount> monthlyCounts = monthCountsRaw.stream()
                .map(row -> {
                    String fullMonthName = (String) row[0]; // Ví dụ: "January"
                    String monthAbbr = fullMonthName.substring(0, 3).toUpperCase(); // Ví dụ: "JAN"
                    long count = (Long) row[1];
                    return new MonthlyContractCount(monthAbbr, count);
                })
                .collect(Collectors.toList());

        return new DashboardStatisticsResponse(statusCounts, monthlyCounts);
    }
}

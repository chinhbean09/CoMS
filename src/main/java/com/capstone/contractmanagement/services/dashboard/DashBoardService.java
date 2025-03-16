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
    public DashboardStatisticsResponse getDashboardData(int year) {
        // 1. Thống kê theo trạng thái (chỉ lấy phiên bản mới nhất)
        Map<ContractStatus, Long> statusCounts = new HashMap<>();
        for (ContractStatus status : ContractStatus.values()) {
            long count = contractRepository.countByStatusAndIsLatestVersionTrue(status, year);
            statusCounts.put(status, count);
        }
        // 2. Thống kê theo tháng (chỉ lấy phiên bản mới nhất và theo năm)
        List<Object[]> monthCountsRaw = contractRepository.countLatestContractsByMonth(year);
        List<MonthlyContractCount> monthlyCounts = monthCountsRaw.stream()
                .map(row -> {
                    String monthAbbr = (String) row[0]; // Ví dụ: "Jan"
                    long count = ((Number) row[1]).longValue(); // Chuyển đổi số lượng hợp đồng
                    return new MonthlyContractCount(monthAbbr, count);
                })
                .collect(Collectors.toList());

        return new DashboardStatisticsResponse(statusCounts, monthlyCounts);
    }}

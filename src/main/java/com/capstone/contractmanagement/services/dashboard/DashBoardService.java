package com.capstone.contractmanagement.services.dashboard;

import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.repositories.IContractRepository;
import com.capstone.contractmanagement.responses.dashboard.DashboardStatisticsResponse;
import com.capstone.contractmanagement.responses.dashboard.MonthlyContractCount;
import com.capstone.contractmanagement.responses.dashboard.PieChartData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashBoardService implements IDashBoardService {
    private final IContractRepository contractRepository;

    @Override
    public DashboardStatisticsResponse getDashboardData(int year) {
        Map<ContractStatus, Long> statusCounts = new HashMap<>();
        for (ContractStatus status : ContractStatus.values()) {
            long count = contractRepository.countByStatusAndIsLatestVersionTrue(status, year);
            statusCounts.put(status, count);
        }

        // pie chart
        List<ContractStatus> pieStatuses = Arrays.asList(
                ContractStatus.APPROVAL_PENDING,
                ContractStatus.COMPLETED,
                ContractStatus.EXPIRED,
                ContractStatus.ENDED,
                ContractStatus.CANCELLED,
                ContractStatus.ACTIVE,
                ContractStatus.SIGNED,
                ContractStatus.APPROVED
        );

        long totalPieContracts = pieStatuses.stream()
                .mapToLong(statusCounts::get)
                .sum();

        List<PieChartData> pieChartData = new ArrayList<>();
        if (totalPieContracts > 0) {
            for (ContractStatus status : pieStatuses) {
                long count = statusCounts.get(status);
                double percentage = (count * 100.0) / totalPieContracts; // Tính phần trăm
                pieChartData.add(new PieChartData(status.name(), percentage));
            }
        } else {
            // Nếu không có hợp đồng nào trong các trạng thái này, trả về 0% cho tất cả
            for (ContractStatus status : pieStatuses) {
                pieChartData.add(new PieChartData(status.name(), 0.0));
            }
        }

        List<Object[]> monthCountsRaw = contractRepository.countLatestContractsByMonth(year);
        List<MonthlyContractCount> monthlyCounts = monthCountsRaw.stream()
                .map(row -> {
                    String monthAbbr = (String) row[0]; // Ví dụ: "Jan"
                    long count = ((Number) row[1]).longValue(); // Chuyển đổi số lượng hợp đồng
                    return new MonthlyContractCount(monthAbbr, count);
                })
                .collect(Collectors.toList());

        return new DashboardStatisticsResponse(statusCounts, monthlyCounts, pieChartData);
    }
}

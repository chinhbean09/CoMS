package com.capstone.contractmanagement.services.dashboard;

import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.repositories.IContractRepository;
import com.capstone.contractmanagement.responses.dashboard.DashboardStatisticsResponse;
import com.capstone.contractmanagement.responses.dashboard.MonthlyContractCount;
import com.capstone.contractmanagement.responses.dashboard.PieChartData;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashBoardService implements IDashBoardService {
    private final IContractRepository contractRepository;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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

    @Override
    public Workbook generateTimeReportExcel(LocalDateTime from, LocalDateTime to, String groupBy) {
        List<Object[]> rows;
        switch(groupBy.toUpperCase()) {
            case "YEAR":
                rows = contractRepository.reportByYear(from, to);
                break;
            case "QUARTER":
                rows = contractRepository.reportByQuarter(from, to);
                break;
            case "MONTH":
            default:
                rows = contractRepository.reportByMonth(from, to);
                break;
        }

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Time Report");
        Row h = sheet.createRow(0);
        h.createCell(0).setCellValue("Period");
        h.createCell(1).setCellValue("Contract Count");
        h.createCell(2).setCellValue("Total Value");

        int r = 1;
        for (Object[] o : rows) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(o[0].toString());
            row.createCell(1).setCellValue(((Number)o[1]).longValue());
            row.createCell(2).setCellValue(((Number)o[2]).doubleValue());
        }
        for(int c=0;c<3;c++) sheet.autoSizeColumn(c);
        return wb;
    }

    @Override
    public Workbook generateCustomerReportExcel(LocalDateTime from, LocalDateTime to) {
        List<Contract> contracts = contractRepository.findByIsLatestVersionTrueAndSigningDateBetween(
                from, to,
                Sort.by("partner.partnerName").ascending()
                        .and(Sort.by("signingDate").ascending())
        );

        Workbook wb = new SXSSFWorkbook();
        Sheet sheet = wb.createSheet("Customer Report");

        // Header
        Row h = sheet.createRow(0);
        String[] cols = { "Customer ID", "Customer Name", "Contract #", "Signing Date", "Amount", "Status" };
        for (int i = 0; i < cols.length; i++) {
            h.createCell(i).setCellValue(cols[i]);
        }

        // Data
        int r = 1;
        for (Contract c : contracts) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(c.getPartner().getId());
            row.createCell(1).setCellValue(c.getPartner().getPartnerName());
            row.createCell(2).setCellValue(c.getContractNumber());
            row.createCell(3).setCellValue(c.getSigningDate().format(dtf));
            row.createCell(4).setCellValue(c.getAmount() != null ? c.getAmount() : 0);
            row.createCell(5).setCellValue(c.getStatus().name());
        }

        for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
        return wb;
    }

    @Override
    public Workbook generateStatusReportExcel(LocalDateTime from, LocalDateTime to) {
        List<ContractStatus> statuses = List.of(
                ContractStatus.APPROVAL_PENDING,
                ContractStatus.SIGNED,
                ContractStatus.EXPIRED
        );
        List<Object[]> raw = contractRepository.countByStatusesBetween(statuses, from, to);

        // map init = 0
        Map<ContractStatus, Long> map = new EnumMap<>(ContractStatus.class);
        statuses.forEach(s -> map.put(s, 0L));
        raw.forEach(o -> map.put((ContractStatus)o[0], ((Number)o[1]).longValue()));

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Status Report");
        Row h = sheet.createRow(0);
        h.createCell(0).setCellValue("Status");
        h.createCell(1).setCellValue("Count");

        int r = 1;
        for (ContractStatus s : statuses) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(s.name());
            row.createCell(1).setCellValue(map.get(s));
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        return wb;
    }
}

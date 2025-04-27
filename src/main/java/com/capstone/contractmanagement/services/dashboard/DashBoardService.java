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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashBoardService implements IDashBoardService {
    private static final Logger logger = LoggerFactory.getLogger(DashBoardService.class);
    private final IContractRepository contractRepository;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final List<ContractStatus> VALID_STATUSES = List.of(
            ContractStatus.ACTIVE,
            ContractStatus.EXPIRED,
            ContractStatus.ENDED,
            ContractStatus.CANCELLED,
            ContractStatus.CREATED
    );

    @Override
    public DashboardStatisticsResponse getDashboardData(int year) {
        Map<ContractStatus, Long> statusCounts = new HashMap<>();
        for (ContractStatus status : ContractStatus.values()) {
            long count = contractRepository.countByStatusAndIsLatestVersionTrue(status, year);
            statusCounts.put(status, count);
        }

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
                double percentage = (count * 100.0) / totalPieContracts;
                pieChartData.add(new PieChartData(status.name(), percentage));
            }
        } else {
            for (ContractStatus status : pieStatuses) {
                pieChartData.add(new PieChartData(status.name(), 0.0));
            }
        }

        List<Object[]> monthCountsRaw = contractRepository.countLatestContractsByMonth(year);
        List<MonthlyContractCount> monthlyCounts = monthCountsRaw.stream()
                .map(row -> {
                    String monthAbbr = (String) row[0];
                    long count = ((Number) row[1]).longValue();
                    return new MonthlyContractCount(monthAbbr, count);
                })
                .collect(Collectors.toList());

        return new DashboardStatisticsResponse(statusCounts, monthlyCounts, pieChartData);
    }

    @Override
    public Workbook generateTimeReportExcel(LocalDateTime from, LocalDateTime to, String groupBy) {
        logger.info("Tạo báo cáo thời gian từ {} đến {} theo nhóm {}", from, to, groupBy);

        if ("TAXCODE".equalsIgnoreCase(groupBy)) {
            // Lấy danh sách hợp đồng theo trạng thái và thời gian
            List<Contract> contracts = contractRepository.findByIsLatestVersionTrueAndEffectiveDateBetweenAndStatusIn(
                    from, to, VALID_STATUSES,
                    Sort.by("partner.taxCode").ascending()
                            .and(Sort.by("effectiveDate").ascending())
            );

            Workbook wb = new XSSFWorkbook();
            Sheet summarySheet = wb.createSheet("Summary by Tax Code");
            Sheet detailsSheet = wb.createSheet("Contract Details");

            // Tạo header cho sheet tóm tắt
            Row summaryHeader = summarySheet.createRow(0);
            String[] summaryCols = {"Tax Code", "Total Contracts", "Total Value"};
            for (int i = 0; i < summaryCols.length; i++) {
                summaryHeader.createCell(i).setCellValue(summaryCols[i]);
            }

            // Nhóm hợp đồng theo mã số thuế
            Map<String, List<Contract>> contractsByTaxCode = contracts.stream()
                    .collect(Collectors.groupingBy(c -> c.getPartner().getTaxCode()));

            int summaryRowNum = 1;
            for (Map.Entry<String, List<Contract>> entry : contractsByTaxCode.entrySet()) {
                String taxCode = entry.getKey();
                List<Contract> taxCodeContracts = entry.getValue();
                long totalContracts = taxCodeContracts.size();
                double totalValue = taxCodeContracts.stream()
                        .mapToDouble(c -> c.getAmount() != null ? c.getAmount() : 0)
                        .sum();

                Row row = summarySheet.createRow(summaryRowNum++);
                row.createCell(0).setCellValue(taxCode);
                row.createCell(1).setCellValue(totalContracts);
                row.createCell(2).setCellValue(totalValue);
            }

            // Tạo header cho sheet chi tiết
            Row detailsHeader = detailsSheet.createRow(0);
            String[] detailsCols = {"Tax Code", "Customer ID", "Customer Name", "Contract #", "Signing Date", "Amount", "Status"};
            for (int i = 0; i < detailsCols.length; i++) {
                detailsHeader.createCell(i).setCellValue(detailsCols[i]);
            }

            int detailsRowNum = 1;
            for (Contract c : contracts) {
                Row row = detailsSheet.createRow(detailsRowNum++);
                row.createCell(0).setCellValue(c.getPartner().getTaxCode());
                row.createCell(1).setCellValue(c.getPartner().getId());
                row.createCell(2).setCellValue(c.getPartner().getPartnerName());
                row.createCell(3).setCellValue(c.getContractNumber());
                row.createCell(4).setCellValue(c.getSigningDate().format(dtf));
                row.createCell(5).setCellValue(c.getAmount() != null ? c.getAmount() : 0);
                row.createCell(6).setCellValue(c.getStatus().name());
            }

            // Tự động điều chỉnh kích thước cột
            for (int i = 0; i < summaryCols.length; i++) {
                summarySheet.autoSizeColumn(i);
            }
            for (int i = 0; i < detailsCols.length; i++) {
                detailsSheet.autoSizeColumn(i);
            }

            logger.info("Hoàn thành tạo Workbook thời gian với {} mã số thuế và {} hợp đồng", summaryRowNum - 1, detailsRowNum - 1);
            return wb;
        } else {
            // Logic hiện tại cho MONTH hoặc YEAR
            List<Object[]> currentPeriodData;
            String periodLabel;

            switch (groupBy.toUpperCase()) {
                case "YEAR":
                    currentPeriodData = contractRepository.reportByYearWithStatuses(from, to, VALID_STATUSES);
                    periodLabel = "Year";
                    break;
                case "MONTH":
                default:
                    currentPeriodData = contractRepository.reportByMonthWithStatuses(from, to, VALID_STATUSES);
                    periodLabel = "Month";
                    break;
            }

            logger.info("Tìm thấy {} hàng dữ liệu cho khoảng thời gian hiện tại từ {} đến {}",
                    currentPeriodData.size(), from, to);

            List<Object[]> previousPeriodData = new ArrayList<>();
            if (!currentPeriodData.isEmpty()) {
                String earliestPeriod = currentPeriodData.stream()
                        .map(row -> row[0].toString())
                        .min(String::compareTo)
                        .orElse(null);
                String latestPeriod = currentPeriodData.stream()
                        .map(row -> row[0].toString())
                        .max(String::compareTo)
                        .orElse(null);

                if (earliestPeriod != null && latestPeriod != null) {
                    LocalDateTime previousFrom;
                    LocalDateTime previousTo;

                    if (groupBy.toUpperCase().equals("YEAR")) {
                        int earliestYear = Integer.parseInt(earliestPeriod);
                        int latestYear = Integer.parseInt(latestPeriod);
                        int previousEarliestYear = earliestYear - 1;
                        previousFrom = LocalDateTime.of(previousEarliestYear, 1, 1, 0, 0);
                        previousTo = LocalDateTime.of(latestYear - 1, 12, 31, 23, 59, 59);
                        previousPeriodData = contractRepository.reportByYearWithStatuses(previousFrom, previousTo, VALID_STATUSES);
                    } else {
                        int earliestYear = Integer.parseInt(earliestPeriod.substring(0, 4));
                        int earliestMonth = Integer.parseInt(earliestPeriod.substring(5, 7));
                        LocalDateTime earliestDate = LocalDateTime.of(earliestYear, earliestMonth, 1, 0, 0);
                        LocalDateTime previousEarliestDate = earliestDate.minusMonths(1);
                        previousFrom = previousEarliestDate.withDayOfMonth(1);
                        previousTo = previousEarliestDate.withDayOfMonth(previousEarliestDate.getMonth().length(previousEarliestDate.toLocalDate().isLeapYear()));
                        previousPeriodData = contractRepository.reportByMonthWithStatuses(previousFrom, previousTo, VALID_STATUSES);
                    }
                }
            }

            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet("Time Report");

            Row header = sheet.createRow(0);
            String[] cols = {periodLabel, "Contract Count", "Total Value", "% Change Count (vs Previous)", "% Change Value (vs Previous)"};
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }

            int rowNum = 1;
            if (currentPeriodData.isEmpty()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue("No data available for the specified period and statuses.");
            } else {
                Map<String, Object[]> previousMap = previousPeriodData.stream()
                        .collect(Collectors.toMap(row -> row[0].toString(), row -> row));

                for (Object[] rowData : currentPeriodData) {
                    Row row = sheet.createRow(rowNum++);
                    String period = rowData[0].toString();
                    long contractCount = ((Number) rowData[1]).longValue();
                    double totalValue = ((Number) rowData[2]).doubleValue();

                    row.createCell(0).setCellValue(period);
                    row.createCell(1).setCellValue(contractCount);
                    row.createCell(2).setCellValue(totalValue);

                    double countChangePercent = 0.0;
                    double valueChangePercent = 0.0;
                    String previousPeriod;

                    if (groupBy.toUpperCase().equals("YEAR")) {
                        int currentYear = Integer.parseInt(period);
                        previousPeriod = String.valueOf(currentYear - 1);
                    } else {
                        int year = Integer.parseInt(period.substring(0, 4));
                        int month = Integer.parseInt(period.substring(5, 7));
                        LocalDateTime currentDate = LocalDateTime.of(year, month, 1, 0, 0);
                        LocalDateTime previousDate = currentDate.minusMonths(1);
                        previousPeriod = previousDate.getYear() + "-" + String.format("%02d", previousDate.getMonthValue());
                    }

                    Object[] prevData = previousMap.get(previousPeriod);
                    if (prevData != null) {
                        long prevCount = ((Number) prevData[1]).longValue();
                        double prevValue = ((Number) prevData[2]).doubleValue();

                        if (prevCount > 0) {
                            countChangePercent = ((double) (contractCount - prevCount) / prevCount) * 100;
                        }
                        if (prevValue > 0) {
                            valueChangePercent = ((double) (totalValue - prevValue) / prevValue) * 100;
                        }
                    }

                    row.createCell(3).setCellValue(String.format("%.2f%%", countChangePercent));
                    row.createCell(4).setCellValue(String.format("%.2f%%", valueChangePercent));
                }
            }

            for (int i = 0; i < cols.length; i++) {
                sheet.autoSizeColumn(i);
            }

            logger.info("Hoàn thành tạo Workbook thời gian với {} hàng", rowNum - 1);
            return wb;
        }
    }

    @Override
    public Workbook generateCustomerReportExcel(LocalDateTime from, LocalDateTime to) {
        logger.info("Tạo báo cáo khách hàng từ {} đến {}", from, to);

        // Lấy danh sách hợp đồng theo trạng thái và thời gian
        List<Contract> contracts = contractRepository.findByIsLatestVersionTrueAndEffectiveDateBetweenAndStatusIn(
                from, to, VALID_STATUSES,
                Sort.by("partner.taxCode").ascending()
                        .and(Sort.by("effectiveDate").ascending())
        );

        Workbook wb = new XSSFWorkbook();
        Sheet summarySheet = wb.createSheet("Summary");
        Sheet detailsSheet = wb.createSheet("Contract Details");

        // Tạo header cho sheet tóm tắt
        Row summaryHeader = summarySheet.createRow(0);
        String[] summaryCols = {"Tax Code", "Total Contracts", "Total Value"};
        for (int i = 0; i < summaryCols.length; i++) {
            summaryHeader.createCell(i).setCellValue(summaryCols[i]);
        }

        // Nhóm hợp đồng theo mã số thuế
        Map<String, List<Contract>> contractsByTaxCode = contracts.stream()
                .collect(Collectors.groupingBy(c -> c.getPartner().getTaxCode()));

        int summaryRowNum = 1;
        for (Map.Entry<String, List<Contract>> entry : contractsByTaxCode.entrySet()) {
            String taxCode = entry.getKey();
            List<Contract> taxCodeContracts = entry.getValue();
            long totalContracts = taxCodeContracts.size();
            double totalValue = taxCodeContracts.stream()
                    .mapToDouble(c -> c.getAmount() != null ? c.getAmount() : 0)
                    .sum();

            Row row = summarySheet.createRow(summaryRowNum++);
            row.createCell(0).setCellValue(taxCode);
            row.createCell(1).setCellValue(totalContracts);
            row.createCell(2).setCellValue(totalValue);
        }

        // Tạo header cho sheet chi tiết
        Row detailsHeader = detailsSheet.createRow(0);
        String[] detailsCols = {"Tax Code", "Customer ID", "Customer Name", "Contract #", "Signing Date", "Amount", "Status"};
        for (int i = 0; i < detailsCols.length; i++) {
            detailsHeader.createCell(i).setCellValue(detailsCols[i]);
        }

        int detailsRowNum = 1;
        for (Contract c : contracts) {
            Row row = detailsSheet.createRow(detailsRowNum++);
            row.createCell(0).setCellValue(c.getPartner().getTaxCode());
            row.createCell(1).setCellValue(c.getPartner().getId());
            row.createCell(2).setCellValue(c.getPartner().getPartnerName());
            row.createCell(3).setCellValue(c.getContractNumber());
            row.createCell(4).setCellValue(c.getSigningDate().format(dtf));
            row.createCell(5).setCellValue(c.getAmount() != null ? c.getAmount() : 0);
            row.createCell(6).setCellValue(c.getStatus().name());
        }

        // Tự động điều chỉnh kích thước cột
        for (int i = 0; i < summaryCols.length; i++) {
            summarySheet.autoSizeColumn(i);
        }
        for (int i = 0; i < detailsCols.length; i++) {
            detailsSheet.autoSizeColumn(i);
        }

        logger.info("Hoàn thành tạo Workbook khách hàng với {} mã số thuế và {} hợp đồng", summaryRowNum - 1, detailsRowNum - 1);
        return wb;
    }

    @Override
    public Workbook generateStatusReportExcel(LocalDateTime from, LocalDateTime to) {
        logger.info("Tạo báo cáo trạng thái từ {} đến {}", from, to);
        List<ContractStatus> statuses = List.of(
                ContractStatus.ACTIVE,
                ContractStatus.EXPIRED,
                ContractStatus.ENDED,
                ContractStatus.CANCELLED
        );
        List<Object[]> raw = contractRepository.countByStatusesBetween(statuses, from, to);

        Map<ContractStatus, Long> map = new EnumMap<>(ContractStatus.class);
        statuses.forEach(s -> map.put(s, 0L));
        raw.forEach(o -> map.put((ContractStatus) o[0], ((Number) o[1]).longValue()));

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Status Report");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Status");
        header.createCell(1).setCellValue("Count");

        int rowNum = 1;
        for (ContractStatus s : statuses) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(s.name());
            row.createCell(1).setCellValue(map.get(s));
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);

        logger.info("Hoàn thành tạo Workbook trạng thái với {} trạng thái", rowNum - 1);
        return wb;
    }
}
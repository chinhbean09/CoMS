package com.capstone.contractmanagement.services.dashboard;

import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.repositories.IContractRepository;
import com.capstone.contractmanagement.responses.dashboard.DashboardStatisticsResponse;
import com.capstone.contractmanagement.responses.dashboard.MonthlyContractCount;
import com.capstone.contractmanagement.responses.dashboard.PieChartData;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
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
            ContractStatus.LIQUIDATED
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
                ContractStatus.APPROVED,
                ContractStatus.LIQUIDATED
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

        Workbook wb = new XSSFWorkbook();
        CellStyle boldStyle = createBoldStyle(wb);

        if ("TAXCODE".equalsIgnoreCase(groupBy)) {
            List<Contract> contracts = contractRepository.findByIsLatestVersionTrueAndEffectiveDateBetweenAndStatusIn(
                    from, to, VALID_STATUSES,
                    Sort.by("partner.taxCode").ascending()
                            .and(Sort.by("effectiveDate").ascending())
            );

            Sheet summarySheet = wb.createSheet("Tóm tắt");
            Sheet detailsSheet = wb.createSheet("Chi tiết hợp đồng");

            Row summaryHeader = summarySheet.createRow(0);
            String[] summaryCols = {"Mã khách hàng", "Mã thuế", "Tên khách hàng", "Tổng số lượng hợp đồng", "Tổng giá trị"};
            for (int i = 0; i < summaryCols.length; i++) {
                Cell cell = summaryHeader.createCell(i);
                cell.setCellValue(summaryCols[i]);
                cell.setCellStyle(boldStyle);
            }

            Map<Long, List<Contract>> contractsByPartner = contracts.stream()
                    .collect(Collectors.groupingBy(c -> c.getPartner().getId()));

            int summaryRowNum = 1;
            for (Map.Entry<Long, List<Contract>> entry : contractsByPartner.entrySet()) {
                Long partnerId = entry.getKey();
                List<Contract> partnerContracts = entry.getValue();
                String PartnerCode = partnerContracts.get(0).getPartner().getPartnerCode();
                String taxCode = partnerContracts.get(0).getPartner().getTaxCode();
                String partnerName = partnerContracts.get(0).getPartner().getPartnerName();
                long totalContracts = partnerContracts.size();
                double totalValue = partnerContracts.stream()
                        .mapToDouble(c -> c.getAmount() != null ? c.getAmount() : 0)
                        .sum();

                Row row = summarySheet.createRow(summaryRowNum++);
                row.createCell(0).setCellValue(PartnerCode);
                row.createCell(1).setCellValue(taxCode);
                row.createCell(2).setCellValue(partnerName);
                row.createCell(3).setCellValue(totalContracts);
                row.createCell(4).setCellValue(totalValue);
            }

            // Tạo header cho sheet chi tiết với tiêu đề tiếng Việt
            Row detailsHeader = detailsSheet.createRow(0);
            String[] detailsCols = {"Mã thuế", "Mã khách hàng", "Tên khách hàng", "Số hợp đồng", "Ngày hiệu lực", "Ngày hết hạn", "Số tiền", "Trạng thái"};
            for (int i = 0; i < detailsCols.length; i++) {
                Cell cell = detailsHeader.createCell(i);
                cell.setCellValue(detailsCols[i]);
                cell.setCellStyle(boldStyle);
            }

            int detailsRowNum = 1;
            for (Contract c : contracts) {
                Row row = detailsSheet.createRow(detailsRowNum++);
                row.createCell(0).setCellValue(c.getPartner().getTaxCode());
                row.createCell(1).setCellValue(c.getPartner().getPartnerCode());
                row.createCell(2).setCellValue(c.getPartner().getPartnerName());
                row.createCell(3).setCellValue(c.getContractNumber());
                row.createCell(5).setCellValue(c.getEffectiveDate() != null ? c.getEffectiveDate().format(dtf) : "N/A");
                row.createCell(6).setCellValue(c.getExpiryDate() != null ? c.getExpiryDate().format(dtf) : "N/A");
                row.createCell(7).setCellValue(c.getAmount() != null ? c.getAmount() : 0);
                row.createCell(8).setCellValue(translateContractStatusToVietnamese(c.getStatus().name())); // Dịch trạng thái sang tiếng Việt
            }

            for (int i = 0; i < summaryCols.length; i++) {
                summarySheet.autoSizeColumn(i);
            }
            for (int i = 0; i < detailsCols.length; i++) {
                detailsSheet.autoSizeColumn(i);
            }

            return wb;
        } else {
            List<Object[]> currentPeriodData;
            String periodLabel;

            switch (groupBy.toUpperCase()) {
                case "YEAR":
                    currentPeriodData = contractRepository.reportByYearWithStatuses(from, to, VALID_STATUSES);
                    periodLabel = "Năm";
                    break;
                case "MONTH":
                default:
                    currentPeriodData = contractRepository.reportByMonthWithStatuses(from, to, VALID_STATUSES);
                    periodLabel = "Tháng";
                    break;
            }

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

            Sheet sheet = wb.createSheet("Báo cáo thời gian");

            Row header = sheet.createRow(0);
            String[] cols = {periodLabel, "Tổng số lượng hợp đồng", "Tổng giá trị", "% Thay đổi số lượng (so với trước)", "% Thay đổi giá trị (so với trước)"};
            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(boldStyle);
            }

            int rowNum = 1;
            if (currentPeriodData.isEmpty()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue("Không có dữ liệu cho khoảng thời gian và trạng thái đã chỉ định.");
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

            return wb;
        }
    }

    @Override
    public Workbook generateCustomerReportExcel(LocalDateTime from, LocalDateTime to) {

        List<Contract> contracts = contractRepository.findByIsLatestVersionTrueAndEffectiveDateBetweenAndStatusIn(
                from, to, VALID_STATUSES,
                Sort.by("partner.taxCode").ascending()
                        .and(Sort.by("effectiveDate").ascending())
        );

        Workbook wb = new XSSFWorkbook();
        CellStyle boldStyle = createBoldStyle(wb);

        Sheet summarySheet = wb.createSheet("Tóm tắt");
        Sheet detailsSheet = wb.createSheet("Chi tiết hợp đồng");

        Row summaryHeader = summarySheet.createRow(0);
        String[] summaryCols = {"Mã khách hàng", "Mã thuế", "Tên khách hàng", "Tổng số lượng hợp đồng", "Tổng giá trị"};
        for (int i = 0; i < summaryCols.length; i++) {
            Cell cell = summaryHeader.createCell(i);
            cell.setCellValue(summaryCols[i]);
            cell.setCellStyle(boldStyle);
        }

        Map<Long, List<Contract>> contractsByPartner = contracts.stream()
                .collect(Collectors.groupingBy(c -> c.getPartner().getId()));

        int summaryRowNum = 1;
        for (Map.Entry<Long, List<Contract>> entry : contractsByPartner.entrySet()) {

            List<Contract> partnerContracts = entry.getValue();
            String partnerCode = partnerContracts.get(0).getPartner().getPartnerCode();
            String taxCode = partnerContracts.get(0).getPartner().getTaxCode();
            String partnerName = partnerContracts.get(0).getPartner().getPartnerName();
            long totalContracts = partnerContracts.size();
            double totalValue = partnerContracts.stream()
                    .mapToDouble(c -> c.getAmount() != null ? c.getAmount() : 0)
                    .sum();

            Row row = summarySheet.createRow(summaryRowNum++);
            row.createCell(0).setCellValue(partnerCode);
            row.createCell(1).setCellValue(taxCode);
            row.createCell(2).setCellValue(partnerName);
            row.createCell(3).setCellValue(totalContracts);
            row.createCell(4).setCellValue(totalValue);
        }

        Row detailsHeader = detailsSheet.createRow(0);
        String[] detailsCols = {"Mã thuế", "Mã khách hàng", "Tên khách hàng", "Số hợp đồng", "Ngày hiệu lực", "Ngày hết hạn", "Số tiền", "Trạng thái"};
        for (int i = 0; i < detailsCols.length; i++) {
            Cell cell = detailsHeader.createCell(i);
            cell.setCellValue(detailsCols[i]);
            cell.setCellStyle(boldStyle);
        }

        int detailsRowNum = 1;
        for (Contract c : contracts) {
            Row row = detailsSheet.createRow(detailsRowNum++);
            row.createCell(0).setCellValue(c.getPartner().getTaxCode());
            row.createCell(1).setCellValue(c.getPartner().getPartnerCode());
            row.createCell(2).setCellValue(c.getPartner().getPartnerName());
            row.createCell(3).setCellValue(c.getContractNumber());
            row.createCell(5).setCellValue(c.getEffectiveDate() != null ? c.getEffectiveDate().format(dtf) : "N/A");
            row.createCell(6).setCellValue(c.getExpiryDate() != null ? c.getExpiryDate().format(dtf) : "N/A");
            row.createCell(7).setCellValue(c.getAmount() != null ? c.getAmount() : 0);
            row.createCell(8).setCellValue(translateContractStatusToVietnamese(c.getStatus().name()));
        }

        for (int i = 0; i < summaryCols.length; i++) {
            summarySheet.autoSizeColumn(i);
        }
        for (int i = 0; i < detailsCols.length; i++) {
            detailsSheet.autoSizeColumn(i);
        }
        return wb;
    }

    private CellStyle createBoldStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private String translateContractStatusToVietnamese(String status) {
        switch (status) {
            case "DRAFT": return "Bản nháp";
            case "CREATED": return "Đã tạo";
            case "UPDATED": return "Đã cập nhật";
            case "APPROVAL_PENDING": return "Chờ phê duyệt";
            case "APPROVED": return "Đã phê duyệt";
            case "PENDING": return "Chưa ký";
            case "REJECTED": return "Bị từ chối";
            case "FIXED": return "Đã chỉnh sửa";
            case "SIGNED": return "Đã ký";
            case "ACTIVE": return "Đang có hiệu lực";
            case "COMPLETED": return "Hoàn thành";
            case "EXPIRED": return "Hết hạn";
            case "CANCELLED": return "Đã hủy";
            case "ENDED": return "Kết thúc";
            case "DELETED": return "Đã xóa";
            case "LIQUIDATED": return "Đã thanh lý";
            default: return status;
        }
    }

    @Override
    public Workbook generateStatusReportExcel(LocalDateTime from, LocalDateTime to) {
        logger.info("Tạo báo cáo trạng thái từ {} đến {}", from, to);
        List<ContractStatus> statuses = List.of(
                ContractStatus.ACTIVE,
                ContractStatus.EXPIRED,
                ContractStatus.ENDED,
                ContractStatus.CANCELLED,
                ContractStatus.LIQUIDATED
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
        return wb;
    }
}
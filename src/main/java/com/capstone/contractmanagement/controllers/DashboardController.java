package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.responses.dashboard.DashboardStatisticsResponse;
import com.capstone.contractmanagement.services.dashboard.IDashBoardService;
import com.capstone.contractmanagement.utils.MessageKeys;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;

@RestController
@RequestMapping("${api.prefix}/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    private final IDashBoardService dashBoardService;

    @GetMapping("/statistics")
    public ResponseEntity<ResponseObject> getDashboardStatistics(@RequestParam("year") int year) {
        try {
            DashboardStatisticsResponse response = dashBoardService.getDashboardData(year);
            return ResponseEntity.ok(ResponseObject.builder()
                    .message(MessageKeys.GET_DASHBOARD_STATISTICS_SUCCESSFULLY)
                    .status(HttpStatus.OK)
                    .data(response)
                    .build());
        } catch (Exception e) {
            logger.error("Lỗi khi lấy thống kê dashboard cho năm {}: {}", year, e.getMessage(), e);
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .message("Không thể lấy thống kê dashboard: " + e.getMessage())
                    .status(HttpStatus.BAD_REQUEST)
                    .build());
        }
    }

    @GetMapping("/time/export")
    public void exportTime(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "MONTH") String groupBy,
            HttpServletResponse resp
    ) throws IOException {
        try {
            logger.info("Bắt đầu tạo báo cáo thời gian từ {} đến {} theo nhóm {}", from, to, groupBy);
            Workbook wb = dashBoardService.generateTimeReportExcel(from, to, groupBy);
            String fn = String.format("time-report_%s_%s_%s.xlsx", groupBy.toLowerCase(), from.toLocalDate(), to.toLocalDate());
            resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + fn + "\"");
            resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
            resp.setHeader("Expires", "0");
            try (var out = resp.getOutputStream()) {
                wb.write(out);
                out.flush();
            }
            wb.close();
            logger.info("Hoàn thành tạo và gửi báo cáo thời gian: {}", fn);
        } catch (Exception e) {
            logger.error("Lỗi khi tạo báo cáo Excel thời gian từ {} đến {} theo nhóm {}: {}",
                    from, to, groupBy, e.getMessage(), e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain");
            resp.getWriter().write("Không thể tạo báo cáo thời gian: " + e.getMessage());
        }
    }

    @GetMapping("/customer/export")
    public void exportCustomer(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            HttpServletResponse resp
    ) throws IOException {
        try {
            logger.info("Bắt đầu tạo báo cáo khách hàng từ {} đến {}", from, to);
            Workbook wb = dashBoardService.generateCustomerReportExcel(from, to);
            String fn = String.format("customer-report_%s_%s.xlsx", from.toLocalDate(), to.toLocalDate());
            resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + fn + "\"");
            resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
            resp.setHeader("Expires", "0");
            try (var out = resp.getOutputStream()) {
                wb.write(out);
                out.flush();
            }
            wb.close();
            logger.info("Hoàn thành tạo và gửi báo cáo khách hàng: {}", fn);
        } catch (Exception e) {
            logger.error("Lỗi khi tạo báo cáo Excel khách hàng từ {} đến {}: {}",
                    from, to, e.getMessage(), e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain");
            resp.getWriter().write("Không thể tạo báo cáo khách hàng: " + e.getMessage());
        }
    }

    @GetMapping("/status/export")
    public void exportStatus(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            HttpServletResponse resp
    ) throws IOException {
        try {
            logger.info("Bắt đầu tạo báo cáo trạng thái từ {} đến {}", from, to);
            Workbook wb = dashBoardService.generateStatusReportExcel(from, to);
            String fn = String.format("status-report_%s_%s.xlsx", from.toLocalDate(), to.toLocalDate());
            resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + fn + "\"");
            resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
            resp.setHeader("Expires", "0");
            try (var out = resp.getOutputStream()) {
                wb.write(out);
                out.flush();
            }
            wb.close();
            logger.info("Hoàn thành tạo và gửi báo cáo trạng thái: {}", fn);
        } catch (Exception e) {
            logger.error("Lỗi khi tạo báo cáo Excel trạng thái từ {} đến {}: {}",
                    from, to, e.getMessage(), e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain");
            resp.getWriter().write("Không thể tạo báo cáo trạng thái: " + e.getMessage());
        }
    }
}
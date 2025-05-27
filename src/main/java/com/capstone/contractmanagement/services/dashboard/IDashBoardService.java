package com.capstone.contractmanagement.services.dashboard;

import com.capstone.contractmanagement.responses.dashboard.DashboardStatisticsResponse;
import org.apache.poi.ss.usermodel.Workbook;

import java.time.LocalDateTime;

public interface IDashBoardService {

     DashboardStatisticsResponse getDashboardData(int year);

     Workbook generateTimeReportExcel(LocalDateTime from,
                                      LocalDateTime to,
                                      String groupBy);
     Workbook generateCustomerReportExcel(LocalDateTime from,
                                          LocalDateTime to);
     Workbook generateStatusReportExcel(LocalDateTime from,
                                        LocalDateTime to);


}

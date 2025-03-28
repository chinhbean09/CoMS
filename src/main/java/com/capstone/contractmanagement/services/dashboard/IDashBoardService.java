package com.capstone.contractmanagement.services.dashboard;

import com.capstone.contractmanagement.responses.dashboard.DashboardStatisticsResponse;

public interface IDashBoardService {

     DashboardStatisticsResponse getDashboardData(int year);


}

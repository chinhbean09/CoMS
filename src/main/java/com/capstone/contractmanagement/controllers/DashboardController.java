package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.responses.dashboard.DashboardStatisticsResponse;
import com.capstone.contractmanagement.services.dashboard.DashBoardService;
import com.capstone.contractmanagement.services.dashboard.IDashBoardService;
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("${api.prefix}/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final IDashBoardService dashBoardService;

    @GetMapping("/statistics")
    public ResponseEntity<ResponseObject> getDashboardStatistics() {
        try {
            DashboardStatisticsResponse response = dashBoardService.getDashboardData();
            return ResponseEntity.ok(ResponseObject.builder()
                    .message(MessageKeys.GET_DASHBOARD_STATISTICS_SUCCESSFULLY)
                    .status(HttpStatus.OK)
                    .data(response)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .message(e.getMessage())
                    .status(HttpStatus.BAD_REQUEST)
                    .build());
        }

    }
}

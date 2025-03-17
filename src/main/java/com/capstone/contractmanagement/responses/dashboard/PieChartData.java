package com.capstone.contractmanagement.responses.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PieChartData {

    private String name; // Tên trạng thái
    private double value; // Phần trăm

}

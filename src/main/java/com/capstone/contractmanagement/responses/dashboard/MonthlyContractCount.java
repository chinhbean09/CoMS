package com.capstone.contractmanagement.responses.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyContractCount {
    private String month; // Tên tháng, ví dụ: "Jan", "Feb"
    private long contracts; // Số lượng hợp đồng trong tháng đó
}

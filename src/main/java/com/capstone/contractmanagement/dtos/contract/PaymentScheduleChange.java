package com.capstone.contractmanagement.dtos.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentScheduleChange {
    private Long paymentId; // ID của PaymentSchedule (nếu có)
    private String oldValue; // Chuỗi đại diện cho lịch thanh toán cũ
    private String newValue; // Chuỗi đại diện cho lịch thanh toán mới
    private String action; // "CREATE", "UPDATE", "DELETE"
}

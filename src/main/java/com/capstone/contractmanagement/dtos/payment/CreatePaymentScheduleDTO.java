package com.capstone.contractmanagement.dtos.payment;

import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class CreatePaymentScheduleDTO {
    private Integer paymentOrder; // Thứ tự đợt thanh toán

    private Double amount; // Số tiền thanh toán trong đợt

    private String notifyPaymentContent;

    private LocalDateTime paymentDate; // Ngày đến hạn thanh toán

    private String paymentMethod; // Ghi chú
}

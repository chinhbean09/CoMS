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

    private String currency; // Đơn vị tiền tệ

    private LocalDateTime dueDate; // Ngày đến hạn thanh toán

    private String description; // Ghi chú
}

package com.capstone.contractmanagement.dtos.payment;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentDTO {
    private Double amount;
    private LocalDateTime paymentDate;
    private LocalDateTime notifyPaymentDate;
    private String paymentMethod;
    private String notifyPaymentContent;
}

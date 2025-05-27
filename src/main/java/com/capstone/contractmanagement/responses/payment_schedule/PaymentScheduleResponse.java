package com.capstone.contractmanagement.responses.payment_schedule;

import com.capstone.contractmanagement.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentScheduleResponse {
    private Long id;
    private Integer paymentOrder;
    private Double amount;
    private LocalDateTime notifyPaymentDate;
    private LocalDateTime paymentDate;
    private PaymentStatus status;
    private String paymentMethod;
    private Integer paymentPercentage;
    private List<String> billUrls;
    private String notifyPaymentContent;
    private boolean reminderEmailSent;
    private boolean overdueEmailSent;
}

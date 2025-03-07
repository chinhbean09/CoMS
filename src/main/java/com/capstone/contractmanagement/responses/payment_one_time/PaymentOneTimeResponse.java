package com.capstone.contractmanagement.responses.payment_one_time;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOneTimeResponse {
    private Long id;
    private Double amount;
    private String currency;
    private LocalDateTime dueDate;
    private String status;
}
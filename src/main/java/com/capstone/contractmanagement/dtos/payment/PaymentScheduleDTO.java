package com.capstone.contractmanagement.dtos.payment;
import com.capstone.contractmanagement.enums.PaymentStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentScheduleDTO {
    private Long id; // ID của PaymentSchedule, nếu có (để cập nhật)
    private Integer paymentOrder; // Thứ tự đợt thanh toán
    private Double amount; // Số tiền thanh toán
    private LocalDateTime notifyPaymentDate; // Ngày thông báo thanh toán
    private LocalDateTime paymentDate; // Ngày đến hạn thanh toán
    private PaymentStatus status; // Trạng thái thanh toán
    private String paymentMethod; // Phương thức thanh toán
    private String notifyPaymentContent; // Nội dung thông báo thanh toán
    private boolean reminderEmailSent; // Cờ gửi email nhắc nhở
    private boolean overdueEmailSent; // Cờ gửi email quá hạn
}
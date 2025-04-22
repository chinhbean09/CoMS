package com.capstone.contractmanagement.entities.addendum;

import com.capstone.contractmanagement.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "addendum_payment_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddendumPaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @Column(name = "payment_order", nullable = true)
    private Integer paymentOrder;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "payment_date", nullable = true)
    private LocalDateTime paymentDate;

    @Column(name = "status", length = 50)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "payment_method", length = 500)
    private String paymentMethod;

    @Column(name = "notified_payment_date")
    private LocalDateTime notifyPaymentDate;

//    @Column(name = "notify_payment_content", columnDefinition = "TEXT")
//    private String notifyPaymentContent;

    // Liên kết với Addendum
    @ManyToOne
    @JoinColumn(name = "addendum_id", nullable = false)
    @JsonIgnore
    private Addendum addendum;

    @Column(name = "reminder_email_sent")
    private boolean reminderEmailSent;

    // Flag gửi email quá hạn
    @Column(name = "overdue_email_sent")
    private boolean overdueEmailSent;
}
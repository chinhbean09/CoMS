package com.capstone.contractmanagement.entities;

import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "payment_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @Column(name = "payment_order", nullable = true)
    private Integer paymentOrder;

    //    @Column(name = "payment_content")
//    private String paymentContent;
    @Column(name = "amount", nullable = false)
    private Double amount; // Số tiền thanh toán trong đợt

    @Column(name = "notified_payment_date")
    private LocalDateTime notifyPaymentDate;

    @Column(name = "payment_date", nullable = true)
    private LocalDateTime paymentDate; // Ngày đến hạn thanh toán

    @Column(name = "status", length = 50)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status; // Trạng thái: Chưa thanh toán, Đã thanh toán, Quá hạn

    @Column(name = "payment_method", length = 500)
    private String paymentMethod; // Ghi chú

    @Column(name = "notify_payment_content", columnDefinition = "TEXT")
    private String notifyPaymentContent;

    // Flag gửi email nhắc nhở
    @Column(name = "reminder_email_sent")
    private boolean reminderEmailSent;

    // Flag gửi email quá hạn
    @Column(name = "overdue_email_sent")
    private boolean overdueEmailSent;

    @Column(name = "payment_percentage")
    private Integer paymentPercentage;

    // Thay đổi field này thành List<String> để lưu nhiều URL
    @ElementCollection
    @CollectionTable(name = "payment_schedule_evidences", joinColumns = @JoinColumn(name = "payment_id"))
    @Column(name = "bill_url")
    private List<String> billUrls; // Lưu nhiều URL trong một trường

    // Relationship với Contract
    @ManyToOne
    @JoinColumn(name = "contract_id", nullable = true)
    @JsonIgnore
    private Contract contract;

    @ManyToOne
    @JoinColumn(name = "partner_contract_id", nullable = true)
    @JsonIgnore
    private PartnerContract partnerContract;
}
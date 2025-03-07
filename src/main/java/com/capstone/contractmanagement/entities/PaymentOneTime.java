package com.capstone.contractmanagement.entities;

import com.capstone.contractmanagement.entities.contract.Contract;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_one_time")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOneTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @Column(name = "amount", nullable = false)
    private Double amount; // Số tiền thanh toán một lần

    @Column(name = "currency", nullable = false, length = 10)
    private String currency; // Đơn vị tiền tệ

    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate; // Ngày đến hạn thanh toán

    @Column(name = "status", length = 50)
    private String status; // Trạng thái: Chưa thanh toán, Đã thanh toán, Quá hạn

    // Relationship với Contract
    @OneToOne
    @JoinColumn(name = "contract_id", nullable = false)
    @JsonIgnore
    private Contract contract;
}

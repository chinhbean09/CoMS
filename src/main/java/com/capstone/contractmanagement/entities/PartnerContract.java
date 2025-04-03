package com.capstone.contractmanagement.entities;

import com.capstone.contractmanagement.entities.contract.ContractItem;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "partner_contracts")
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PartnerContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_number", unique = true, length = 255)
    private String contractNumber;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "partner_name", length = 255)
    private String partnerName;

    @Column(name = "signing_date")
    private LocalDateTime signingDate;

    @Column(name = "effective_date")
    private LocalDateTime effectiveDate;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "file_url", length = 255)
    private String fileUrl;

    @OneToMany(mappedBy = "partnerContract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentSchedule> paymentSchedules;

    // Quan hệ với ContractItem
    @OneToMany(mappedBy = "partnerContract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContractItem> contractItems;

    @Column(name = "is_effective_notified")
    private Boolean isEffectiveNotified = false; // Mặc định là chưa gửi thông báo hiệu lực

    @Column(name = "is_expiry_notified")
    private Boolean isExpiryNotified = false; // Mặc định là chưa gửi thông báo hết hạn

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}

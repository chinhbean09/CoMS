package com.capstone.contractmanagement.entities;

import com.capstone.contractmanagement.enums.ContractStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contracts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_id")
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "contract_number", nullable = false, unique = true, length = 100)
    private String contractNumber; // Số hợp đồng

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ContractStatus status; // Enum cho các trạng thái

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Các trường từ ContractDetails
    @Column(name = "scope", length = 500)
    private String scope; // Phạm vi hợp đồng

    @Column(name = "configuration", length = 500)
    private String configuration; // Cấu hình máy chủ hoặc phần mềm

    @Column(name = "sla", length = 500)
    private String sla; // Cam kết chất lượng dịch vụ

    @Column(name = "confidentiality", length = 500)
    private String confidentiality; // Điều khoản bảo mật

    @Column(name = "obligations", length = 500)
    private String obligations; // Quyền và nghĩa vụ của các bên

    // Relationship với PaymentSchedules
    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentSchedule> paymentSchedules = new ArrayList<>();

    // Relationship với PaymentOneTime
    @OneToOne(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private PaymentOneTime paymentOneTime;

    @Column(name = "amount")
    private Double amount; // Số tiền

    // Relationship: Many-to-One với User
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToMany
    @JoinTable(
            name = "contract_terms",
            joinColumns = @JoinColumn(name = "contract_id"),
            inverseJoinColumns = @JoinColumn(name = "term_id")
    )
    private List<Term> terms = new ArrayList<>();

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AuditTrail> auditTrails = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "template_id", referencedColumnName = "template_id")
    private ContractTemplate template;

    @ManyToOne
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

//    @ManyToOne
//    @JoinColumn(name = "task_id", nullable = true)
//    private Task task;

}

    package com.capstone.contractmanagement.entities.contract;

    import com.capstone.contractmanagement.entities.*;
    import com.capstone.contractmanagement.entities.approval_workflow.ApprovalWorkflow;
    import com.capstone.contractmanagement.entities.contract_template.ContractTemplate;
    import com.capstone.contractmanagement.enums.ContractStatus;
    import com.fasterxml.jackson.annotation.JsonIgnore;
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
    public class   Contract {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "contract_id")
        private Long id;

        @Column(name = "signing_date")
        private LocalDateTime signingDate;

        @Column(name = "contract_location", length = 255)
        private String contractLocation;

        @Column(name = "contract_number", nullable = false, unique = true, length = 100)
        private String contractNumber; // Số hợp đồng

        @Column(name = "special_terms_a", columnDefinition = "TEXT")
        private String specialTermsA;

        @Column(name = "special_terms_b", columnDefinition = "TEXT")
        private String specialTermsB;


        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false)
        private ContractStatus status; // Enum cho các trạng thái

        @Column(name = "created_at")
        private LocalDateTime createdAt;

        @Column(name = "updated_at")
        private LocalDateTime updatedAt;

//        // Các trường từ ContractDetails
//        @Column(name = "scope", columnDefinition = "TEXT")
//        private String scope; // Phạm vi hợp đồng
//
//        @Column(name = "sla", length = 500)
//        private String sla; // Cam kết chất lượng dịch vụ
//
//        @Column(name = "confidentiality", columnDefinition = "TEXT")
//        private String confidentiality; // Điều khoản bảo mật
//
//        @Column(name = "obligations", columnDefinition = "TEXT")
//        private String obligations; // Quyền và nghĩa vụ của các bên

        // Relationship với PaymentSchedules
        @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
        @JsonIgnore
        private List<PaymentSchedule> paymentSchedules = new ArrayList<>();

        @Column(name = "effective_date")
        private LocalDateTime effectiveDate;

        @Column(name = "expiry_date")
        private LocalDateTime expiryDate;

        @Column(name = "notify_effective_date")
        private LocalDateTime notifyEffectiveDate;

        @Column(name = "notify_expiry_date")
        private LocalDateTime notifyExpiryDate;

        @Column(name = "notify_effective_content", length = 255)
        private String notifyEffectiveContent;

        @Column(name = "notify_expiry_content", length = 255)
        private String notifyExpiryContent;

        @Column(name = "title", nullable = false, length = 200)
        private String title;

        // Relationship với PaymentOneTime
        @OneToOne(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
        @JsonIgnore
        private PaymentOneTime paymentOneTime;

        @Column(name = "amount")
        private Double amount; // Số tiền

        // Relationship: Many-to-One với User
        @ManyToOne
        @JoinColumn(name = "user_id", nullable = false)
        @JsonIgnore
        private User user;

        @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
        @JsonIgnore
        private List<ContractTerm> contractTerms = new ArrayList<>();

        @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
        @JsonIgnore
        private List<ContractAdditionalTermDetail> additionalTermDetails = new ArrayList<>();

        @Builder.Default
        @Column(name = "is_date_late_checked")
        private Boolean isDateLateChecked = false;


        @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
        @JsonIgnore
        private List<AuditTrail> auditTrails = new ArrayList<>();

        @Column(name = "max_date_late")
        private Integer maxDateLate;

        @ManyToOne
        @JoinColumn(name = "template_id", referencedColumnName = "template_id")
        @JsonIgnore
        private ContractTemplate template;

        @ManyToOne
        @JoinColumn(name = "party_id", nullable = false)
        @JsonIgnore
        private Party party;

        @Builder.Default
        @Column(name = "appendix_enabled")
        private Boolean appendixEnabled = false;

        //cho phép chuyển nhượng
        @Builder.Default
        @Column(name = "transfer_enabled")
        private Boolean transferEnabled = false;

        @Builder.Default
        @Column(name = "auto_add_vat")
        private Boolean autoAddVAT = false;

        //phí VAT
        @Column(name = "vat_percentage")
        private Double vatPercentage;

        @Builder.Default
        @Column(name = "auto_renew")
        private Boolean autoRenew = false;

        // Các field tùy chọn từ TemplateData
        @Column(name = "violate")
        private Boolean violate;

        @Column(name = "suspend")
        private Boolean suspend;

        @Column(name = "suspend_content", columnDefinition = "TEXT")
        private String suspendContent;

        @Column(name = "contract_content", columnDefinition = "TEXT")
        private String contractContent;

        @ManyToOne
        @JoinColumn(name = "approval_workflow_id")
        private ApprovalWorkflow approvalWorkflow;


    }

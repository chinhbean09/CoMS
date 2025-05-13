    package com.capstone.contractmanagement.entities.contract;

    import com.capstone.contractmanagement.entities.*;
    import com.capstone.contractmanagement.entities.addendum.Addendum;
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
    public class Contract {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "contract_id")
        private Long id;

        @Column(name = "signing_date")
        private LocalDateTime signingDate;

        @Column(name = "contract_location", length = 255)
        private String contractLocation;

        @Column(name = "contract_number", nullable = false, unique = true, length = 255)
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

        @Column(name = "day_deleted")
        private LocalDateTime daysDeleted;

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

        @Column(name = "amount")
        private Double amount; // Số tiền

        @ManyToOne
        @JoinColumn(name = "user_id", nullable = false)
        @JsonIgnore
        private User user;

        @Builder.Default
        @Column(name = "is_date_late_checked")
        private Boolean isDateLateChecked = false;

        @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
        @JsonIgnore
        private List<AuditTrail> auditTrails = new ArrayList<>();

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "template_id")
        @JsonIgnore
        private ContractTemplate template;

        @ManyToOne
        @JoinColumn(name = "partner_id", nullable = false)
        @JsonIgnore
        private Partner partner;

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

        // Liên kết với quy trình phê duyệt (mỗi hợp đồng có 1 quy trình duyệt riêng)
        @OneToOne
        @JoinColumn(name = "approval_workflow_id")
        @JsonIgnore
        private ApprovalWorkflow approvalWorkflow;

        @Column(name = "max_date_late")
        private Integer maxDateLate;

        @ManyToOne
        @JoinColumn(name = "contract_type_id", nullable = false)
        @JsonIgnore
        private ContractType contractType;

        @Column(name = "original_contract_id")
        private Long originalContractId;

        @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
        @JsonIgnore
        private List<ContractTerm> contractTerms;

        @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL)
        @JsonIgnore
        private List<ContractAdditionalTermDetail> additionalTermDetails = new ArrayList<>();

        @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
        @JsonIgnore
        private List<Addendum> addenda = new ArrayList<>();

        //Việc tăng số phiên bản (version) trong hệ thống giúp phản ánh rằng hợp đồng
        //đã được chỉnh sửa và cho phép bạn theo dõi trạng thái tổng thể của hợp đồng qua thời gian.
        @Column(name = "version", nullable = false)
        private Integer version = 1; // Mặc định là phiên bản 1

        @Column(name = "is_latest_version")
        private Boolean isLatestVersion = false; // Mặc định là false

        @Column(name = "duplicate_number")
        private Integer duplicateNumber = 0; // Giá trị mặc định là 0 cho hợp đồng gốc

        @Column(name = "is_effective_notified")
        private Boolean isEffectiveNotified = false; // Mặc định là chưa gửi thông báo hiệu lực

        @Column(name = "is_expiry_notified")
        private Boolean isExpiryNotified = false; // Mặc định là chưa gửi thông báo hết hạn

        @Column(name = "is_effective_overdue_notified")
        private Boolean isEffectiveOverdueNotified = false;

        // Trong class Contract, thêm vào sau các mối quan hệ khác (ví dụ: sau List<Addendum> addenda)
        @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
        @JsonIgnore
        private List<ContractItem> contractItems = new ArrayList<>();

        @Column(name = "source_contract_id")
        private Long sourceContractId; //quản lý duplicate

        @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
        @JsonIgnore
        private List<ContractPartner> contractPartners = new ArrayList<>();

        @Column(name = "signed_file_path")
        private String signedFilePath;

        @Column(name = "signed_by")
        private String signedBy;

        @Column(name = "signed_at")
        private LocalDateTime signedAt;

        @ElementCollection(fetch = FetchType.EAGER)
        @CollectionTable(name = "contract_signed_url", joinColumns = @JoinColumn(name = "contract_id"))
        @Column(name = "signed_contract_url")
        private List<String> signedContractUrls; // Lưu nhiều URL trong một trường
        // Nội dung lý do hủy
        @Column(name = "cancel_content", columnDefinition = "TEXT")
        private String cancelContent;

        @Column(name = "cancel_date")
        private LocalDateTime cancelDate;

        @ElementCollection(fetch = FetchType.EAGER)
        @CollectionTable(name = "contract_cancellation_files", joinColumns = @JoinColumn(name = "contract_id"))
        @Column(name = "cancellation_file_url")
        private List<String> cancellationFileUrls = new ArrayList<>();

        @Column(name = "liquidate_content", columnDefinition = "TEXT")
        private String liquidateContent;

        @Column(name = "liquidate_date")
        private LocalDateTime liquidateDate;

        @ElementCollection(fetch = FetchType.EAGER)
        @CollectionTable(name = "contract_liquidate_files", joinColumns = @JoinColumn(name = "contract_id"))
        @Column(name = "liquidate_file_url")
        private List<String> liquidateFileUrls = new ArrayList<>();
    }

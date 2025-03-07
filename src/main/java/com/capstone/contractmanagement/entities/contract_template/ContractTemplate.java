package com.capstone.contractmanagement.entities.contract_template;

import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.entities.contract.ContractType;
import com.capstone.contractmanagement.entities.term.Term;
import com.capstone.contractmanagement.entities.contract.Contract;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contract_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long id;

    @Column(name = "contract_title", nullable = false, columnDefinition = "TEXT", unique = true)
    private String contractTitle;

    //specialTerms cua ben A
    @Column(name = "special_termsA", columnDefinition = "TEXT")
    private String specialTermsA;

    //specialTerms cua ben B
    @Column(name = "special_termsB", columnDefinition = "TEXT")
    private String specialTermsB;

    //dc tạo phụ lục
    @Builder.Default
    @Column(name = "appendix_enabled")
    private Boolean appendixEnabled = false;

    //cho phép chuyển nhượng
    @Builder.Default
    @Column(name = "transfer_enabled")
    private Boolean transferEnabled = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    //vi phạm điều khoản
    @Builder.Default
    @Column(name = "violate")
    private Boolean violate = false;

    //tạm ngưng
    @Builder.Default
    @Column(name = "suspend")
    private Boolean suspend = false;

    //trường hợp được tạm ngưng
    @Column(name = "suspend_content", columnDefinition = "TEXT")
    private String suspendContent;

    @Column(name = "contract_content", columnDefinition = "TEXT")
    private String contractContent;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Contract> contracts = new ArrayList<>();

    //cho phép tự động add VAT
    @Builder.Default
    @Column(name = "auto_add_vat")
    private Boolean autoAddVAT = false;

    //phí VAT
    @Column(name = "vat_percentage")
    private Integer vatPercentage;

    //cho phép trễ hạn thanh toán
    @Builder.Default
    @Column(name = "is_date_late_checked")
    private Boolean isDateLateChecked = false;

    //ngày tối đa trễ hạn thanh toán
    @Column(name = "max_date_late")
    private Integer maxDateLate;

    //tự động gia hạn
    @Builder.Default
    @Column(name = "auto_renew")
    private Boolean autoRenew = false;

    //  Many-to-Many cho Legal Basis
    @ManyToMany
    @JsonIgnore
    @JoinTable(
            name = "contract_template_legal_basis",
            joinColumns = @JoinColumn(name = "template_id"),
            inverseJoinColumns = @JoinColumn(name = "term_id")
    )
    private List<Term> legalBasisTerms = new ArrayList<>();

    // Many-to-Many cho General Terms
    @ManyToMany
    @JsonIgnore

    @JoinTable(
            name = "contract_template_general_terms",
            joinColumns = @JoinColumn(name = "template_id"),
            inverseJoinColumns = @JoinColumn(name = "term_id")
    )
    private List<Term> generalTerms = new ArrayList<>();

    // Many-to-Many cho Other Terms
    @ManyToMany
    @JsonIgnore
    @JoinTable(
            name = "contract_template_other_terms",
            joinColumns = @JoinColumn(name = "template_id"),
            inverseJoinColumns = @JoinColumn(name = "term_id")
    )
    private List<Term> otherTerms = new ArrayList<>();


//    @ManyToMany
//    @JsonIgnore
//    @JoinTable(
//            name = "contract_template_additional_type_terms", // đổi tên bảng join
//            joinColumns = @JoinColumn(name = "template_id"),
//            inverseJoinColumns = @JoinColumn(name = "term_id")
//    )
//    private List<Term> additionalTerms = new ArrayList<>();


    @OneToMany(mappedBy = "contractTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ContractTemplateAdditionalTermDetail> additionalTermConfigs = new ArrayList<>();


    @ManyToOne
    @JoinColumn(name = "contract_type_id", nullable = false)
    @JsonIgnore
    private ContractType contractType;

    @Column(name = "original_template_id")
    private Long originalTemplateId; // null nếu là bản gốc

    @Column(name = "duplicate_version")
    private Integer duplicateVersion; // 0 hoặc null nếu là bản gốc, >=1 đối với các duplicate

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false) // Trường lưu ID của user tạo template
    @JsonIgnore
    private User createdBy;



}
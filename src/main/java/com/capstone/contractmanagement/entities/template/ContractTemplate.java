package com.capstone.contractmanagement.entities.template;

import com.capstone.contractmanagement.entities.Contract;
import com.capstone.contractmanagement.entities.Term;
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

    @Column(name = "contract_title", nullable = false, length = 200)
    private String contractTitle;

    @Column(name = "party_info")
    private String partyInfo;

    //vd: specialTerms cua ben A, specialTerms cua ben B
    @Column(name = "special_terms")
    private String specialTerms;

    //dc tạo phụ lục
    @Column(name = "appendix_enabled")
    private Boolean appendixEnabled;

    //cho phép chuyển nhượng
    @Column(name = "transfer_enabled")
    private Boolean transferEnabled;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    //vi phạm điều khoản
    @Column(name = "violate")
    private String violate;

    //tạm ngưng
    @Column(name = "suspend")
    private String suspend;

    //trường hợp được tạm ngưng
    @Column(name = "suspend_content")
    private String suspendContent;

    @Column(name = "contract_content", columnDefinition = "TEXT")
    private String contractContent;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Contract> contracts = new ArrayList<>();

    //cho phép tự động add VAT
    @Column(name = "auto_add_vat")
    private Boolean autoAddVAT;

    //phí VAT
    @Column(name = "vat_percentage")
    private Integer vatPercentage;

    //cho phép trễ hạn thanh toán
    @Column(name = "is_date_late_checked")
    private Boolean isDateLateChecked;

    //ngày tối đa trễ hạn thanh toán
    @Column(name = "max_date_late")
    private Integer maxDateLate;

    //tự động gia hạn
    @Column(name = "auto_renew")
    private Boolean autoRenew;

    //generalTerms, additionalTerms.
    @ManyToMany
    @JoinTable(
            name = "contract_template_terms",
            joinColumns = @JoinColumn(name = "template_id"),
            inverseJoinColumns = @JoinColumn(name = "term_id")
    )
    private List<Term> terms = new ArrayList<>();

    //clause (dieu khoan, section, sub-section  )
    @OneToMany(mappedBy = "contractTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContractTemplateClause> clauses;
}
package com.capstone.contractmanagement.entities.template;

import jakarta.persistence.*;
import lombok.*;

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

    @Lob
    @Column(name = "party_info")
    private String partyInfo;

    @Lob
    @Column(name = "special_terms")
    private String specialTerms;

    @Column(name = "appendix_enabled")
    private Boolean appendixEnabled;

    @Column(name = "transfer_enabled")
    private Boolean transferEnabled;

    @Column(name = "created_at")
    private String createdAt;

    @Column(name = "updated_at")
    private String updatedAt;

    @Column(name = "violate")
    private String violate;

    @Column(name = "suspend")
    private String suspend;

    @Lob
    @Column(name = "suspend_content")
    private String suspendContent;

    @OneToMany(mappedBy = "contractTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContractTemplateClause> clauses;
}
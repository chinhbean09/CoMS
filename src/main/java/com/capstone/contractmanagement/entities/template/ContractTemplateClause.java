package com.capstone.contractmanagement.entities.template;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "contract_template_clause")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ContractTemplateClause {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "clause_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "template_id", nullable = false)
    private ContractTemplate contractTemplate;

    //để chỉ tên nhóm chính : legalBasis, additional, RightsAndObligations
    @Column(name = "section", nullable = false)
    private String section;

    //để lưu tên sub-group (vd: additionalCommon, specialA, …).
    @Column(name = "sub_section")
    private String subSection;

    @Column(name = "clause_order")
    private Integer clauseOrder;

    //Cau truc cua sub section
    @Column(name = "label")
    private String label;

    //Cau truc cua sub section
    @Column(name = "value")
    private String value;

    //Cau truc cua sub section
    @Column(name = "key")
    private String key;
}

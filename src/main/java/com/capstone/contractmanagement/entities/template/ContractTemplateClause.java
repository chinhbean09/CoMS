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

    @Column(name = "section", nullable = false)
    private String section;

    @Column(name = "sub_section")
    private String subSection;

    @Column(name = "clause_order")
    private Integer clauseOrder;

    @Column(name = "label")
    private String label;

    @Column(name = "value")
    private String value;

    @Column(name = "key")
    private String key;
}

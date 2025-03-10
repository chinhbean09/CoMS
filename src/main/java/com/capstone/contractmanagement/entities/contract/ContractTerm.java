package com.capstone.contractmanagement.entities.contract;

import com.capstone.contractmanagement.enums.TypeTermIdentifier;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "contract_terms_snapshot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Lưu lại label của điều khoản
    @Column(name = "term_label_snapshot", columnDefinition = "TEXT")
    private String termLabel;


    // Lưu lại value của điều khoản (nội dung)
    @Column(name = "term_value_snapshot", columnDefinition = "TEXT")
    private String termValue;


    // Loại điều khoản (LEGAL_BASIS, GENERAL_TERMS, OTHER_TERMS, ADDITIONAL)
    @Enumerated(EnumType.STRING)
    @Column(name = "term_type", nullable = false)
    private TypeTermIdentifier termType;

    // Lưu lại id của điều khoản gốc
    @Column(name = "original_term_id")
    private Long originalTermId;

    // Liên kết với hợp đồng chứa bản snapshot này
    @ManyToOne
    @JoinColumn(name = "contract_id", nullable = false)
    @JsonBackReference
    private Contract contract;
}

package com.capstone.contractmanagement.entities;

import com.capstone.contractmanagement.enums.TypeTermIdentifier;
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

    // Nội dung điều khoản (được copy từ Term)
    @Column(name = "term_content", columnDefinition = "TEXT", nullable = false)
    private String termContent;

    // Loại điều khoản (LEGAL_BASIS, GENERAL_TERMS, OTHER_TERMS, ADDITIONAL)
    @Enumerated(EnumType.STRING)
    @Column(name = "term_type", nullable = false)
    private TypeTermIdentifier termType;

    // Lưu lại id của điều khoản gốc
    @Column(name = "original_term_id")
    private Long originalTermId;

    // Thông tin nhóm cho điều khoản bổ sung (Common, A, B). Nếu không phải additional thì có thể null.
    @Column(name = "additional_group", length = 50)
    private String additionalGroup;

    // Liên kết với hợp đồng chứa bản snapshot này
    @ManyToOne
    @JoinColumn(name = "contract_id", nullable = false)
    @JsonIgnore
    private Contract contract;
}

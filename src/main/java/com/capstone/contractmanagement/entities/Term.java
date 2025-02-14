package com.capstone.contractmanagement.entities;

import jakarta.persistence.*;
import lombok.*;
import net.minidev.json.annotate.JsonIgnore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "terms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Term {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "term_id")
    private Long id;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "value", nullable = false)
    private String value;

    @Column(name = "clause_code")
    private String clauseCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Liên kết Many-to-Many với Contracts thông qua bảng contract_terms
    @ManyToMany(mappedBy = "terms")
    @JsonIgnore
    private List<Contract> contracts = new ArrayList<>();
    // Liên kết Many-to-One với TypeTerm
    @ManyToOne
    @JoinColumn(name = "type_term_id", nullable = false)
    private TypeTerm typeTerm;

    // false - chưa xóa, true - đã xóa mềm
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = Boolean.FALSE;

}

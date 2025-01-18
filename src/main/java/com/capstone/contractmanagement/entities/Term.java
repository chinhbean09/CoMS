package com.capstone.contractmanagement.entities;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Liên kết Many-to-Many với Contracts thông qua bảng contract_terms
    @ManyToMany(mappedBy = "terms")
    private List<Contract> contracts = new ArrayList<>();

    // Liên kết Many-to-One với TypeTerm
//    @ManyToOne
//    @JoinColumn(name = "type_term_id", nullable = false)
//    private TypeTerm typeTerm;
}

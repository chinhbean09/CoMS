package com.capstone.contractmanagement.entities;

import com.capstone.contractmanagement.enums.TypeTermIdentifier;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "type_term")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypeTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "type_term_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    //generalTerms, additionalTerms, otherTerms, legalbasis
    @Column(name = "identifier", nullable = false, length = 255)
    @Enumerated(EnumType.STRING)
    private TypeTermIdentifier identifier;

    // Liên kết One-to-Many với Terms
    @OneToMany(mappedBy = "typeTerm", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore

    private List<Term> terms = new ArrayList<>();
}

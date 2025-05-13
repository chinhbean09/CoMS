package com.capstone.contractmanagement.entities.addendum;

import com.capstone.contractmanagement.enums.TypeTermIdentifier;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "addendum_terms_snapshot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddendumTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "term_label_snapshot", columnDefinition = "TEXT")
    private String termLabel;

    @Column(name = "term_value_snapshot", columnDefinition = "TEXT")
    private String termValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "term_type", nullable = false)
    private TypeTermIdentifier termType;

    @Column(name = "original_term_id")
    private Long originalTermId;

    // Liên kết với Addendum
    @ManyToOne
    @JoinColumn(name = "addendum_id", nullable = false)
    @JsonIgnore
    private Addendum addendum;
}
package com.capstone.contractmanagement.entities;

import com.capstone.contractmanagement.enums.TermGroup;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contract_additional_terms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractAdditionalTerm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_type")
    private TermGroup group; // COMMON, A, B

    @OneToMany(mappedBy = "additionalTerm", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<ContractAdditionalTermDetail> details = new ArrayList<>();

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "template_id", nullable = false)
    private ContractTemplate contractTemplate;

    @ManyToOne
    @JoinColumn(name = "contract_id")
    @JsonIgnore
    private Contract contract;
}


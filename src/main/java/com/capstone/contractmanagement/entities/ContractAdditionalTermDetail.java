package com.capstone.contractmanagement.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contract_additional_term_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractAdditionalTermDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "additional_term_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "contract_id", nullable = false)
    @JsonIgnore
    private Contract contract;

    @Column(name = "type_term_id", nullable = false)
    private Long typeTermId;

    // Danh sách term id cho nhóm "Common"
    @ElementCollection
    @CollectionTable(name = "contract_ct_additional_common", joinColumns = @JoinColumn(name = "additional_term_id"))
    private List<AdditionalTermSnapshot> commonTerms = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "contract_ct_additional_a", joinColumns = @JoinColumn(name = "additional_term_id"))
    private List<AdditionalTermSnapshot> aTerms = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "contract_ct_additional_b", joinColumns = @JoinColumn(name = "additional_term_id"))
    private List<AdditionalTermSnapshot> bTerms = new ArrayList<>();
}

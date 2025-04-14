package com.capstone.contractmanagement.entities.addendum;

import com.capstone.contractmanagement.entities.contract.AdditionalTermSnapshot;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "addendum_additional_term_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddendumAdditionalTermDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "additional_term_id")
    private Long id;

    // Liên kết với Addendum
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addendum_id", nullable = false)
    @JsonIgnore
    private Addendum addendum;

    @Column(name = "type_term_id", nullable = false)
    private Long typeTermId;

    @ElementCollection
    @CollectionTable(name = "addendum_ct_additional_common", joinColumns = @JoinColumn(name = "additional_term_id"))
    private List<AdditionalTermSnapshot> commonTerms = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "addendum_ct_additional_a", joinColumns = @JoinColumn(name = "additional_term_id"))
    private List<AdditionalTermSnapshot> aTerms = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "addendum_ct_additional_b", joinColumns = @JoinColumn(name = "additional_term_id"))
    private List<AdditionalTermSnapshot> bTerms = new ArrayList<>();
}
package com.capstone.contractmanagement.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

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
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content; // Snapshot nội dung

    @ManyToOne
    @JoinColumn(name = "term_id")
    private Term term; // Reference nếu cần

    @ManyToOne
    @JoinColumn(name = "additional_term_id")
    @JsonIgnore
    private ContractAdditionalTerm additionalTerm;
}

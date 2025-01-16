package com.capstone.contractmanagement.entities.template;

import jakarta.persistence.*;
import lombok.*;

@Table(name = "contract_sections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "contract_section", columnDefinition = "TEXT")
    private String content;
}

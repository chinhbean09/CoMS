package com.capstone.contractmanagement.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "banks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "bank_name", nullable = false)
    private String bankName;
    @Column(name = "branch_name", nullable = false)
    private String backAccountNumber;

    @ManyToOne
    @JoinColumn(name = "party_id")
    @JsonIgnore
    private Party party;
}

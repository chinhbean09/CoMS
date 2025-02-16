package com.capstone.contractmanagement.entities;

import com.capstone.contractmanagement.enums.PartyType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "parties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Party {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "party_id")
    private Long id;

    private String partnerCode;

    @Column(name = "party_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private PartyType partnerType; // Loại bên (Bên A, Bên B)

    @Column(name = "name", nullable = false, length = 200)
    private String partnerName; // Tên công ty hoặc cá nhân

    @Column(name = "spokesman_name", length = 100)
    private String spokesmanName;

    @Column(name = "address", length = 300)
    private String address;

    @Column(name = "tax_code", length = 50)
    private String taxCode; // Mã số thuế

    private String phone;

    private String email;

    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bank> banking = new ArrayList<>();
    // One-to-Many với Contract
    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Contract> contracts = new ArrayList<>();


}

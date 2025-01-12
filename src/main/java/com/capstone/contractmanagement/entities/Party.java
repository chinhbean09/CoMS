package com.capstone.contractmanagement.entities;

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

    @Column(name = "party_type", nullable = false, length = 50)
    private String partyType; // Loại bên (Bên A, Bên B)

    @Column(name = "name", nullable = false, length = 200)
    private String name; // Tên công ty hoặc cá nhân

    @Column(name = "address", length = 300)
    private String address;

    @Column(name = "tax_code", length = 50)
    private String taxCode; // Mã số thuế

    @Column(name = "identity_card", length = 50)
    private String identityCard; // CCCD

    @Column(name = "representative", length = 100)
    private String representative; // Người đại diện pháp luật

    @Column(name = "contact_info", length = 300)
    private String contactInfo; // Thông tin liên hệ

    // One-to-Many với Contract
    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Contract> contracts = new ArrayList<>();


}

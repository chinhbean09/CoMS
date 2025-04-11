package com.capstone.contractmanagement.entities;

import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.enums.PartnerType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.checkerframework.checker.units.qual.C;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "partners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "partner_id")
    private Long id;

    @Column(name = "partner_code", nullable = false, length = 50)
    private String partnerCode;

    @Column(name = "partner_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private PartnerType partnerType; // Loại bên (Bên A, Bên B)

    @Column(name = "abbreviation", length = 200)
    private String abbreviation;

    @Column(name = "name", nullable = false, length = 200)
    private String partnerName; // Tên công ty hoặc cá nhân

    @Column(name = "spokesman_name", length = 100)
    private String spokesmanName;

    @Column(name = "address", length = 300)
    private String address;

    @Column(name = "tax_code", length = 50)
    private String taxCode; // Mã số thuế

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;
    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "position", length = 100)
    private String position;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = Boolean.FALSE;

    @OneToMany(mappedBy = "partner", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Bank> banking = new ArrayList<>();
    // One-to-Many với Contract
    @OneToMany(mappedBy = "partner", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Contract> contracts = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

}

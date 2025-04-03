package com.capstone.contractmanagement.entities.contract;

import com.capstone.contractmanagement.entities.Partner;
import com.capstone.contractmanagement.enums.PartnerType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "contract_partners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractPartner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "contract_id", nullable = false)
    @JsonIgnore
    private Contract contract;

    @Enumerated(EnumType.STRING)
    @Column(name = "partner_type", nullable = false)
    private PartnerType partnerType;

    @Column(name = "partner_name", length = 200)
    private String partnerName;

    @Column(name = "partner_address", length = 300)
    private String partnerAddress;

    @Column(name = "partner_tax_code", length = 50)
    private String partnerTaxCode;

    @Column(name = "partner_phone", length = 20)
    private String partnerPhone;

    @Column(name = "partner_email", length = 100)
    private String partnerEmail;

    @Column(name = "spokesman_name", length = 100)
    private String spokesmanName;

    @Column(name = "position", length = 100)
    private String position;

    @ManyToOne
    @JoinColumn(name = "partner_id")
    @JsonIgnore
    private Partner partner;
}

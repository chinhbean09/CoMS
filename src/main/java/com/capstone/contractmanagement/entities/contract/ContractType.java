package com.capstone.contractmanagement.entities.contract;

import com.capstone.contractmanagement.entities.contract_template.ContractTemplate;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contract_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_type_id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;

    @Column(name = "isdeleted", nullable = false)
    private boolean isDeleted = false;

    // loại hợp đồng có thể có nhiều template
    @OneToMany(mappedBy = "contractType", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ContractTemplate> templates = new ArrayList<>();

    @OneToMany(mappedBy = "contractType", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Contract> contracts = new ArrayList<>();

}

package com.capstone.contractmanagement.entities.contract;

import com.capstone.contractmanagement.entities.approval_workflow.ApprovalWorkflow;
import com.capstone.contractmanagement.entities.contract_template.ContractTemplate;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "isdeleted", nullable = false)
    private boolean isDeleted = false;

    @OneToMany(mappedBy = "contractType", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ContractTemplate> templates = new ArrayList<>();

    @OneToMany(mappedBy = "contractType", fetch = FetchType.LAZY)
    @JsonIgnore // Bỏ qua trường contractType trong ContractTemplate
    private List<Contract> contracts = new ArrayList<>();

}

package com.capstone.contractmanagement.responses.contract;

import com.capstone.contractmanagement.entities.Party;
import com.capstone.contractmanagement.entities.contract.ContractType;
import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.responses.User.UserContractResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetAllContractReponse {
    private Long id;
    private String title;
    private UserContractResponse user;
    private LocalDateTime updatedAt;
    private String contractNumber;
    private LocalDateTime createdAt;
    private ContractType contractType;
    private Party party;
    private Double amount;
    private ContractStatus status;

}

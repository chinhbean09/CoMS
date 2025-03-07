package com.capstone.contractmanagement.services.contract;

import com.capstone.contractmanagement.dtos.contract.ContractDTO;
import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.contract.ContractResponse;
import com.capstone.contractmanagement.responses.contract.GetAllContractReponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface IContractService {

    Optional<ContractResponse> getContractById(Long id);


    ContractResponse updateContract(Long id, ContractDTO contractDTO);

    void deleteContract(Long id);

    Contract createContractFromTemplate(ContractDTO dto) throws DataNotFoundException;

    Page<GetAllContractReponse> getAllContracts(Pageable pageable, String keyword, ContractStatus status);
    }

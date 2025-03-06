package com.capstone.contractmanagement.services.contract;

import com.capstone.contractmanagement.dtos.contract.ContractDTO;
import com.capstone.contractmanagement.entities.Contract;
import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.contract.ContractResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface IContractService {

    Optional<ContractResponse> getContractById(Long id);


    ContractResponse updateContract(Long id, ContractDTO contractDTO);

    void deleteContract(Long id);

    Contract createContractFromTemplate(ContractDTO dto) throws DataNotFoundException;

    Page<ContractResponse> getAllContracts(Pageable pageable, String keyword, ContractStatus status);
    }

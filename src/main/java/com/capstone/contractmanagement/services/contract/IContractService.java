package com.capstone.contractmanagement.services.contract;

import com.capstone.contractmanagement.dtos.contract.ContractDTO;
import com.capstone.contractmanagement.entities.Contract;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.contract.CommonResponse;
import com.capstone.contractmanagement.responses.contract.ContractMergedResponse;
import com.capstone.contractmanagement.responses.contract.ContractResponse;

import java.util.List;
import java.util.Optional;

public interface IContractService {

    List<ContractResponse> getAllContracts();

    Optional<ContractResponse> getContractById(Long id);


    ContractResponse updateContract(Long id, ContractDTO contractDTO);

    void deleteContract(Long id);

    Contract createContractFromTemplate(ContractDTO dto) throws DataNotFoundException;


    }

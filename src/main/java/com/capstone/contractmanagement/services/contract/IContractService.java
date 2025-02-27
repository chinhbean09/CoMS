package com.capstone.contractmanagement.services.contract;

import com.capstone.contractmanagement.dtos.contract.ContractDTO;
import com.capstone.contractmanagement.entities.Contract;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.contract.ContractResponse;

import java.util.List;

public interface IContractService {

    List<ContractResponse> getAllContracts();

    ContractResponse getContractById(Long id);


    ContractResponse updateContract(Long id, ContractDTO contractDTO);

    void deleteContract(Long id);


    Contract createContractFromTemplate(ContractDTO dto) throws DataNotFoundException;


    }

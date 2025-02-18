package com.capstone.contractmanagement.services.contract_type;

import com.capstone.contractmanagement.entities.ContractType;

import java.util.List;
import java.util.Optional;

public interface IContractTypeService {
    List<ContractType> findAll();

    Optional<ContractType> findById(Long id);

    ContractType save(ContractType contractType);

    ContractType update(Long id, ContractType contractType);

    void delete(Long id);

     void updateDeleteStatus(Long id, Boolean isDeleted);

}

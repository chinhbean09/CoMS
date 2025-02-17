package com.capstone.contractmanagement.services.contract_type;

import com.capstone.contractmanagement.entities.ContractType;

import java.util.List;

public interface IContractTypeService {
    List<ContractType> findAll();

    ContractType findById(Long id);

    ContractType save(ContractType contractType);

    ContractType update(Long id, ContractType contractType);

    void delete(Long id);


}

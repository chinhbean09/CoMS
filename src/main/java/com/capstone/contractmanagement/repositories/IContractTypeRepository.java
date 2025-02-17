package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.ContractTemplate;
import com.capstone.contractmanagement.entities.ContractType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IContractTypeRepository extends JpaRepository<ContractType, Long> {
}

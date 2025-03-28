package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.contract.ContractTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository

public interface IContractTermRepository extends JpaRepository<ContractTerm, Long> {
}


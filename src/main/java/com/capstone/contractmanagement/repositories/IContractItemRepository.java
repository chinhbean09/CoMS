package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.contract.ContractItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IContractItemRepository extends JpaRepository<ContractItem, Long> {
}

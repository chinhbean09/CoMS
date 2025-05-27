package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.PartnerContract;
import com.capstone.contractmanagement.entities.contract.ContractItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IContractItemRepository extends JpaRepository<ContractItem, Long> {
    List<ContractItem> findByPartnerContract(PartnerContract partnerContract);
}

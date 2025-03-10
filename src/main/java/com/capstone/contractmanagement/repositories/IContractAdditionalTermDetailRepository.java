package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.entities.contract.ContractAdditionalTermDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IContractAdditionalTermDetailRepository extends JpaRepository<ContractAdditionalTermDetail, Long> {
}

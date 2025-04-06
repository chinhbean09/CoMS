package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.contract.ContractPartner;
import com.capstone.contractmanagement.enums.PartnerType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IContractPartnerRepository extends JpaRepository<ContractPartner, Long> {
    //List<ContractPartner> findByContractIdAndPartnerType(Long contractId, PartnerType partnerType);
    Optional<ContractPartner> findByContractIdAndPartnerType(Long contractId, PartnerType partnerType);
}

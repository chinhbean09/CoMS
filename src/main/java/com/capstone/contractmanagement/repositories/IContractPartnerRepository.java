package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.ContractPartner;
import com.capstone.contractmanagement.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IContractPartnerRepository extends JpaRepository<ContractPartner, Long> {
    List<ContractPartner> findByUser(User user);
}

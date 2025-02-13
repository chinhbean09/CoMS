package com.capstone.contractmanagement.repositories;


import com.capstone.contractmanagement.entities.ContractTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IContractTemplateRepository extends JpaRepository<ContractTemplate, Long> {
}

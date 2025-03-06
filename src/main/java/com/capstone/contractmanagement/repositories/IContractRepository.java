package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.Contract;
import com.capstone.contractmanagement.entities.ContractTemplate;
import com.capstone.contractmanagement.enums.ContractStatus;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.awt.print.Pageable;
import java.util.Optional;

public interface IContractRepository extends JpaRepository<Contract, Long> {
    boolean existsByContractNumber(@NotBlank(message = "Contract Number cannot be blank") String contractNumber);
    Page<Contract> findByTitleContainingIgnoreCase(String title, org.springframework.data.domain.Pageable pageable);
    Page<Contract> findByStatus(ContractStatus status, org.springframework.data.domain.Pageable pageable);
    Page<Contract> findByTitleContainingIgnoreCaseAndStatus(String title, ContractStatus status, org.springframework.data.domain.Pageable pageable);}

package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.enums.ContractStatus;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IContractRepository extends JpaRepository<Contract, Long> {
    boolean existsByContractNumber(@NotBlank(message = "Contract Number cannot be blank") String contractNumber);

    Page<Contract> findByTitleContainingIgnoreCase(String title, org.springframework.data.domain.Pageable pageable);

    Page<Contract> findByStatus(ContractStatus status, org.springframework.data.domain.Pageable pageable);

    Page<Contract> findByTitleContainingIgnoreCaseAndStatus(String title, ContractStatus status, org.springframework.data.domain.Pageable pageable);

    Page<Contract> findByStatusNot(ContractStatus status, Pageable pageable);

    Page<Contract> findByTitleContainingIgnoreCaseAndStatusNot(String title, ContractStatus status, Pageable pageable);


    long countByOriginalContractId(Long originalContractId);

}

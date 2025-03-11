package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.enums.ContractStatus;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface IContractRepository extends JpaRepository<Contract, Long> {
    boolean existsByContractNumber(@NotBlank(message = "Contract Number cannot be blank") String contractNumber);

    Page<Contract> findByTitleContainingIgnoreCase(String title, org.springframework.data.domain.Pageable pageable);

    Page<Contract> findByStatus(ContractStatus status, org.springframework.data.domain.Pageable pageable);

    Page<Contract> findByTitleContainingIgnoreCaseAndStatus(String title, ContractStatus status, org.springframework.data.domain.Pageable pageable);

    Page<Contract> findByStatusNot(ContractStatus status, Pageable pageable);

    Page<Contract> findByTitleContainingIgnoreCaseAndStatusNot(String title, ContractStatus status, Pageable pageable);

    long countByOriginalContractId(Long originalContractId);

    boolean existsByTitle(String title);

    Page<Contract> findByStatusAndContractTypeId(ContractStatus status, Long contractTypeId, Pageable pageable);

    Page<Contract> findByTitleContainingIgnoreCaseAndStatusAndContractTypeId(
            String title, ContractStatus status, Long contractTypeId, Pageable pageable);

    Page<Contract> findByStatusNotAndContractTypeId(ContractStatus status, Long contractTypeId, Pageable pageable);

    Page<Contract> findByTitleContainingIgnoreCaseAndStatusNotAndContractTypeId(
            String title, ContractStatus status, Long contractTypeId, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Contract c WHERE c.contractNumber LIKE :prefix% AND c.createdAt >= :startOfDay AND c.createdAt < :endOfDay")
    int countByContractNumberStartingWithAndDate(String prefix, LocalDateTime startOfDay, LocalDateTime endOfDay);

    @Query("SELECT MAX(c.version) FROM Contract c WHERE c.originalContractId = :originalContractId")
    Integer findMaxVersionByOriginalContractId(@Param("originalContractId") Long originalContractId);

}

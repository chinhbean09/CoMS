package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.enums.ContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IContractRepository extends JpaRepository<Contract, Long> {

    Page<Contract> findByStatus(ContractStatus status, org.springframework.data.domain.Pageable pageable);

    Page<Contract> findByTitleContainingIgnoreCaseAndStatus(String title, ContractStatus status, org.springframework.data.domain.Pageable pageable);

    Page<Contract> findByStatusNot(ContractStatus status, Pageable pageable);

    Page<Contract> findByTitleContainingIgnoreCaseAndStatusNot(String title, ContractStatus status, Pageable pageable);

    long countByOriginalContractId(Long originalContractId);

//    boolean existsByTitle(String title);

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

    // Truy vấn lấy hợp đồng phiên bản mới nhất theo status
    @Query("SELECT c FROM Contract c WHERE c.status = :status AND c.version = " +
            "(SELECT MAX(c2.version) FROM Contract c2 WHERE c2.originalContractId = c.originalContractId)")
    Page<Contract> findLatestByStatus(@Param("status") ContractStatus status, Pageable pageable);

    // Truy vấn với keyword và status
    @Query("SELECT c FROM Contract c WHERE c.title LIKE %:title% AND c.status = :status AND c.version = " +
            "(SELECT MAX(c2.version) FROM Contract c2 WHERE c2.originalContractId = c.originalContractId)")
    Page<Contract> findLatestByTitleContainingIgnoreCaseAndStatus(@Param("title") String title,
                                                                  @Param("status") ContractStatus status,
                                                                  Pageable pageable);

    // Truy vấn loại trừ status (mặc định không lấy DELETED)
    @Query("SELECT c FROM Contract c WHERE c.status != :status AND c.version = " +
            "(SELECT MAX(c2.version) FROM Contract c2 WHERE c2.originalContractId = c.originalContractId)")
    Page<Contract> findLatestByStatusNot(@Param("status") ContractStatus status, Pageable pageable);

    // Truy vấn với keyword và loại trừ status
    @Query("SELECT c FROM Contract c WHERE c.title LIKE %:title% AND c.status != :status AND c.version = " +
            "(SELECT MAX(c2.version) FROM Contract c2 WHERE c2.originalContractId = c.originalContractId)")
    Page<Contract> findLatestByTitleContainingIgnoreCaseAndStatusNot(@Param("title") String title,
                                                                     @Param("status") ContractStatus status,
                                                                     Pageable pageable);

    // Truy vấn với status và contractTypeId
    @Query("SELECT c FROM Contract c WHERE c.status = :status AND c.contractType.id = :contractTypeId AND c.version = " +
            "(SELECT MAX(c2.version) FROM Contract c2 WHERE c2.originalContractId = c.originalContractId)")
    Page<Contract> findLatestByStatusAndContractTypeId(@Param("status") ContractStatus status,
                                                       @Param("contractTypeId") Long contractTypeId,
                                                       Pageable pageable);

    // Truy vấn với keyword, status và contractTypeId
    @Query("SELECT c FROM Contract c WHERE c.title LIKE %:title% AND c.status = :status AND c.contractType.id = :contractTypeId AND c.version = " +
            "(SELECT MAX(c2.version) FROM Contract c2 WHERE c2.originalContractId = c.originalContractId)")
    Page<Contract> findLatestByTitleContainingIgnoreCaseAndStatusAndContractTypeId(@Param("title") String title,
                                                                                   @Param("status") ContractStatus status,
                                                                                   @Param("contractTypeId") Long contractTypeId,
                                                                                   Pageable pageable);

    // Truy vấn loại trừ status và contractTypeId
    @Query("SELECT c FROM Contract c WHERE c.status != :status AND c.contractType.id = :contractTypeId AND c.version = " +
            "(SELECT MAX(c2.version) FROM Contract c2 WHERE c2.originalContractId = c.originalContractId)")
    Page<Contract> findLatestByStatusNotAndContractTypeId(@Param("status") ContractStatus status,
                                                          @Param("contractTypeId") Long contractTypeId,
                                                          Pageable pageable);

    // Truy vấn với keyword, loại trừ status và contractTypeId
    @Query("SELECT c FROM Contract c WHERE c.title LIKE %:title% AND c.status != :status AND c.contractType.id = :contractTypeId AND c.version = " +
            "(SELECT MAX(c2.version) FROM Contract c2 WHERE c2.originalContractId = c.originalContractId)")
    Page<Contract> findLatestByTitleContainingIgnoreCaseAndStatusNotAndContractTypeId(@Param("title") String title,
                                                                                      @Param("status") ContractStatus status,
                                                                                      @Param("contractTypeId") Long contractTypeId,
                                                                                      Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE c.originalContractId = :originalContractId AND c.version = :version")
    Optional<Contract> findByOriginalContractIdAndVersion(@Param("originalContractId") Long originalContractId,
                                                          @Param("version") int version);
}

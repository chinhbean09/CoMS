package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.AuditTrail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface IAuditTrailRepository extends JpaRepository<AuditTrail, Long> {

    // Tìm audit trails theo contractId với phân trang
    Page<AuditTrail> findByContractId(Long contractId, Pageable pageable);

    // Tìm audit trails theo entityName và entityId với phân trang
    Page<AuditTrail> findByEntityNameAndEntityId(String entityName, Long entityId, Pageable pageable);

    // Tìm audit trails trong khoảng thời gian với phân trang
    Page<AuditTrail> findByChangedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query("SELECT a FROM AuditTrail a WHERE a.contract.originalContractId = :originalContractId")
    Page<AuditTrail> findByOriginalContractId(@Param("originalContractId") Long originalContractId, Pageable pageable);

        @Query("SELECT a FROM AuditTrail a WHERE a.contract.originalContractId = :originalContractId " +
                "AND a.entityName = :entityName")
        Page<AuditTrail> findByOriginalContractIdAndEntityName(
                @Param("originalContractId") Long originalContractId,
                @Param("entityName") String entityName,
                Pageable pageable);

    List<AuditTrail> findByContract_OriginalContractIdAndContract_VersionBetween(
            Long originalContractId,
            Integer versionStart,
            Integer versionEnd
    );

    @Query("SELECT a FROM AuditTrail a WHERE a.contract.originalContractId = :originalContractId " +
            "AND a.contract.version BETWEEN :versionStart AND :versionEnd")
    List<AuditTrail> findByContractOriginalContractIdAndVersionBetween(
            @Param("originalContractId") Long originalContractId,
            @Param("versionStart") Integer versionStart,
            @Param("versionEnd") Integer versionEnd
    );
}

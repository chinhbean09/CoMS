package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.addendum.Addendum;
import com.capstone.contractmanagement.entities.AddendumType;
import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.enums.AddendumStatus;
import com.capstone.contractmanagement.enums.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IAddendumRepository extends JpaRepository<Addendum, Long> {

    List<Addendum> findByContract(Contract contract);

    List<Addendum> findByStatus(AddendumStatus status);

    List<Addendum> findByAddendumType(AddendumType addendumType);

    Page<Addendum> findByContractUserId(Long userId, Pageable pageable);

    @Query("""
        SELECT a FROM Addendum a
        WHERE a.contract.user.id = :userId
        AND (LOWER(a.contractNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
             OR LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    Page<Addendum> findByContractUserIdAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword, Pageable pageable);

    Page<Addendum> findByContractUserIdAndStatusIn(Long userId, List<AddendumStatus> statuses, Pageable pageable);

    @Query("""
        SELECT a FROM Addendum a
        WHERE a.contract.user.id = :userId
        AND (LOWER(a.contractNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
             OR LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND a.status IN :statuses
    """)
    Page<Addendum> findByContractUserIdAndKeywordAndStatusIn(@Param("userId") Long userId, @Param("keyword") String keyword, @Param("statuses") List<AddendumStatus> statuses, Pageable pageable);

    Page<Addendum> findByContractUserIdAndAddendumTypeIdIn(Long userId, List<Long> addendumTypeIds, Pageable pageable);

    @Query("""
        SELECT a FROM Addendum a
        WHERE a.contract.user.id = :userId
        AND (LOWER(a.contractNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
             OR LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND a.addendumType.id IN :addendumTypeIds
    """)
    Page<Addendum> findByContractUserIdAndKeywordAndAddendumTypeIdIn(@Param("userId") Long userId, @Param("keyword") String keyword, @Param("addendumTypeIds") List<Long> addendumTypeIds, Pageable pageable);

    @Query("""
        SELECT a FROM Addendum a
        WHERE a.contract.user.id = :userId
        AND a.status IN :statuses
        AND a.addendumType.id IN :addendumTypeIds
    """)
    Page<Addendum> findByContractUserIdAndStatusInAndAddendumTypeIdIn(@Param("userId") Long userId, @Param("statuses") List<AddendumStatus> statuses, @Param("addendumTypeIds") List<Long> addendumTypeIds, Pageable pageable);

    @Query("""
        SELECT a FROM Addendum a
        WHERE a.contract.user.id = :userId
        AND (LOWER(a.contractNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
             OR LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND a.status IN :statuses
        AND a.addendumType.id IN :addendumTypeIds
    """)
    Page<Addendum> findByContractUserIdAndKeywordAndStatusInAndAddendumTypeIdIn(@Param("userId") Long userId, @Param("keyword") String keyword, @Param("statuses") List<AddendumStatus> statuses, @Param("addendumTypeIds") List<Long> addendumTypeIds, Pageable pageable);

    Page<Addendum> findByStatusIn(List<AddendumStatus> statuses, Pageable pageable);

    @Query("""
        SELECT a FROM Addendum a
        WHERE (LOWER(a.contractNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND a.status IN :statuses
    """)
    Page<Addendum> findByKeywordAndStatusIn(@Param("keyword") String keyword, @Param("statuses") List<AddendumStatus> statuses, Pageable pageable);

    Page<Addendum> findByAddendumTypeIdIn(List<Long> addendumTypeIds, Pageable pageable);

    @Query("""
        SELECT a FROM Addendum a
        WHERE (LOWER(a.contractNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND a.addendumType.id IN :addendumTypeIds
    """)
    Page<Addendum> findByKeywordAndAddendumTypeIdIn(@Param("keyword") String keyword, @Param("addendumTypeIds") List<Long> addendumTypeIds, Pageable pageable);

    @Query("""
        SELECT a FROM Addendum a
        WHERE a.status IN :statuses AND a.addendumType.id IN :addendumTypeIds
    """)
    Page<Addendum> findByStatusInAndAddendumTypeIdIn(@Param("statuses") List<AddendumStatus> statuses, @Param("addendumTypeIds") List<Long> addendumTypeIds, Pageable pageable);

    @Query("""
        SELECT a FROM Addendum a
        WHERE (LOWER(a.contractNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND a.status IN :statuses
        AND a.addendumType.id IN :addendumTypeIds
    """)
    Page<Addendum> findByKeywordAndStatusInAndAddendumTypeIdIn(@Param("keyword") String keyword, @Param("statuses") List<AddendumStatus> statuses, @Param("addendumTypeIds") List<Long> addendumTypeIds, Pageable pageable);

    @Query("""
        SELECT a FROM Addendum a
        WHERE (LOWER(a.contractNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    Page<Addendum> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // Đếm số phụ lục bị từ chối mà người duyệt đã từ chối
    long countByStatusAndContract_IsLatestVersionAndApprovalWorkflow_Stages_Approver_IdAndApprovalWorkflow_Stages_Status(
            AddendumStatus status, Boolean isLatestVersion, Long approverId, ApprovalStatus approvalStatus);

    long countByContract_User_IdAndStatusAndContract_IsLatestVersion(
            Long userId, AddendumStatus status, Boolean isLatestVersion);

    // Truy vấn phụ lục theo trạng thái và loại phụ lục
    List<Addendum> findByStatusAndAddendumTypeId(AddendumStatus status, Long addendumTypeId);

    boolean existsByContractIdAndTitle(Long contractId, String title);
    @Query("SELECT a.signedAddendumUrls FROM Addendum a WHERE a.id = :addendumId")
    List<String> findSignedAddendumUrls(@Param("addendumId") Long addendumId);

}

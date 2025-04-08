package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.enums.ApprovalStatus;
import com.capstone.contractmanagement.enums.ContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

import java.util.Optional;

public interface IContractRepository extends JpaRepository<Contract, Long> {
    // Các phương thức hiện có (giữ nguyên)
//    Page<Contract> findByStatus(ContractStatus status, Pageable pageable);
//
//    Page<Contract> findByTitleContainingIgnoreCaseAndStatus(String title, ContractStatus status, Pageable pageable);
//
//    Page<Contract> findByStatusNot(ContractStatus status, Pageable pageable);
//
//    Page<Contract> findByTitleContainingIgnoreCaseAndStatusNot(String title, ContractStatus status, Pageable pageable);
//
//    long countByOriginalContractId(Long originalContractId);
//
//    Page<Contract> findByStatusAndContractTypeId(ContractStatus status, Long contractTypeId, Pageable pageable);
//
//    Page<Contract> findByTitleContainingIgnoreCaseAndStatusAndContractTypeId(
//            String title, ContractStatus status, Long contractTypeId, Pageable pageable);
//
//    Page<Contract> findByStatusNotAndContractTypeId(ContractStatus status, Long contractTypeId, Pageable pageable);
//
//    Page<Contract> findByTitleContainingIgnoreCaseAndStatusNotAndContractTypeId(
//            String title, ContractStatus status, Long contractTypeId, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Contract c WHERE c.contractNumber LIKE :prefix% AND c.createdAt >= :startOfDay AND c.createdAt < :endOfDay")
    int countByContractNumberStartingWithAndDate(@Param("prefix") String prefix, @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT MAX(c.version) FROM Contract c WHERE c.originalContractId = :originalContractId")
    Integer findMaxVersionByOriginalContractId(@Param("originalContractId") Long originalContractId);

    List<Contract> findByStatusAndIsLatestVersion(ContractStatus status, boolean isLatestVersion);

    // find all latest contracts
    @Query("SELECT c FROM Contract c WHERE c.isLatestVersion = true")
    List<Contract> findLatest();

    @Query("SELECT c FROM Contract c WHERE c.status = :status AND c.isLatestVersion = true")
    Page<Contract> findLatestByStatus(@Param("status") ContractStatus status, Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE c.title LIKE %:title% AND c.status = :status AND c.isLatestVersion = true")
    Page<Contract> findLatestByTitleContainingIgnoreCaseAndStatus(@Param("title") String title,
                                                                  @Param("status") ContractStatus status,
                                                                  Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE c.status != :status AND c.isLatestVersion = true")
    Page<Contract> findLatestByStatusNot(@Param("status") ContractStatus status, Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE c.title LIKE %:title% AND c.status != :status AND c.isLatestVersion = true")
    Page<Contract> findLatestByTitleContainingIgnoreCaseAndStatusNot(@Param("title") String title,
                                                                     @Param("status") ContractStatus status,
                                                                     Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE c.status = :status AND c.contractType.id = :contractTypeId AND c.isLatestVersion = true")
    Page<Contract> findLatestByStatusAndContractTypeId(@Param("status") ContractStatus status,
                                                       @Param("contractTypeId") Long contractTypeId,
                                                       Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE c.title LIKE %:title% AND c.status = :status AND c.contractType.id = :contractTypeId AND c.isLatestVersion = true")
    Page<Contract> findLatestByTitleContainingIgnoreCaseAndStatusAndContractTypeId(@Param("title") String title,
                                                                                   @Param("status") ContractStatus status,
                                                                                   @Param("contractTypeId") Long contractTypeId,
                                                                                   Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE c.status != :status AND c.contractType.id = :contractTypeId AND c.isLatestVersion = true")
    Page<Contract> findLatestByStatusNotAndContractTypeId(@Param("status") ContractStatus status,
                                                          @Param("contractTypeId") Long contractTypeId,
                                                          Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE c.title LIKE %:title% AND c.status != :status AND c.contractType.id = :contractTypeId AND c.isLatestVersion = true")
    Page<Contract> findLatestByTitleContainingIgnoreCaseAndStatusNotAndContractTypeId(@Param("title") String title,
                                                                                      @Param("status") ContractStatus status,
                                                                                      @Param("contractTypeId") Long contractTypeId,
                                                                                      Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE c.originalContractId = :originalContractId AND c.version = :version")
    Optional<Contract> findByOriginalContractIdAndVersion(@Param("originalContractId") Long originalContractId,
                                                          @Param("version") int version);

    // Các phương thức mới dùng 'user' thay vì 'createdBy'
    @Query("SELECT c FROM Contract c WHERE c.status = :status AND c.user = :user AND c.isLatestVersion = true")
    Page<Contract> findLatestByStatusAndUser(@Param("status") ContractStatus status,
                                             @Param("user") User user,
                                             Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE c.title LIKE %:title% AND c.status = :status AND c.user = :user AND c.isLatestVersion = true")
    Page<Contract> findLatestByTitleContainingIgnoreCaseAndStatusAndUser(@Param("title") String title,
                                                                         @Param("status") ContractStatus status,
                                                                         @Param("user") User user,
                                                                         Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE c.status != :status AND c.user = :user AND c.isLatestVersion = true")
    Page<Contract> findLatestByStatusNotAndUser(@Param("status") ContractStatus status,
                                                @Param("user") User user,
                                                Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE c.title LIKE %:title% AND c.status != :status AND c.user = :user AND c.isLatestVersion = true")
    Page<Contract> findLatestByTitleContainingIgnoreCaseAndStatusNotAndUser(@Param("title") String title,
                                                                            @Param("status") ContractStatus status,
                                                                            @Param("user") User user,
                                                                            Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE c.status = :status AND c.contractType.id = :contractTypeId AND c.user = :user AND c.isLatestVersion = true")
    Page<Contract> findLatestByStatusAndContractTypeIdAndUser(@Param("status") ContractStatus status,
                                                              @Param("contractTypeId") Long contractTypeId,
                                                              @Param("user") User user,
                                                              Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE c.title LIKE %:title% AND c.status = :status AND c.contractType.id = :contractTypeId AND c.user = :user AND c.isLatestVersion = true")
    Page<Contract> findLatestByTitleContainingIgnoreCaseAndStatusAndContractTypeIdAndUser(@Param("title") String title,
                                                                                          @Param("status") ContractStatus status,
                                                                                          @Param("contractTypeId") Long contractTypeId,
                                                                                          @Param("user") User user,
                                                                                          Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE c.status != :status AND c.contractType.id = :contractTypeId AND c.user = :user AND c.isLatestVersion = true")
    Page<Contract> findLatestByStatusNotAndContractTypeIdAndUser(@Param("status") ContractStatus status,
                                                                 @Param("contractTypeId") Long contractTypeId,
                                                                 @Param("user") User user,
                                                                 Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE c.title LIKE %:title% AND c.status != :status AND c.contractType.id = :contractTypeId AND c.user = :user AND c.isLatestVersion = true")
    Page<Contract> findLatestByTitleContainingIgnoreCaseAndStatusNotAndContractTypeIdAndUser(@Param("title") String title,
                                                                                             @Param("status") ContractStatus status,
                                                                                             @Param("contractTypeId") Long contractTypeId,
                                                                                             @Param("user") User user,
                                                                                             Pageable pageable);

    Optional<Contract> findByOriginalContractIdAndVersion(Long originalContractId, Integer version);

    @Query("SELECT c FROM Contract c WHERE c.originalContractId = :originalContractId")
    Page<Contract> findAllByOriginalContractId(@Param("originalContractId") Long originalContractId, Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE c.originalContractId = :originalContractId")
    List<Contract> findAllByOriginalContractId(@Param("originalContractId") Long originalContractId);


    @Query("SELECT c FROM Contract c WHERE c.originalContractId = :originalContractId AND c.user = :user")
    Page<Contract> findAllByOriginalContractIdAndUser(@Param("originalContractId") Long originalContractId,
                                                      @Param("user") User user,
                                                      Pageable pageable);

        List<Contract> findByOriginalContractIdOrderByVersionDesc(Long originalContractId);



    List<Contract> findByOriginalContractIdAndVersionIn(Long originalContractId, List<Integer> versions);

    Page<Contract> findByPartner_IdAndStatusIn(Long partnerId, List<ContractStatus> statuses, Pageable pageable);
    @Query("SELECT c FROM Contract c " +
            "WHERE c.partner.id = :partnerId " +
            "  AND (:keyword IS NULL OR (lower(c.title) LIKE :keyword " +
            "       OR lower(c.contractNumber) LIKE :keyword)) " +
            "  AND (:status IS NULL OR c.status = :status) " +
            "  AND (:signingDate IS NULL OR c.signingDate = :signingDate) " +
            "  AND c.status IN (?#{#statuses})" +
            "  AND c.isLatestVersion = true")

    Page<Contract> searchContractsByPartnerAndFilters(
            @Param("partnerId") Long partnerId,
            @Param("keyword") String keyword,
            @Param("status") ContractStatus status,
            @Param("signingDate") LocalDateTime signingDate,
            @Param("statuses") List<ContractStatus> statuses,
            Pageable pageable);

    @Query("SELECT COUNT(c) FROM Contract c WHERE c.status = :status AND c.isLatestVersion = true AND EXTRACT(YEAR FROM c.createdAt) = :year")
    long countByStatusAndIsLatestVersionTrue(@Param("status") ContractStatus status, @Param("year") int year);

    @Query(value = """
    SELECT 
        TO_CHAR(TO_DATE(m.month::text, 'MM'), 'Mon'), 
        COALESCE(COUNT(c.contract_id), 0) 
    FROM 
        generate_series(1, 12) AS m(month) 
    LEFT JOIN 
        contracts c 
        ON EXTRACT(MONTH FROM c.created_at) = m.month 
        AND EXTRACT(YEAR FROM c.created_at) = :year 
        AND c.is_latest_version = true 
    GROUP BY 
        TO_CHAR(TO_DATE(m.month::text, 'MM'), 'Mon'), 
        m.month 
    ORDER BY 
        m.month
""", nativeQuery = true)
    List<Object[]> countLatestContractsByMonth(@Param("year") int year);

    // Phương thức cho CEO: lọc theo danh sách statuses
    @Query("SELECT c FROM Contract c WHERE c.status IN :statuses AND c.isLatestVersion = true")
    Page<Contract> findLatestByStatusIn(@Param("statuses") List<ContractStatus> statuses, Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE c.title LIKE %:title% AND c.status IN :statuses AND c.isLatestVersion = true")
    Page<Contract> findLatestByTitleContainingIgnoreCaseAndStatusIn(
            @Param("title") String title,
            @Param("statuses") List<ContractStatus> statuses,
            Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE c.status IN :statuses AND c.contractType.id = :contractTypeId AND c.isLatestVersion = true")
    Page<Contract> findLatestByStatusInAndContractTypeId(
            @Param("statuses") List<ContractStatus> statuses,
            @Param("contractTypeId") Long contractTypeId,
            Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE c.title LIKE %:title% AND c.status IN :statuses AND c.contractType.id = :contractTypeId AND c.isLatestVersion = true")
    Page<Contract> findLatestByTitleContainingIgnoreCaseAndStatusInAndContractTypeId(
            @Param("title") String title,
            @Param("statuses") List<ContractStatus> statuses,
            @Param("contractTypeId") Long contractTypeId,
            Pageable pageable);

    // Phương thức cho STAFF: lọc theo danh sách statuses và user
    @Query("SELECT c FROM Contract c WHERE c.status IN :statuses AND c.user = :user AND c.isLatestVersion = true")
    Page<Contract> findLatestByStatusInAndUser(
            @Param("statuses") List<ContractStatus> statuses,
            @Param("user") User user,
            Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE c.title LIKE %:title% AND c.status IN :statuses AND c.user = :user AND c.isLatestVersion = true")
    Page<Contract> findLatestByTitleContainingIgnoreCaseAndStatusInAndUser(
            @Param("title") String title,
            @Param("statuses") List<ContractStatus> statuses,
            @Param("user") User user,
            Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE c.status IN :statuses AND c.contractType.id = :contractTypeId AND c.user = :user AND c.isLatestVersion = true")
    Page<Contract> findLatestByStatusInAndContractTypeIdAndUser(
            @Param("statuses") List<ContractStatus> statuses,
            @Param("contractTypeId") Long contractTypeId,
            @Param("user") User user,
            Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE c.title LIKE %:title% AND c.status IN :statuses AND c.contractType.id = :contractTypeId AND c.user = :user AND c.isLatestVersion = true")
    Page<Contract> findLatestByTitleContainingIgnoreCaseAndStatusInAndContractTypeIdAndUser(
            @Param("title") String title,
            @Param("statuses") List<ContractStatus> statuses,
            @Param("contractTypeId") Long contractTypeId,
            @Param("user") User user,
            Pageable pageable);

    // Đếm số hợp đồng cần phê duyệt mà người duyệt được giao
    long countByStatusAndIsLatestVersionAndApprovalWorkflow_Stages_Approver_IdAndApprovalWorkflow_Stages_Status(
            ContractStatus status, Boolean isLatestVersion, Long approverId, ApprovalStatus approvalStatus);

    long countByUser_IdAndStatusAndApprovalWorkflow_Stages_Approver_IdAndApprovalWorkflow_Stages_Status(
            Long userId,
            ContractStatus status,
            Long approverId,
            ApprovalStatus approvalStatus
    );

    // Đếm số hợp đồng của nhân viên đang ở trạng thái APPROVAL_PENDING
    long countByUser_IdAndStatusAndIsLatestVersion(Long userId, ContractStatus status, Boolean isLatestVersion);

    Integer findMaxDuplicateNumberByOriginalContractId(Long id);

    @Query("SELECT MAX(c.duplicateNumber) FROM Contract c WHERE c.sourceContractId = :sourceContractId")
    Integer findMaxDuplicateNumberBySourceContractId(Long sourceContractId);

    boolean existsByPartnerIdAndStatus(Long partnerId, ContractStatus status);

    boolean existsByContractNumber(String contractNumber);
}
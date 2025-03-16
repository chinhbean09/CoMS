package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.entities.contract.Contract;
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
    Page<Contract> findByStatus(ContractStatus status, Pageable pageable);

    Page<Contract> findByTitleContainingIgnoreCaseAndStatus(String title, ContractStatus status, Pageable pageable);

    Page<Contract> findByStatusNot(ContractStatus status, Pageable pageable);

    Page<Contract> findByTitleContainingIgnoreCaseAndStatusNot(String title, ContractStatus status, Pageable pageable);

    long countByOriginalContractId(Long originalContractId);

    Page<Contract> findByStatusAndContractTypeId(ContractStatus status, Long contractTypeId, Pageable pageable);

    Page<Contract> findByTitleContainingIgnoreCaseAndStatusAndContractTypeId(
            String title, ContractStatus status, Long contractTypeId, Pageable pageable);

    Page<Contract> findByStatusNotAndContractTypeId(ContractStatus status, Long contractTypeId, Pageable pageable);

    Page<Contract> findByTitleContainingIgnoreCaseAndStatusNotAndContractTypeId(
            String title, ContractStatus status, Long contractTypeId, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Contract c WHERE c.contractNumber LIKE :prefix% AND c.createdAt >= :startOfDay AND c.createdAt < :endOfDay")
    int countByContractNumberStartingWithAndDate(@Param("prefix") String prefix, @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT MAX(c.version) FROM Contract c WHERE c.originalContractId = :originalContractId")
    Integer findMaxVersionByOriginalContractId(@Param("originalContractId") Long originalContractId);

    List<Contract> findByStatus(ContractStatus status);

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

    @Query("SELECT c FROM Contract c WHERE c.originalContractId = :originalContractId AND c.user = :user")
    Page<Contract> findAllByOriginalContractIdAndUser(@Param("originalContractId") Long originalContractId,
                                                      @Param("user") User user,
                                                      Pageable pageable);

        List<Contract> findByOriginalContractIdOrderByVersionDesc(Long originalContractId);



    List<Contract> findByOriginalContractIdAndVersionIn(Long originalContractId, List<Integer> versions);

    // Lấy tất cả hợp đồng có is_latest_version = true
    List<Contract> findByIsLatestVersionTrue();

    // Đếm số hợp đồng theo trạng thái và chỉ lấy phiên bản mới nhất
    long countByStatusAndIsLatestVersionTrue(ContractStatus status);

    // Đếm hợp đồng theo tháng và chỉ lấy phiên bản mới nhất
    @Query("SELECT FUNCTION('MONTHNAME', c.createdAt), COUNT(c) FROM Contract c WHERE c.isLatestVersion = true GROUP BY FUNCTION('MONTH', c.createdAt), FUNCTION('YEAR', c.createdAt) ORDER BY FUNCTION('YEAR', c.createdAt), FUNCTION('MONTH', c.createdAt)")
    List<Object[]> countLatestContractsByMonth();

    @Query("SELECT c FROM Contract c WHERE c.originalContractId = :originalContractId")
    List<Contract> findByOriginalContractId(@Param("originalContractId") Long originalContractId);

    boolean existsByContractNumber(String contractNumber);

}
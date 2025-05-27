package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.Partner;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.enums.PartnerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IPartnerRepository extends JpaRepository<Partner, Long> {
//    Page<Partner> findByPartnerCodeContainingIgnoreCaseOrPartnerNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
//            String partnerCode, String partnerName, String email, Pageable pageable);
    @Query("SELECT p FROM Partner p WHERE p.isDeleted = false AND p.id <> 1 AND (" +
            "LOWER(p.partnerCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.partnerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Partner> searchByFields(@Param("search") String search, Pageable pageable);

    Page<Partner> findByIsDeletedFalseAndIdNot(Pageable pageable, Long id);

    @Query("SELECT p FROM Partner p WHERE p.isDeleted = false AND p.id <> 1 AND p.partnerType = :partnerType AND " +
            "(LOWER(p.partnerName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(p.taxCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(p.partnerCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Partner> searchByFieldsAndPartnerType(String search, PartnerType partnerType, Pageable pageable);

    // Lọc theo loại partner
    Page<Partner> findByIsDeletedFalseAndPartnerTypeAndIdNot(PartnerType partnerType, Long excludedId, Pageable pageable);

    boolean existsByTaxCode(String taxCode);

    boolean existsByTaxCodeAndPartnerType(String taxCode, PartnerType partnerType);

    @Query("SELECT p FROM Partner p WHERE p.isDeleted = false AND p.id <> 1 AND p.user = :user AND (" +
            "LOWER(p.partnerCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.partnerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Partner> searchByFieldsAndUser(@Param("search") String search, @Param("user") User user, Pageable pageable);

    @Query("SELECT p FROM Partner p WHERE p.isDeleted = false AND p.id <> 1 AND p.user = :user AND p.partnerType = :partnerType AND (" +
            "LOWER(p.partnerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.taxCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.partnerCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Partner> searchByFieldsAndPartnerTypeAndUser(@Param("search") String search, @Param("partnerType") PartnerType partnerType, @Param("user") User user, Pageable pageable);

    Page<Partner> findByIsDeletedFalseAndUser(User user, Pageable pageable);

    Page<Partner> findByIsDeletedFalseAndPartnerTypeAndUser(PartnerType partnerType, User user, Pageable pageable);

    // CASE A: non-director, không search, không filter partnerType
    @Query("SELECT p FROM Partner p " +
            " WHERE p.isDeleted = false " +
            "   AND (p.user = :user OR p.id = :globalId)")
    Page<Partner> findAllowedForUser(@Param("user") User user,
                                     @Param("globalId") Long globalId,
                                     Pageable pageable);

    // CASE B: non-director, có search
    @Query("SELECT p FROM Partner p " +
            " WHERE p.isDeleted = false " +
            "   AND (p.user = :user OR p.id = :globalId) " +
            "   AND (LOWER(p.partnerCode)   LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(p.partnerName)   LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(p.email)         LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Partner> searchAllowedForUser(@Param("search")   String search,
                                       @Param("user")     User user,
                                       @Param("globalId") Long globalId,
                                       Pageable pageable);

    // CASE C: non-director, có partnerType
    @Query("SELECT p FROM Partner p " +
            " WHERE p.isDeleted = false " +
            "   AND p.partnerType = :partnerType " +
            "   AND (p.user = :user OR p.id = :globalId)")
    Page<Partner> findByTypeAllowedForUser(@Param("partnerType") PartnerType partnerType,
                                           @Param("user")        User user,
                                           @Param("globalId")    Long globalId,
                                           Pageable pageable);

    // CASE D: non-director, có cả search + partnerType
    @Query("SELECT p FROM Partner p " +
            " WHERE p.isDeleted = false " +
            "   AND p.partnerType = :partnerType " +
            "   AND (p.user = :user OR p.id = :globalId) " +
            "   AND (LOWER(p.partnerName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(p.taxCode)     LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(p.partnerCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Partner> searchByTypeAllowedForUser(@Param("search")      String search,
                                             @Param("partnerType") PartnerType partnerType,
                                             @Param("user")        User user,
                                             @Param("globalId")    Long globalId,
                                             Pageable pageable);

    // Cách 1: truyền nguyên entity User
    boolean existsByTaxCodeAndUser(String taxCode, User user);
}

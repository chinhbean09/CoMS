package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.Partner;
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
}

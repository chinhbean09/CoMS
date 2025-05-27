package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.Partner;
import com.capstone.contractmanagement.entities.PartnerContract;
import com.capstone.contractmanagement.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IPartnerContractRepository extends JpaRepository<PartnerContract, Long> {
    List<PartnerContract> findByUser(User user);

    @Query("SELECT cp FROM PartnerContract cp " +
            "WHERE cp.user = :user AND " +
            "(LOWER(cp.contractNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(cp.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(cp.partnerName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<PartnerContract> searchByUserAndKeyword(@Param("user") User user,
                                                 @Param("search") String search,
                                                 Pageable pageable);
    @Query("SELECT pc FROM PartnerContract pc " +
            "WHERE LOWER(pc.contractNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR LOWER(pc.partnerName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR LOWER(pc.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<PartnerContract> searchByKeyword(@Param("keyword") String keyword,
                                          Pageable pageable);

    boolean existsByContractNumberAndUser(String contractNumber, User user);

    @Query("SELECT pc FROM PartnerContract pc " +
            "WHERE pc.partner = :partner " +
            "  AND (LOWER(pc.contractNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "    OR LOWER(pc.partnerName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "    OR LOWER(pc.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<PartnerContract> searchByPartnerAndKeyword(@Param("partner") Partner partner,
                                                    @Param("keyword") String keyword,
                                                    Pageable pageable);
}

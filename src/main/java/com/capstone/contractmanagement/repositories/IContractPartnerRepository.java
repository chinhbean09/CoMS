package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.ContractPartner;
import com.capstone.contractmanagement.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IContractPartnerRepository extends JpaRepository<ContractPartner, Long> {
    List<ContractPartner> findByUser(User user);

    @Query("SELECT cp FROM ContractPartner cp " +
            "WHERE cp.user = :user AND " +
            "(LOWER(cp.contractNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(cp.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(cp.partnerName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ContractPartner> searchByUserAndKeyword(@Param("user") User user,
                                                 @Param("search") String search,
                                                 Pageable pageable);
}

package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.Party;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IPartyRepository extends JpaRepository<Party, Long> {
    Page<Party> findByPartnerCodeContainingIgnoreCaseOrPartnerNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String partnerCode, String partnerName, String email, Pageable pageable);
}

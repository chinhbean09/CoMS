package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.Bank;
import com.capstone.contractmanagement.entities.Partner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IBankRepository extends JpaRepository<Bank, Long> {

    List<Bank> findByPartner(Partner partner);
    boolean existsByBackAccountNumber(String backAccountNumber);
}

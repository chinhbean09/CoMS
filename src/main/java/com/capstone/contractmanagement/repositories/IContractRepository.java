package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IContractRepository extends JpaRepository<Contract, Long> {
}

package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.Contract;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IContractRepository extends JpaRepository<Contract, Long> {
    boolean existsByContractNumber(@NotBlank(message = "Contract Number cannot be blank") String contractNumber);
}

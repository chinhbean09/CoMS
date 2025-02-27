package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.Contract;
import com.capstone.contractmanagement.entities.ContractTemplate;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IContractRepository extends JpaRepository<Contract, Long> {
    boolean existsByContractNumber(@NotBlank(message = "Contract Number cannot be blank") String contractNumber);

}

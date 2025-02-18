package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.ContractType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IContractTypeRepository extends JpaRepository<ContractType, Long> {

    boolean existsByName(String name);

    List<ContractType> findAllByIsDeletedFalse();

    boolean existsByNameAndIdNot(String name, Long id);

}

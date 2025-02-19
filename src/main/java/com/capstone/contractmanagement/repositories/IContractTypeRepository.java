package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.ContractType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IContractTypeRepository extends JpaRepository<ContractType, Long> {

    boolean existsByName(String name);

    Optional<ContractType> findByName(String name);


    List<ContractType> findAllByIsDeletedFalse();

    boolean existsByNameAndIdNot(String name, Long id);

    Optional<ContractType> findByNameAndIsDeletedFalse(String name);

    boolean existsByNameAndIdNotAndIsDeletedFalse(String name, Long id);


}

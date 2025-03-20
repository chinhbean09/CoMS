package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.Addendum;
import com.capstone.contractmanagement.entities.AddendumType;
import com.capstone.contractmanagement.entities.contract.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IAddendumRepository extends JpaRepository<Addendum, Long> {

    List<Addendum> findByContract(Contract contract);

    List<Addendum> findByAddendumType(AddendumType addendumType);
}

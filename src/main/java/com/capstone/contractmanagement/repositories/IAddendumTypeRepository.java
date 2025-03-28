package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.AddendumType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IAddendumTypeRepository extends JpaRepository<AddendumType, Long> {
}

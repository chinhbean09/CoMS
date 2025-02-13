package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.TypeTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ITypeTermRepository extends JpaRepository<TypeTerm, Long> {

}

package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.TypeTerm;
import com.capstone.contractmanagement.enums.TypeTermIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ITypeTermRepository extends JpaRepository<TypeTerm, Long> {
    List<TypeTerm> findByIdentifier(TypeTermIdentifier identifier);

}

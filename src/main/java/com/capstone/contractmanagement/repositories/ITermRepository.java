package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.Party;
import com.capstone.contractmanagement.entities.Term;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ITermRepository extends JpaRepository<Term, Long> {

    List<Term> findByTypeTermId(Long typeTermId);

}

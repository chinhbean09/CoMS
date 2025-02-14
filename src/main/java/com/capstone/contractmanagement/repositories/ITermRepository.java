package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.Party;
import com.capstone.contractmanagement.entities.Term;
import com.capstone.contractmanagement.enums.TypeTermIdentifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ITermRepository extends JpaRepository<Term, Long> {
    int countByTypeTermId(Long typeTermId);

    List<Term> findByTypeTermId(Long typeTermId);

    // Khi truyền vào identifier (bao gồm cả LEGAL_BASIS) và tìm kiếm theo search
    @Query("SELECT t FROM Term t " +
            "WHERE t.typeTerm.identifier = :identifier " +
            "AND (t.clauseCode LIKE %:search% OR t.label LIKE %:search%)")
    Page<Term> findByTypeTermIdentifierAndSearch(@Param("identifier") TypeTermIdentifier identifier,
                                                 @Param("search") String search,
                                                 Pageable pageable);

    // Khi không truyền identifier, trả về tất cả ngoại trừ LEGAL_BASIS, tìm kiếm theo search
    @Query("SELECT t FROM Term t " +
            "WHERE t.typeTerm.identifier <> 'LEGAL_BASIS' " +
            "AND (t.clauseCode LIKE %:search% OR t.label LIKE %:search%)")
    Page<Term> findAllExcludingLegalBasicAndSearch(@Param("search") String search,
                                                   Pageable pageable);

}

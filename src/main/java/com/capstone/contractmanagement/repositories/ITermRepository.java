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

    // Lọc theo identifier
    Page<Term> findByTypeTermIdentifier(TypeTermIdentifier identifier, Pageable pageable);

    // Tìm theo danh sách loại, nhưng loại bỏ "LEGAL_BASIS"
    @Query("SELECT t FROM Term t WHERE t.typeTerm.identifier IN :identifiers AND t.typeTerm.identifier <> 'LEGAL_BASIS'")
    Page<Term> findByTypeTermIdentifierInExcludingLegalBasic(@Param("identifiers") List<TypeTermIdentifier> identifiers, Pageable pageable);


    // Trả về tất cả ngoại trừ "LEGAL_BASIC"

    Page<Term> findByIdIn(List<Long> ids, Pageable pageable);

    @Query("SELECT t FROM Term t WHERE t.typeTerm.id IN :ids")
    Page<Term> findByTypeTermIdIn(@Param("ids") java.util.List<Long> ids, Pageable pageable);

    @Query("SELECT t FROM Term t WHERE t.typeTerm.identifier = 'LEGAL_BASIS'")
    Page<Term> findByTypeTermIdentifier(Pageable pageable);

    @Query("SELECT t FROM Term t WHERE t.typeTerm.identifier <> 'LEGAL_BASIS'")
    Page<Term> findAllExcludingLegalBasic(Pageable pageable);

  

    @Query("SELECT t FROM Term t WHERE t.typeTerm.identifier = 'LEGAL_BASIS' OR t.typeTerm.id IN :ids")
    Page<Term> findByLegalBasisOrTypeTermIdIn(@Param("ids") java.util.List<Long> ids, Pageable pageable);

}

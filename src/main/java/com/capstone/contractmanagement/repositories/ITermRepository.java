package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.Term;
import com.capstone.contractmanagement.entities.TypeTerm;
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

    @Query("SELECT t FROM Term t WHERE t.typeTerm.identifier = :identifier AND t.status = 'NEW'")
    Page<Term> findByTypeTermIdentifier(@Param("identifier") TypeTermIdentifier identifier, Pageable pageable);

    @Query("SELECT t FROM Term t WHERE t.typeTerm.identifier IN :identifiers AND t.typeTerm.identifier <> 'LEGAL_BASIS' AND t.status = 'NEW'")
    Page<Term> findByTypeTermIdentifierInExcludingLegalBasic(@Param("identifiers") List<TypeTermIdentifier> identifiers, Pageable pageable);

    @Query("SELECT t FROM Term t WHERE t.typeTerm.id IN :ids  AND t.status = 'NEW'")
    Page<Term> findByTypeTermIdIn(@Param("ids") List<Long> ids, Pageable pageable);

    @Query("SELECT t FROM Term t WHERE t.typeTerm.identifier = 'LEGAL_BASIS' AND t.status = 'NEW'")
    Page<Term> findByTypeTermIdentifier(Pageable pageable);

    @Query("SELECT t FROM Term t WHERE t.typeTerm.identifier <> 'LEGAL_BASIS' AND t.status = 'NEW'")
    Page<Term> findAllExcludingLegalBasic(Pageable pageable);

    @Query("SELECT t FROM Term t WHERE (t.typeTerm.identifier = 'LEGAL_BASIS' OR t.typeTerm.id IN :ids)  AND t.status = 'NEW'")
    Page<Term> findByLegalBasisOrTypeTermIdIn(@Param("ids") List<Long> ids, Pageable pageable);

    // Các truy vấn hỗ trợ search
    @Query("SELECT t FROM Term t WHERE t.typeTerm.identifier <> 'LEGAL_BASIS' AND t.status = 'NEW' " +
            "AND (LOWER(t.label) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.clauseCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Term> findAllExcludingLegalBasicWithSearch(@Param("search") String search, Pageable pageable);

    @Query("SELECT t FROM Term t WHERE t.typeTerm.id IN :ids AND t.status = 'NEW' " +
            "AND (LOWER(t.label) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.clauseCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Term> findByTypeTermIdInWithSearch(@Param("ids") List<Long> ids, @Param("search") String search, Pageable pageable);

    @Query("SELECT t FROM Term t WHERE (t.typeTerm.identifier = 'LEGAL_BASIS' OR t.typeTerm.id IN :ids)  AND t.status = 'NEW' " +
            "AND (LOWER(t.label) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.clauseCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Term> findByLegalBasisOrTypeTermIdInWithSearch(@Param("ids") List<Long> ids, @Param("search") String search, Pageable pageable);

    @Query("SELECT t FROM Term t WHERE t.typeTerm.identifier = 'LEGAL_BASIS' AND t.status = 'NEW'")
    List<Term> findAllLegalBasisTerms();
  
    @Query("SELECT t FROM Term t WHERE t.typeTerm = :typeTerm " +
            "AND (LOWER(t.label) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(t.clauseCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Term> searchByTypeTermAndLabelOrClauseCode(@Param("typeTerm") TypeTerm typeTerm,
                                                    @Param("search") String search,
                                                    Pageable pageable);

    boolean existsByLabel(String label);

    // Đếm số lượng contract_template (tính duy nhất các template) sử dụng term
    @Query(value = "SELECT COUNT(DISTINCT template_id) FROM ( " +
            "SELECT template_id FROM contract_template_legal_basis WHERE term_id = :termId " +
            "UNION " +
            "SELECT template_id FROM contract_template_general_terms WHERE term_id = :termId " +
            "UNION " +
            "SELECT template_id FROM contract_template_other_terms WHERE term_id = :termId " +
            ") as templates", nativeQuery = true)
    int countContractTemplateUsage(@Param("termId") Long termId);

    // Đếm số lượng contract sử dụng term (giả sử có bảng join contract_terms)
    @Query(value = "SELECT COUNT(DISTINCT contract_id) FROM contract_terms WHERE term_id = :termId", nativeQuery = true)
    int countContractUsage(@Param("termId") Long termId);

}

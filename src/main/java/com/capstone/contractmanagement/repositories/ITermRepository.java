package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.entities.term.Term;
import com.capstone.contractmanagement.entities.term.TypeTerm;
import com.capstone.contractmanagement.enums.TermStatus;
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
            "AND (LOWER(t.label) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.value) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.clauseCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Term> findAllExcludingLegalBasicWithSearch(@Param("search") String search, Pageable pageable);

    @Query("SELECT t FROM Term t WHERE t.typeTerm.id IN :ids AND t.status = 'NEW' " +
            "AND (LOWER(t.label) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.value) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.clauseCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Term> findByTypeTermIdInWithSearch(@Param("ids") List<Long> ids, @Param("search") String search, Pageable pageable);

    @Query("SELECT t FROM Term t WHERE (t.typeTerm.identifier = 'LEGAL_BASIS' OR t.typeTerm.id IN :ids)  AND t.status = 'NEW' " +
            "AND (LOWER(t.label) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.value) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.clauseCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Term> findByLegalBasisOrTypeTermIdInWithSearch(@Param("ids") List<Long> ids, @Param("search") String search, Pageable pageable);

    @Query("SELECT t FROM Term t WHERE t.typeTerm.identifier = 'LEGAL_BASIS' AND t.status = 'NEW'")
    List<Term> findAllLegalBasisTerms();

    @Query("SELECT t FROM Term t WHERE t.typeTerm = :typeTerm " +
            "AND (LOWER(t.label) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(t.clauseCode) LIKE LOWER(CONCAT('%', :search, '%'))" +
            "OR LOWER(t.value) LIKE LOWER(CONCAT('%', :search, '%')))")
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
    @Query(value = "SELECT COUNT(DISTINCT contract_id) FROM contract_terms_snapshot WHERE original_term_id = :termId", nativeQuery = true)
    int countContractUsage(@Param("termId") Long termId);

    @Query("SELECT COUNT(ct) FROM ContractTemplateAdditionalTermDetail ct WHERE :termId IN elements(ct.commonTermIds) OR :termId IN elements(ct.aTermIds) OR :termId IN elements(ct.bTermIds)")
    long countByTermIdInLists(@Param("termId") Long termId);

    @Query("SELECT COUNT(t) FROM ContractTemplate t WHERE :term MEMBER OF t.legalBasisTerms OR :term MEMBER OF t.generalTerms OR :term MEMBER OF t.otherTerms")
    long countTemplatesUsingTerm(@Param("term") Term term);

    @Query("SELECT t FROM Term t WHERE LOWER(t.label) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(t.value) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Term> searchByLabelOrValue(String keyword);

    boolean existsByLabelAndTypeTermAndStatus(String label, TypeTerm typeTerm, TermStatus status);

    // 1.1 Không lọc typeTermIds, không includeLegalBasis
    @Query("""
      SELECT t FROM Term t
       WHERE t.user        = :user
         AND t.typeTerm.identifier <> 'LEGAL_BASIS'
         AND t.status      = 'NEW'
    """)
    Page<Term> findByUserExcludingLegalBasic(
            @Param("user") User user,
            Pageable pageable
    );

    @Query("""
      SELECT t FROM Term t
       WHERE t.user        = :user
         AND t.typeTerm.identifier <> 'LEGAL_BASIS'
         AND t.status      = 'NEW'
         AND (
               LOWER(t.label)      LIKE LOWER(CONCAT('%',:search,'%'))
            OR LOWER(t.value)      LIKE LOWER(CONCAT('%',:search,'%'))
            OR LOWER(t.clauseCode) LIKE LOWER(CONCAT('%',:search,'%'))
         )
    """)
    Page<Term> findByUserExcludingLegalBasicWithSearch(
            @Param("user")   User user,
            @Param("search") String search,
            Pageable pageable
    );

    // 1.2 Không lọc typeTermIds, chỉ includeLegalBasis
    @Query("""
      SELECT t FROM Term t
       WHERE t.user        = :user
         AND t.typeTerm.identifier = 'LEGAL_BASIS'
         AND t.status      = 'NEW'
    """)
    Page<Term> findByUserLegalBasis(
            @Param("user") User user,
            Pageable pageable
    );

    @Query("""
      SELECT t FROM Term t
       WHERE t.user        = :user
         AND t.typeTerm.identifier = 'LEGAL_BASIS'
         AND t.status      = 'NEW'
         AND (
               LOWER(t.label)      LIKE LOWER(CONCAT('%',:search,'%'))
            OR LOWER(t.value)      LIKE LOWER(CONCAT('%',:search,'%'))
            OR LOWER(t.clauseCode) LIKE LOWER(CONCAT('%',:search,'%'))
         )
    """)
    Page<Term> findByUserLegalBasisWithSearch(
            @Param("user") User user,
            @Param("search") String search,
            Pageable pageable
    );

    // 1.3 Lọc theo typeTermIds, không includeLegalBasis
    @Query("""
      SELECT t FROM Term t
       WHERE t.user        = :user
         AND t.typeTerm.id IN :ids
         AND t.status      = 'NEW'
    """)
    Page<Term> findByUserAndTypeTermIdIn(
            @Param("user") User user,
            @Param("ids")  List<Long> ids,
            Pageable pageable
    );

    @Query("""
      SELECT t FROM Term t
       WHERE t.user        = :user
         AND t.typeTerm.id IN :ids
         AND t.status      = 'NEW'
         AND (
               LOWER(t.label)      LIKE LOWER(CONCAT('%',:search,'%'))
            OR LOWER(t.value)      LIKE LOWER(CONCAT('%',:search,'%'))
            OR LOWER(t.clauseCode) LIKE LOWER(CONCAT('%',:search,'%'))
         )
    """)
    Page<Term> findByUserAndTypeTermIdInWithSearch(
            @Param("user")   User user,
            @Param("ids")    List<Long> ids,
            @Param("search") String search,
            Pageable pageable
    );

    // 1.4 Lọc theo typeTermIds + includeLegalBasis
    @Query("""
      SELECT t FROM Term t
       WHERE t.user        = :user
         AND (
               t.typeTerm.identifier = 'LEGAL_BASIS'
            OR t.typeTerm.id IN :ids
         )
         AND t.status      = 'NEW'
    """)
    Page<Term> findByUserLegalBasisOrTypeTermIdIn(
            @Param("user") User user,
            @Param("ids")  List<Long> ids,
            Pageable pageable
    );

    @Query("""
      SELECT t FROM Term t
       WHERE t.user        = :user
         AND (
               t.typeTerm.identifier = 'LEGAL_BASIS'
            OR t.typeTerm.id IN :ids
         )
         AND t.status      = 'NEW'
         AND (
               LOWER(t.label)      LIKE LOWER(CONCAT('%',:search,'%'))
            OR LOWER(t.value)      LIKE LOWER(CONCAT('%',:search,'%'))
            OR LOWER(t.clauseCode) LIKE LOWER(CONCAT('%',:search,'%'))
         )
    """)
    Page<Term> findByUserLegalBasisOrTypeTermIdInWithSearch(
            @Param("user")   User user,
            @Param("ids")    List<Long> ids,
            @Param("search") String search,
            Pageable pageable
    );

}

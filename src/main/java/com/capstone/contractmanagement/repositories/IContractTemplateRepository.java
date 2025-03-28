package com.capstone.contractmanagement.repositories;


import com.capstone.contractmanagement.entities.contract_template.ContractTemplate;
import com.capstone.contractmanagement.enums.ContractTemplateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IContractTemplateRepository extends JpaRepository<ContractTemplate, Long> {

    Page<ContractTemplate> findByContractTitleContainingIgnoreCase(String contractTitle, Pageable pageable);

    @EntityGraph(attributePaths = {"legalBasisTerms", "generalTerms", "otherTerms", "additionalTermConfigs"})
    Optional<ContractTemplate> findWithTermsById(Long id);

    int countByOriginalTemplateId(Long originalTemplateId);

    boolean existsByContractTitle(String contractTitle);

    Optional<ContractTemplate> findByContractTitleAndIdNot(String contractTitle, Long id);

    // Lọc theo cả title và status cụ thể
    Page<ContractTemplate> findByContractTitleContainingIgnoreCaseAndStatus(String keyword, ContractTemplateStatus status, Pageable pageable);

    // Lấy các template mà status không phải là DELETED
    Page<ContractTemplate> findByStatusNot(ContractTemplateStatus status, Pageable pageable);

    // Lấy các template theo title, loại bỏ những bản có status là DELETED
    Page<ContractTemplate> findByContractTitleContainingIgnoreCaseAndStatusNot(String keyword, ContractTemplateStatus status, Pageable pageable);

    // Lọc theo status cụ thể
    Page<ContractTemplate> findByStatus(ContractTemplateStatus status, Pageable pageable);

    Page<ContractTemplate> findByContractTitleContainingIgnoreCaseAndStatusAndContractTypeId(
            String title, ContractTemplateStatus status, Long contractTypeId, Pageable pageable);

    Page<ContractTemplate> findByContractTitleContainingIgnoreCaseAndStatusNotAndContractTypeId(
            String title, ContractTemplateStatus status, Long contractTypeId, Pageable pageable);

    Page<ContractTemplate> findByStatusAndContractTypeId(ContractTemplateStatus status, Long contractTypeId, Pageable pageable);

    Page<ContractTemplate> findByStatusNotAndContractTypeId(ContractTemplateStatus status, Long contractTypeId, Pageable pageable);

    List<ContractTemplate> findByContractTypeId(Long contractTypeId);

    Page<ContractTemplate> findByContractTypeId(Long contractTypeId, Pageable pageable);
}

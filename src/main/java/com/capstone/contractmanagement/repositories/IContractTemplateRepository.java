package com.capstone.contractmanagement.repositories;


import com.capstone.contractmanagement.entities.contract_template.ContractTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IContractTemplateRepository extends JpaRepository<ContractTemplate, Long> {
    Page<ContractTemplate> findByContractTitleContainingIgnoreCase(String contractTitle, Pageable pageable);

    @EntityGraph(attributePaths = {"legalBasisTerms", "generalTerms", "otherTerms", "additionalTermConfigs"})
    Optional<ContractTemplate> findWithTermsById(Long id);

    int countByOriginalTemplateId(Long originalTemplateId);

    boolean existsByContractTitle(String contractTitle);

    Optional<ContractTemplate> findByContractTitleAndIdNot(String contractTitle, Long id);


}

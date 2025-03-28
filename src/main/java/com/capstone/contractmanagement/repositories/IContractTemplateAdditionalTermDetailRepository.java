package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.contract_template.ContractTemplateAdditionalTermDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IContractTemplateAdditionalTermDetailRepository extends JpaRepository<ContractTemplateAdditionalTermDetail, Long> {
    @Query("SELECT COUNT(ct) FROM ContractTemplateAdditionalTermDetail ct WHERE :termId IN elements(ct.commonTermIds) OR :termId IN elements(ct.aTermIds) OR :termId IN elements(ct.bTermIds)")
    long countByTermIdInLists(@Param("termId") Long termId);

}

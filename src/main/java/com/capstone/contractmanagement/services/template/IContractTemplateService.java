package com.capstone.contractmanagement.services.template;

import com.capstone.contractmanagement.dtos.contract_template.ContractTemplateDTO;
import com.capstone.contractmanagement.dtos.contract_template.ContractTemplateIdDTO;
import com.capstone.contractmanagement.entities.contract_template.ContractTemplate;
import com.capstone.contractmanagement.enums.ContractTemplateStatus;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.template.ContractTemplateResponse;
import com.capstone.contractmanagement.responses.template.ContractTemplateResponseIds;
import com.capstone.contractmanagement.responses.template.ContractTemplateSimpleResponse;
import com.capstone.contractmanagement.responses.template.ContractTemplateTitleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface IContractTemplateService {
    ContractTemplate createTemplate(ContractTemplateDTO dto) throws DataNotFoundException;

    Page<ContractTemplateSimpleResponse> getAllTemplates(Pageable pageable, String keyword, String status, Long contractTypeId);

    Optional<ContractTemplateResponse> getTemplateById(Long id);

    Optional<ContractTemplateResponseIds> getTemplateIdsById(Long id);

    void deleteTemplate(Long id);

    Optional<ContractTemplateResponse> duplicateTemplate(Long id);

    Page<ContractTemplateTitleResponse> getAllTemplateTitles(Pageable pageable);

    ContractTemplate updateTemplate(Long templateId, ContractTemplateDTO dto) throws DataNotFoundException, IllegalArgumentException;

    boolean softDelete(Long id);

    ContractTemplateStatus updateContractTemplateStatus(Long id, ContractTemplateStatus status) throws DataNotFoundException;

     Page<ContractTemplateIdDTO> getTemplatesByContractType(Long contractTypeId, Pageable pageable);

    }


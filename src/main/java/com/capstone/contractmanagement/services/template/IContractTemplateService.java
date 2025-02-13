package com.capstone.contractmanagement.services.template;

import com.capstone.contractmanagement.dtos.template.ContractTemplateDTO;
import com.capstone.contractmanagement.entities.ContractTemplate;

import java.util.List;
import java.util.Optional;

public interface IContractTemplateService {
    ContractTemplate createTemplate(ContractTemplateDTO dto);

    List<ContractTemplate> getAllTemplates();

    Optional<ContractTemplate> getTemplateById(Long id);

    void deleteTemplate(Long id);
}

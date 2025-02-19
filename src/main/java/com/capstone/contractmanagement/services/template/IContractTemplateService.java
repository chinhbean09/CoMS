package com.capstone.contractmanagement.services.template;

import com.capstone.contractmanagement.dtos.template.ContractTemplateDTO;
import com.capstone.contractmanagement.entities.ContractTemplate;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.template.ContractTemplateResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface IContractTemplateService {
    ContractTemplate createTemplate(ContractTemplateDTO dto) throws DataNotFoundException;

     Page<ContractTemplate> getAllTemplates(Pageable pageable);

    Optional<ContractTemplateResponse> getTemplateById(Long id);

    void deleteTemplate(Long id);
}

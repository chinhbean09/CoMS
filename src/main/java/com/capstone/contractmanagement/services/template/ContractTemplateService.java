package com.capstone.contractmanagement.services.template;

import com.capstone.contractmanagement.dtos.template.ContractTemplateDTO;
import com.capstone.contractmanagement.entities.ContractTemplate;
import com.capstone.contractmanagement.entities.Term;
import com.capstone.contractmanagement.repositories.IContractTemplateRepository;
import com.capstone.contractmanagement.repositories.ITermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ContractTemplateService implements IContractTemplateService {
    private final IContractTemplateRepository templateRepository;
    private final ITermRepository termRepository;

    public ContractTemplate createTemplate(ContractTemplateDTO dto) {
        List<Term> terms = termRepository.findAllById(dto.getTermIds());

        ContractTemplate template = ContractTemplate.builder()
                .contractTitle(dto.getContractTitle())
                .partyInfo(dto.getPartyInfo())
                .specialTermsA(dto.getSpecialTermsA())
                .specialTermsB(dto.getSpecialTermsB())
                .appendixEnabled(dto.getAppendixEnabled())
                .transferEnabled(dto.getTransferEnabled())
                .violate(dto.getViolate())
                .suspend(dto.getSuspend())
                .suspendContent(dto.getSuspendContent())
                .contractContent(dto.getContractContent())
                .autoAddVAT(dto.getAutoAddVAT())
                .vatPercentage(dto.getVatPercentage())
                .isDateLateChecked(dto.getIsDateLateChecked())
                .maxDateLate(dto.getMaxDateLate())
                .autoRenew(dto.getAutoRenew())
                .terms(terms)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return templateRepository.save(template);
    }

    public List<ContractTemplate> getAllTemplates() {
        return templateRepository.findAll();
    }

    public Optional<ContractTemplate> getTemplateById(Long id) {
        return templateRepository.findById(id);
    }

    public void deleteTemplate(Long id) {
        templateRepository.deleteById(id);
    }


}

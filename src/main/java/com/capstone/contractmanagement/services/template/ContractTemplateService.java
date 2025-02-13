package com.capstone.contractmanagement.services.template;

import com.capstone.contractmanagement.dtos.IdDTO;
import com.capstone.contractmanagement.dtos.template.ContractTemplateDTO;
import com.capstone.contractmanagement.entities.ContractTemplate;
import com.capstone.contractmanagement.entities.Term;
import com.capstone.contractmanagement.repositories.IContractTemplateRepository;
import com.capstone.contractmanagement.repositories.ITermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ContractTemplateService implements IContractTemplateService {
    private final IContractTemplateRepository templateRepository;
    private final ITermRepository termRepository;

    @Override
    public ContractTemplate createTemplate(ContractTemplateDTO dto) {
        // Set để tránh trùng lặp term id
        Set<Long> termIdSet = new HashSet<>();

        // term từ legalBasis (là danh sách các type term id)
        if (dto.getLegalBasis() != null) {
            for (IdDTO legal : dto.getLegalBasis()) {
                List<Term> termsByType = termRepository.findByTypeTermId(legal.getId());
                termsByType.forEach(t -> termIdSet.add(t.getId()));
            }
        }

        // term từ generalTerms (mảng type term id)
        if (dto.getGeneralTerms() != null) {
            for (Long typeTermId : dto.getGeneralTerms()) {
                List<Term> termsByType = termRepository.findByTypeTermId(typeTermId);
                termsByType.forEach(t -> termIdSet.add(t.getId()));
            }
        }

        // term từ additionalTerms (mảng type term id)
        if (dto.getAdditionalTerms() != null) {
            for (Long typeTermId : dto.getAdditionalTerms()) {
                List<Term> termsByType = termRepository.findByTypeTermId(typeTermId);
                termsByType.forEach(t -> termIdSet.add(t.getId()));
            }
        }

        //  term từ cấu hình chi tiết trong additionalConfig
        if (dto.getAdditionalConfig() != null) {
            // additionalConfig có kiểu Map<String, Map<String, List<IdDTO>>>
            // mỗi entry: key là định danh của type term, value là chi tiết
            for (Map<String, List<IdDTO>> groupConfig : dto.getAdditionalConfig().values()) {
                for (List<IdDTO> termGroup : groupConfig.values()) {
                    termGroup.forEach(idDTO -> termIdSet.add(idDTO.getId()));
                }
            }
        }

        // term từ specialTermsA và specialTermsB (nếu có)
        if (dto.getSpecialTermsA() != null) {
            termIdSet.add(dto.getSpecialTermsA());
        }
        if (dto.getSpecialTermsB() != null) {
            termIdSet.add(dto.getSpecialTermsB());
        }

        // danh sách term từ DB theo các id đã thu thập
        List<Long> termIds = new ArrayList<>(termIdSet);
        List<Term> terms = termRepository.findAllById(termIds);

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

    @Override
    public List<ContractTemplate> getAllTemplates() {
        return templateRepository.findAll();
    }

    @Override
    public Optional<ContractTemplate> getTemplateById(Long id) {
        return templateRepository.findById(id);
    }

    @Override
    public void deleteTemplate(Long id) {
        templateRepository.deleteById(id);
    }
}

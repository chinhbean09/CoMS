    package com.capstone.contractmanagement.services.template;

    import com.capstone.contractmanagement.dtos.IdDTO;
    import com.capstone.contractmanagement.dtos.template.ContractTemplateDTO;
    import com.capstone.contractmanagement.entities.ContractTemplate;
    import com.capstone.contractmanagement.entities.ContractTemplateAdditionalTermDetail;
    import com.capstone.contractmanagement.entities.Term;
    import com.capstone.contractmanagement.repositories.IContractTemplateRepository;
    import com.capstone.contractmanagement.repositories.ITermRepository;
    import lombok.RequiredArgsConstructor;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;
    import java.time.LocalDateTime;
    import java.util.*;

    @Service
    @RequiredArgsConstructor
    public class ContractTemplateService implements IContractTemplateService {
        private final IContractTemplateRepository templateRepository;
        private final ITermRepository termRepository;

        @Override
        @Transactional
        public ContractTemplate createTemplate(ContractTemplateDTO dto) {
            // Khai báo các tập hợp id cho từng nhóm
            Set<Long> legalBasisIds = new HashSet<>();
            Set<Long> generalTermsIds = new HashSet<>();
            Set<Long> otherTermsIds = new HashSet<>();
            Set<Long> additionalTermsIds = new HashSet<>();

            // Legal Basis
            if (dto.getLegalBasis() != null) {
                dto.getLegalBasis().forEach(idDTO -> legalBasisIds.add(idDTO.getId()));
            }
            // General Terms
            if (dto.getGeneralTerms() != null) {
                generalTermsIds.addAll(dto.getGeneralTerms());
            }
            // Other Terms
            if (dto.getOtherTerms() != null) {
                otherTermsIds.addAll(dto.getOtherTerms());
            }
            // Additional Terms
            if (dto.getAdditionalTerms() != null) {
                additionalTermsIds.addAll(dto.getAdditionalTerms());
            }
            // Special Terms

            Set<Long> termIdSet = new HashSet<>();
            termIdSet.addAll(legalBasisIds);
            termIdSet.addAll(generalTermsIds);
            termIdSet.addAll(otherTermsIds);
            // các term từ additionalConfig sẽ được thêm sau

            // xử lý additionalConfig để tạo cấu hình chi tiết cho nhóm Additional
            List<ContractTemplateAdditionalTermDetail> additionalTermConfigs = new ArrayList<>();
            if (dto.getAdditionalConfig() != null) {
                // additionalConfig: Map<String, Map<String, List<IdDTO>>>
                for (Map.Entry<String, Map<String, List<IdDTO>>> entry : dto.getAdditionalConfig().entrySet()) {
                    String key = entry.getKey();
                    Long configTypeTermId;
                    try {
                        configTypeTermId = Long.parseLong(key);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Key trong additionalConfig phải là số đại diện cho type term id. Key sai: " + key);
                    }
                    Map<String, List<IdDTO>> groupConfig = entry.getValue();

                    // danh sách term id cho từng nhóm con
                    List<Long> commonTermIds = groupConfig.containsKey("Common")
                            ? groupConfig.get("Common").stream().map(IdDTO::getId).toList()
                            : new ArrayList<>();
                    List<Long> aTermIds = groupConfig.containsKey("A")
                            ? groupConfig.get("A").stream().map(IdDTO::getId).toList()
                            : new ArrayList<>();
                    List<Long> bTermIds = groupConfig.containsKey("B")
                            ? groupConfig.get("B").stream().map(IdDTO::getId).toList()
                            : new ArrayList<>();

                    // không cho cùng 1 term xuất hiện ở nhiều nhóm con
                    Set<Long> unionCommonA = new HashSet<>(commonTermIds);
                    unionCommonA.retainAll(aTermIds);
                    if (!unionCommonA.isEmpty()) {
                        throw new IllegalArgumentException("Các term " + unionCommonA
                                + " không được chọn đồng thời ở 'Common' và 'A' cho type term id " + configTypeTermId);
                    }
                    Set<Long> unionCommonB = new HashSet<>(commonTermIds);
                    unionCommonB.retainAll(bTermIds);
                    if (!unionCommonB.isEmpty()) {
                        throw new IllegalArgumentException("Các term " + unionCommonB
                                + " không được chọn đồng thời ở 'Common' và 'B' cho type term id " + configTypeTermId);
                    }
                    Set<Long> unionAB = new HashSet<>(aTermIds);
                    unionAB.retainAll(bTermIds);
                    if (!unionAB.isEmpty()) {
                        throw new IllegalArgumentException("Các term " + unionAB
                                + " không được chọn đồng thời ở 'A' và 'B' cho type term id " + configTypeTermId);
                    }

                    // các term trong cấu hình này thuộc đúng type term
                    for (Long termId : commonTermIds) {
                        Term term = termRepository.findById(termId)
                                .orElseThrow(() -> new IllegalArgumentException("Term không tồn tại: " + termId));
                        if (!term.getTypeTerm().getId().equals(configTypeTermId)) {
                            throw new IllegalArgumentException("Term " + termId
                                    + " không thuộc type term id " + configTypeTermId);
                        }
                    }
                    for (Long termId : aTermIds) {
                        Term term = termRepository.findById(termId)
                                .orElseThrow(() -> new IllegalArgumentException("Term không tồn tại: " + termId));
                        if (!term.getTypeTerm().getId().equals(configTypeTermId)) {
                            throw new IllegalArgumentException("Term " + termId
                                    + " không thuộc type term id " + configTypeTermId);
                        }
                    }
                    for (Long termId : bTermIds) {
                        Term term = termRepository.findById(termId)
                                .orElseThrow(() -> new IllegalArgumentException("Term không tồn tại: " + termId));
                        if (!term.getTypeTerm().getId().equals(configTypeTermId)) {
                            throw new IllegalArgumentException("Term " + termId
                                    + " không thuộc type term id " + configTypeTermId);
                        }
                    }

                    ContractTemplateAdditionalTermDetail configRecord = ContractTemplateAdditionalTermDetail.builder()
                            .typeTermId(configTypeTermId)
                            .commonTermIds(commonTermIds)
                            .aTermIds(aTermIds)
                            .bTermIds(bTermIds)
                            .build();
                    additionalTermConfigs.add(configRecord);

                    termIdSet.addAll(commonTermIds);
                    termIdSet.addAll(aTermIds);
                    termIdSet.addAll(bTermIds);
                }
            }

            List<Long> allTermIds = new ArrayList<>(termIdSet);
            List<Term> allTerms = termRepository.findAllById(allTermIds);

            // ContractTemplate
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
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            if (!legalBasisIds.isEmpty()) {
                List<Term> legalBasisTerms = termRepository.findAllById(legalBasisIds);
                template.setLegalBasisTerms(legalBasisTerms);
            }
            if (!generalTermsIds.isEmpty()) {
                List<Term> generalTerms = termRepository.findAllById(generalTermsIds);
                template.setGeneralTerms(generalTerms);
            }
            if (!otherTermsIds.isEmpty()) {
                List<Term> otherTerms = termRepository.findAllById(otherTermsIds);
                template.setOtherTerms(otherTerms);
            }
            if (!additionalTermsIds.isEmpty()) {
                List<Term> additionalTerms = termRepository.findAllById(additionalTermsIds);
                template.setAdditionalTerms(additionalTerms);
            }

            template.setAdditionalTermConfigs(additionalTermConfigs);
            additionalTermConfigs.forEach(config -> config.setContractTemplate(template));

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

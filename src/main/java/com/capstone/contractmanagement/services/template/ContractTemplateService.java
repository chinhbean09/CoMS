    package com.capstone.contractmanagement.services.template;

    import com.capstone.contractmanagement.dtos.IdDTO;
    import com.capstone.contractmanagement.dtos.template.ContractTemplateDTO;
    import com.capstone.contractmanagement.entities.ContractTemplate;
    import com.capstone.contractmanagement.entities.ContractTemplateAdditionalTermDetail;
    import com.capstone.contractmanagement.entities.ContractType;
    import com.capstone.contractmanagement.entities.Term;
    import com.capstone.contractmanagement.enums.TypeTermIdentifier;
    import com.capstone.contractmanagement.exceptions.DataNotFoundException;
    import com.capstone.contractmanagement.repositories.IContractTemplateRepository;
    import com.capstone.contractmanagement.repositories.IContractTypeRepository;
    import com.capstone.contractmanagement.repositories.ITermRepository;
    import lombok.RequiredArgsConstructor;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.Pageable;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;
    import java.time.LocalDateTime;
    import java.util.*;
    import java.util.stream.Collectors;

    @Service
    @RequiredArgsConstructor
    public class ContractTemplateService implements IContractTemplateService {
        private final IContractTemplateRepository templateRepository;
        private final ITermRepository termRepository;
        private final IContractTypeRepository contractTypeRepository;

        @Override
        @Transactional
        public ContractTemplate createTemplate(ContractTemplateDTO dto) throws DataNotFoundException {

            ContractType contractType = contractTypeRepository.findById(dto.getContractTypeId())
                    .orElseThrow(() -> new DataNotFoundException("Không tìm thấy loại hợp đồng với id: " + dto.getContractTypeId()));

            // Khai báo các tập hợp id cho từng nhóm
            Set<Long> legalBasisIds = new HashSet<>();
            Set<Long> generalTermsIds = new HashSet<>();
            Set<Long> otherTermsIds = new HashSet<>();
            Set<Long> additionalTermsIds = new HashSet<>();

            // Legal Basis
            if (dto.getLegalBasis() != null) {
                legalBasisIds.addAll(dto.getLegalBasis());
                for (Long id : legalBasisIds) {
                    Term term = termRepository.findById(id)
                            .orElseThrow(() -> new IllegalArgumentException("Không tồn tại điều khoản với id: " + id));
                    if (!term.getTypeTerm().getIdentifier().equals(TypeTermIdentifier.LEGAL_BASIS)) {
                        throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại Căn cứ pháp lí (LEGAL_BASIS)");
                    }
                }
            }

            // General Terms
            if (dto.getGeneralTerms() != null) {
                generalTermsIds.addAll(dto.getGeneralTerms());
                for (Long id : generalTermsIds) {
                    Term term = termRepository.findById(id)
                            .orElseThrow(() -> new IllegalArgumentException("Không tồn tại điều khoản với id: " + id));
                    if (!term.getTypeTerm().getIdentifier().equals(TypeTermIdentifier.GENERAL_TERMS)) {
                        throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại Các điều khoản khác (GENERAL_TERMS)");
                    }
                }
            }
            // Other Terms
            if (dto.getOtherTerms() != null) {
                otherTermsIds.addAll(dto.getOtherTerms());
                for (Long id : otherTermsIds) {
                    Term term = termRepository.findById(id)
                            .orElseThrow(() -> new IllegalArgumentException("Không tồn tại điều khoản với id: " + id));
                    if (!term.getTypeTerm().getIdentifier().equals(TypeTermIdentifier.OTHER_TERMS)) {
                        throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại Các điều khoản khác (OTHER_TERMS)");
                    }
                }
            }

            // Additional Terms, type term khong can phai luu
            if (dto.getAdditionalTerms() != null) {
                additionalTermsIds.addAll(dto.getAdditionalTerms());
            }

            //luu 3 term con lai
            Set<Long> termIdSet = new HashSet<>();
            termIdSet.addAll(legalBasisIds);
            termIdSet.addAll(generalTermsIds);
            termIdSet.addAll(otherTermsIds);
            // các term từ additionalConfig sẽ được thêm sau

            // xử lý additionalConfig để tạo cấu hình chi tiết cho nhóm Additional
            //term cuoi cung can phai xu ly truoc khi luu
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

                        //convert id thành tên điều khoản
                        List<String> conflictTerms = unionCommonA.stream()
                                .map(id -> termRepository.findById(id)
                                        .map(Term::getLabel)
                                        .orElse(String.valueOf(id)))
                                .toList();

                        // Lấy tên loại điều khoản từ một term trong commonTermIds
                        String typeName = termRepository.findById(commonTermIds.iterator().next())
                                .map(t -> t.getTypeTerm().getName())
                                .orElse(String.valueOf(configTypeTermId));
                        throw new IllegalArgumentException("Các điều khoản " + conflictTerms
                                + " không được chọn đồng thời ở 'Common' và 'A' cho loại điều khoản:" + typeName);
                    }

                    Set<Long> unionCommonB = new HashSet<>(commonTermIds);
                    unionCommonB.retainAll(bTermIds);
                    if (!unionCommonB.isEmpty()) {

                        List<String> conflictTerms = unionCommonB.stream()
                                .map(id -> termRepository.findById(id)
                                        .map(Term::getLabel)
                                        .orElse(String.valueOf(id)))
                                .toList();

                        String typeName = termRepository.findById(commonTermIds.iterator().next())
                                .map(t -> t.getTypeTerm().getName())
                                .orElse(String.valueOf(configTypeTermId));
                        throw new IllegalArgumentException("Các điều khoản " + conflictTerms
                                + " không được chọn đồng thời ở 'Common' và 'B' cho loại điều khoản: " + typeName);
                    }

                    Set<Long> unionAB = new HashSet<>(aTermIds);
                    unionAB.retainAll(bTermIds);
                    if (!unionAB.isEmpty()) {
                        List<String> conflictTerms = unionAB.stream()
                                .map(id -> termRepository.findById(id)
                                        .map(Term::getLabel)
                                        .orElse(String.valueOf(id)))
                                .collect(Collectors.toList());
                        String typeName = termRepository.findById(aTermIds.iterator().next())
                                .map(t -> t.getTypeTerm().getName())
                                .orElse(String.valueOf(configTypeTermId));
                        throw new IllegalArgumentException("Các điều khoản " + conflictTerms
                                + " không được chọn đồng thời ở 'A' và 'B' cho loại điều khoản: " + typeName);
                    }

                    // các term trong cấu hình này thuộc đúng type term
                    for (Long termId : commonTermIds) {
                        Term term = termRepository.findById(termId)
                                .orElseThrow(() -> new IllegalArgumentException("Không tồn tại điều khoản: " + termId));
                        if (!term.getTypeTerm().getId().equals(configTypeTermId)) {
                            throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại điều khoản: \""
                                    + term.getTypeTerm().getName() + "\"");
                        }
                    }
                    for (Long termId : aTermIds) {
                        Term term = termRepository.findById(termId)
                                .orElseThrow(() -> new IllegalArgumentException("Không tồn tại điều khoản: " + termId));
                        if (!term.getTypeTerm().getId().equals(configTypeTermId)) {
                            throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại điều khoản: \""
                                    + term.getTypeTerm().getName() + "\"");
                        }
                    }
                    for (Long termId : bTermIds) {
                        Term term = termRepository.findById(termId)
                                .orElseThrow(() -> new IllegalArgumentException("Không tồn tại điều khoản: " + termId));
                        if (!term.getTypeTerm().getId().equals(configTypeTermId)) {
                            throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại điều khoản: \""
                                    + term.getTypeTerm().getName() + "\"");
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
                    .contractType(contractType)
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
        public Page<ContractTemplate> getAllTemplates(Pageable pageable) {
            return templateRepository.findAll(pageable);
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

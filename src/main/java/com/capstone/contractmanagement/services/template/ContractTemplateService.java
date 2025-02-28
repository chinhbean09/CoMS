    package com.capstone.contractmanagement.services.template;

    import com.capstone.contractmanagement.dtos.IdDTO;
    import com.capstone.contractmanagement.dtos.template.ContractTemplateDTO;
    import com.capstone.contractmanagement.dtos.term.TermSimpleDTO;
    import com.capstone.contractmanagement.entities.ContractTemplate;
    import com.capstone.contractmanagement.entities.ContractTemplateAdditionalTermDetail;
    import com.capstone.contractmanagement.entities.ContractType;
    import com.capstone.contractmanagement.entities.Term;
    import com.capstone.contractmanagement.enums.TypeTermIdentifier;
    import com.capstone.contractmanagement.exceptions.DataNotFoundException;
    import com.capstone.contractmanagement.repositories.IContractTemplateRepository;
    import com.capstone.contractmanagement.repositories.IContractTypeRepository;
    import com.capstone.contractmanagement.repositories.ITermRepository;
    import com.capstone.contractmanagement.repositories.ITypeTermRepository;
    import com.capstone.contractmanagement.responses.template.*;
    import com.capstone.contractmanagement.responses.term.TermResponse;
    import com.capstone.contractmanagement.responses.term.TypeTermResponse;
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
        private final ITypeTermRepository typeTermRepository;


        @Override
        @Transactional
        public ContractTemplate createTemplate(ContractTemplateDTO dto) throws DataNotFoundException {

            ContractType contractType = contractTypeRepository.findById(dto.getContractTypeId())
                    .orElseThrow(() -> new DataNotFoundException("Không tìm thấy loại hợp đồng với id: " + dto.getContractTypeId()));

            if (templateRepository.existsByContractTitle(dto.getContractTitle())) {
                throw new IllegalArgumentException("Tiêu đề hợp đồng đã tồn tại: " + dto.getContractTitle());
            }

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
            template.setAdditionalTermConfigs(additionalTermConfigs);
            additionalTermConfigs.forEach(config -> config.setContractTemplate(template));

            return templateRepository.save(template);
        }
        @Override
        @Transactional(readOnly = true)
        public Page<ContractTemplateSimpleResponse> getAllTemplates(Pageable pageable, String keyword) {
            boolean hasSearch = keyword != null && !keyword.trim().isEmpty();
            Page<ContractTemplate> templates;

            if (hasSearch) {
                keyword = keyword.trim(); // Loại bỏ khoảng trắng dư thừa
                templates = templateRepository.findByContractTitleContainingIgnoreCase(keyword, pageable);
            } else {
                templates = templateRepository.findAll(pageable);
            }

            return templates.map(this::convertToSimpleResponseDTO);
        }


        private ContractTemplateSimpleResponse convertToSimpleResponseDTO(ContractTemplate template) {
            return ContractTemplateSimpleResponse.builder()
                    .id(template.getId())
                    .contractTitle(template.getContractTitle())
                    .partyInfo(template.getPartyInfo())
                    .specialTermsA(template.getSpecialTermsA())
                    .specialTermsB(template.getSpecialTermsB())
                    .appendixEnabled(template.getAppendixEnabled())
                    .transferEnabled(template.getTransferEnabled())
                    .createdAt(template.getCreatedAt())
                    .updatedAt(template.getUpdatedAt())
                    .violate(template.getViolate())
                    .suspend(template.getSuspend())
                    .suspendContent(template.getSuspendContent())
                    .contractContent(template.getContractContent())
                    .autoAddVAT(template.getAutoAddVAT())
                    .vatPercentage(template.getVatPercentage())
                    .isDateLateChecked(template.getIsDateLateChecked())
                    .maxDateLate(template.getMaxDateLate())
                    .autoRenew(template.getAutoRenew())
                    .contractType(ContractType.builder()
                            .id(template.getContractType().getId())
                            .name(template.getContractType().getName())
                            .build())
                    .build();
        }



        @Override
        @Transactional(readOnly = true)
        public Optional<ContractTemplateResponse> getTemplateById(Long id) {
            return templateRepository.findById(id)
                    .map(template -> {
                        // Force lazy loading of collections while session is open.
                        template.getLegalBasisTerms().size();
                        template.getGeneralTerms().size();
                        template.getOtherTerms().size();
                        // No additionalTerms list is used now.
                        template.getAdditionalTermConfigs().forEach(config -> {
                            config.getCommonTermIds().size();
                            config.getATermIds().size();
                            config.getBTermIds().size();
                        });
                        return convertToResponseDTO(template);
                    });
        }

        @Override
        @Transactional(readOnly = true)
        public List<ContractTemplateTitleResponse> getAllTemplateTitles() {
            return templateRepository.findAll().stream()
                    .map(template -> ContractTemplateTitleResponse.builder()
                            .id(template.getId())
                            .contractTitle(template.getContractTitle())
                            .build())
                    .collect(Collectors.toList());
        }




        private ContractTemplateResponse convertToResponseDTO(ContractTemplate template) {
            List<TermResponse> legalBasisTerms = template.getLegalBasisTerms().stream()
                    .map(term -> TermResponse.builder()
                            .id(term.getId())
                            .label(term.getLabel())
                            .value(term.getValue())
                            .build())
                    .collect(Collectors.toList());

            List<TermResponse> generalTerms = template.getGeneralTerms().stream()
                    //biến đổi từng phần tử của stream từ kiểu dữ liệu này sang kiểu dữ liệu khác.
                    .map(term -> TermResponse.builder()
                            .id(term.getId())
                            .label(term.getLabel())
                            .value(term.getValue())
                            .build())
                    .collect(Collectors.toList());

            List<TermResponse> otherTerms = template.getOtherTerms().stream()
                    .map(term -> TermResponse.builder()
                            .id(term.getId())
                            .label(term.getLabel())
                            .value(term.getValue())
                            .build())
                    .collect(Collectors.toList());

//            List<TermResponse> additionalTerms = template.getAdditionalTerms().stream()
//                    .map(term -> TermResponse.builder()
//                            .id(term.getId())
//                            .label(term.getLabel())
//                            .value(term.getValue())
//                            .build())
//                    .collect(Collectors.toList());

            List<TypeTermResponse> additionalTerms = template.getAdditionalTermConfigs().stream()
                    .map(config -> typeTermRepository.findById(config.getTypeTermId()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(typeTerm -> TypeTermResponse.builder()
                            .id(typeTerm.getId())
                            .name(typeTerm.getName())
                            .identifier(typeTerm.getIdentifier())
                            .build())
                    .distinct()  // ensure uniqueness if needed
                    .collect(Collectors.toList());

            Map<String, Map<String, List<TermResponse>>> additionalConfig = template.getAdditionalTermConfigs()

                    //gọi .stream(), Java sẽ tự động lặp qua từng phần tử của danh sách
                    //chỉ cần định nghĩa cách xử lý từng phần tử (biến đổi thành key và value)
                    .stream()
                    //tạo ra map => Collectors.toMap
                    //Key là typeTermId, value là một Map bao gồm 3 phần: common, A và B.
                    .collect(Collectors.toMap(
                            //Một hàm để tạo key cho mỗi entry.
                            //Với mỗi config, lấy config.getTypeTermId(), chuyển thành chuỗi và dùng làm key.
                            config -> String.valueOf(config.getTypeTermId()),

                            //Một hàm để tạo value cho mỗi entry.
                            config -> {
                                // khởi tạo inner map phục vụ cho value của key
                                Map<String, List<TermResponse>> innerMap = new java.util.HashMap<>();
                                //Gán key "Common"̀ ; value: gán danh sách TermResponse được tạo từ config.getCommonTermIds().
                                innerMap.put("Common", convertTermIdsToTermResponseDTOList(config.getCommonTermIds()));
                                innerMap.put("A", convertTermIdsToTermResponseDTOList(config.getATermIds())); // Ensure getter naming is correct
                                innerMap.put("B", convertTermIdsToTermResponseDTOList(config.getBTermIds())); // Ensure getter naming is correct
                                return innerMap;
                            }
                    ));

            return ContractTemplateResponse.builder()
                    .id(template.getId())
                    .contractTitle(template.getContractTitle())
                    .partyInfo(template.getPartyInfo())
                    .specialTermsA(template.getSpecialTermsA())
                    .specialTermsB(template.getSpecialTermsB())
                    .appendixEnabled(template.getAppendixEnabled())
                    .transferEnabled(template.getTransferEnabled())
                    .createdAt(template.getCreatedAt())
                    .updatedAt(template.getUpdatedAt())
                    .violate(template.getViolate())
                    .suspend(template.getSuspend())
                    .suspendContent(template.getSuspendContent())
                    .contractContent(template.getContractContent())
                    .autoAddVAT(template.getAutoAddVAT())
                    .vatPercentage(template.getVatPercentage())
                    .isDateLateChecked(template.getIsDateLateChecked())
                    .maxDateLate(template.getMaxDateLate())
                    .autoRenew(template.getAutoRenew())
                    .legalBasisTerms(legalBasisTerms)
                    .generalTerms(generalTerms)
                    .otherTerms(otherTerms)
                    .additionalTerms(additionalTerms)
                    .contractTypeId(template.getContractType() != null ? template.getContractType().getId() : null)
                    .additionalConfig(additionalConfig)
                    .originalTemplateId(template.getOriginalTemplateId())
                    .duplicateVersion(template.getDuplicateVersion())
                    .build();
        }

        //Biến đổi từng phần tử trong danh sách termIds (Long) thành các đối tượng TermResponse.
        private List<TermResponse> convertTermIdsToTermResponseDTOList(List<Long> termIds) {
            return termIds
                    //chuyển danh sách thành stream
                    .stream()

                    //chuyển mỗi phần tử id thành một đối tượng TermResponse
                    //Mỗi phần tử id của stream sẽ được truyền vào lambda thực hiện lambda id -> { ... }.
                    .map(id ->

                            //với mỗi id, gọi termRepository.findById(id) để tìm đối tượng Term tương ứng.
                            termRepository.findById(id)

                    //Phương thức map nhận đối tượng Term và áp dụng lambda: term -> TermResponse.builder()
                    //Lambda term -> TermResponse.builder()...build(): "với đối tượng term, tạo ra một đối tượng TermResponse dựa trên các thông tin của term."
                    .map(term -> TermResponse.builder()
                                    .id(term.getId())
                                    .label(term.getLabel())
                                    .value(term.getValue())
                                    .build())
                            .orElse(null))
                    //chỉ giữ lại các TermResponse hợp lệ.
                    .filter(dto -> dto != null)
                    //tất cả các đối tượng TermResponse được thu thập lại thành một danh sách (List<TermResponse>) và trả về.
                    .collect(Collectors.toList());
        }


        @Override
        public void deleteTemplate(Long id) {
            templateRepository.deleteById(id);
        }
        @Override
        @Transactional(readOnly = true)
        public Optional<ContractTemplateResponseIds> getTemplateIdsById(Long id) {
            return templateRepository.findById(id)
                    .map(template -> {
                        template.getLegalBasisTerms().size();
                        template.getGeneralTerms().size();
                        template.getOtherTerms().size();
                        template.getAdditionalTermConfigs().forEach(config -> {
                            config.getCommonTermIds().size();
                            config.getATermIds().size();
                            config.getBTermIds().size();
                        });
                        return convertToIdResponseDTO(template);
                    });
        }


        private ContractTemplateResponseIds convertToIdResponseDTO(ContractTemplate template) {
            List<Long> legalBasisTermIds = template.getLegalBasisTerms().stream()
                    .map(Term::getId)
                    .collect(Collectors.toList());

            List<Long> generalTermIds = template.getGeneralTerms().stream()
                    .map(Term::getId)
                    .collect(Collectors.toList());

            List<Long> otherTermIds = template.getOtherTerms().stream()
                    .map(Term::getId)
                    .collect(Collectors.toList());

            List<Long> additionalTermIds = template.getAdditionalTermConfigs().stream()
                    .map(ContractTemplateAdditionalTermDetail::getTypeTermId)
                    .distinct()
                    .collect(Collectors.toList());

            Map<String, Map<String, List<Long>>> additionalConfig = template.getAdditionalTermConfigs().stream()
                    .collect(Collectors.toMap(
                            config -> String.valueOf(config.getTypeTermId()),
                            config -> {
                                Map<String, List<Long>> innerMap = new HashMap<>();
                                innerMap.put("Common", config.getCommonTermIds());
                                innerMap.put("A", config.getATermIds());
                                innerMap.put("B", config.getBTermIds());
                                return innerMap;
                            }
                    ));

            return ContractTemplateResponseIds.builder()
                    .id(template.getId())
                    .legalBasisTermIds(legalBasisTermIds)
                    .generalTermIds(generalTermIds)
                    .otherTermIds(otherTermIds)
                    .additionalTermIds(additionalTermIds)
                    .additionalConfig(additionalConfig)
                    .build();
        }

        @Override
        @Transactional
        public Optional<ContractTemplateResponse> duplicateTemplate(Long id) {
            return templateRepository.findById(id)
                    .map(originalTemplate -> {

                        ContractTemplate duplicate = new ContractTemplate();
                        int duplicateCount = templateRepository.countByOriginalTemplateId(originalTemplate.getId());
                        duplicate.setDuplicateVersion(duplicateCount + 1);
                        duplicate.setOriginalTemplateId(originalTemplate.getId());

                        // Cập nhật tiêu đề: thêm dấu hiệu copy và số thứ tự duplicate
                        String newTitle = originalTemplate.getContractTitle()
                                + " (Copy " + (duplicateCount + 1) + ")";
                        duplicate.setContractTitle(newTitle);
                        duplicate.setPartyInfo(originalTemplate.getPartyInfo());
                        duplicate.setSpecialTermsA(originalTemplate.getSpecialTermsA());
                        duplicate.setSpecialTermsB(originalTemplate.getSpecialTermsB());
                        duplicate.setAppendixEnabled(originalTemplate.getAppendixEnabled());
                        duplicate.setTransferEnabled(originalTemplate.getTransferEnabled());
                        duplicate.setViolate(originalTemplate.getViolate());
                        duplicate.setSuspend(originalTemplate.getSuspend());
                        duplicate.setSuspendContent(originalTemplate.getSuspendContent());
                        duplicate.setContractContent(originalTemplate.getContractContent());
                        duplicate.setAutoAddVAT(originalTemplate.getAutoAddVAT());
                        duplicate.setVatPercentage(originalTemplate.getVatPercentage());
                        duplicate.setIsDateLateChecked(originalTemplate.getIsDateLateChecked());
                        duplicate.setMaxDateLate(originalTemplate.getMaxDateLate());
                        duplicate.setAutoRenew(originalTemplate.getAutoRenew());
                        duplicate.setContractType(originalTemplate.getContractType());

                        // Copy các danh sách liên quan (nếu cần duplicate sâu)
                        duplicate.setLegalBasisTerms(new ArrayList<>(originalTemplate.getLegalBasisTerms()));
                        duplicate.setGeneralTerms(new ArrayList<>(originalTemplate.getGeneralTerms()));
                        duplicate.setOtherTerms(new ArrayList<>(originalTemplate.getOtherTerms()));

                        // Đặt lại thời gian tạo, cập nhật
                        duplicate.setCreatedAt(LocalDateTime.now());
                        duplicate.setUpdatedAt(LocalDateTime.now());

                        // Set thông tin version:
                        // Gán originalTemplateId = id của template gốc
                        duplicate.setOriginalTemplateId(originalTemplate.getId());
                        // Đếm số bản duplicate đã tạo từ template gốc

                        // Lưu đối tượng duplicate vào database
                        ContractTemplate savedDuplicate = templateRepository.save(duplicate);

                        // Duplicate các bản ghi trong bảng contract_template_additional_term_details
                        List<ContractTemplateAdditionalTermDetail> duplicatedDetails = new ArrayList<>();
                        for (ContractTemplateAdditionalTermDetail detail : originalTemplate.getAdditionalTermConfigs()) {
                            ContractTemplateAdditionalTermDetail newDetail = ContractTemplateAdditionalTermDetail.builder()
                                    // Liên kết với duplicate mới (foreign key template_id)
                                    .contractTemplate(savedDuplicate)
                                    .typeTermId(detail.getTypeTermId())
                                    // Copy danh sách term id theo từng nhóm
                                    .commonTermIds(new ArrayList<>(detail.getCommonTermIds()))
                                    .aTermIds(new ArrayList<>(detail.getATermIds()))
                                    .bTermIds(new ArrayList<>(detail.getBTermIds()))
                                    .build();
                            duplicatedDetails.add(newDetail);
                        }

                        // Nếu cascade persist đã được cấu hình thì chỉ cần gán danh sách mới vào savedDuplicate.
                        // Nếu không, bạn cần lưu từng bản ghi detail riêng thông qua repository.
                        savedDuplicate.setAdditionalTermConfigs(duplicatedDetails);
                        // Nếu không có cascade, uncomment dòng dưới đây và inject additionalTermDetailRepository:
                        // duplicatedDetails.forEach(additionalTermDetailRepository::save);

                        savedDuplicate = templateRepository.save(savedDuplicate);

                        savedDuplicate.getLegalBasisTerms().size();
                        savedDuplicate.getGeneralTerms().size();
                        savedDuplicate.getOtherTerms().size();
                        savedDuplicate.getAdditionalTermConfigs().size();

                        return convertToResponseDTO(savedDuplicate);
                    });
        }


    }

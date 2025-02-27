package com.capstone.contractmanagement.services.contract;

import com.capstone.contractmanagement.components.SecurityUtils;
import com.capstone.contractmanagement.dtos.contract.ContractDTO;
import com.capstone.contractmanagement.entities.*;
import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.enums.TermGroup;
import com.capstone.contractmanagement.enums.TypeTermIdentifier;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.*;
import com.capstone.contractmanagement.responses.contract.AdditionalTermDetailResponse;
import com.capstone.contractmanagement.responses.contract.AdditionalTermResponse;
import com.capstone.contractmanagement.responses.contract.ContractResponse;
import com.capstone.contractmanagement.responses.contract.ContractTermResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractService implements IContractService{


    private final IContractRepository contractRepository;
    private final IContractTemplateRepository contractTemplateRepository;
    private final IUserRepository userRepository;
    private final IPartyRepository partyRepository;
    private final ITermRepository termRepository;
    private final SecurityUtils currentUser;

    @Override
    public List<ContractResponse> getAllContracts() {
        return contractRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private void validateParty(Party party) {
        if (party.getPartnerName() == null || party.getPartnerName().isEmpty()) {
            throw new IllegalArgumentException("Party must have a valid name");
        }
        if (party.getAddress() == null || party.getAddress().isEmpty()) {
            throw new IllegalArgumentException("Party must have a valid address");
        }
    }
//    public Contract createContractFromTemplate(Long templateId, ContractDTO dto) throws DataNotFoundException {
//        // Fetch Template với tất cả quan hệ cần thiết
//        ContractTemplate template = contractTemplateRepository.findWithTermsById(templateId)
//                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy mẫu hợp đồng với id: " + templateId));
//
//        // Tạo Contract mới và sao chép thông tin từ Template
//        Contract contract = new Contract();
//        contract.setTitle(template.getContractTitle());
//        contract.setSpecialTermsA(template.getSpecialTermsA());
//        contract.setSpecialTermsB(template.getSpecialTermsB());
//        contract.setContractContent(template.getContractContent());
//        contract.setAppendixEnabled(template.getAppendixEnabled());
//        contract.setTransferEnabled(template.getTransferEnabled());
//        contract.setAutoAddVAT(template.getAutoAddVAT());
//        contract.setVatPercentage(template.getVatPercentage());
//        contract.setAutoRenew(template.getAutoRenew());
//
//        contract.setContractNumber(dto.getContractNumber());
//        contract.setStatus(dto.getStatus());
//
//        contract.setStartDate(LocalDateTime.now());
//
//        Set<Term> allTerms = new HashSet<>();
//
//        allTerms.addAll(template.getLegalBasisTerms());
//        allTerms.addAll(template.getGeneralTerms());
//        allTerms.addAll(template.getOtherTerms());
//
//        // Thu thập các Term từ additionalTermConfigs
//        Set<Long> additionalTermIds = new HashSet<>();
//        for (ContractTemplateAdditionalTermDetail config : template.getAdditionalTermConfigs()) {
//            additionalTermIds.addAll(config.getCommonTermIds());
//            additionalTermIds.addAll(config.getATermIds());
//            additionalTermIds.addAll(config.getBTermIds());
//        }
//        List<Term> additionalTerms = termRepository.findAllById(additionalTermIds);
//        allTerms.addAll(additionalTerms);
//
//        // Gán danh sách Term vào Contract
//        contract.setTerms(new ArrayList<>(allTerms));
//
//        User user = currentUser.getLoggedInUser();
//
//        // Liên kết Contract với Template và các entity khác
//        contract.setTemplate(template);
//        contract.setUser(user); // Giả sử currentUser được lấy từ security context
//        contract.setParty(partyRepository.findById(dto.getPartyId()) // Giả sử partyId từ DTO
//                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy đối tác")));
//
//        // Lưu Contract
//        return contractRepository.save(contract);
//    }
//

    @Override
    public ContractResponse updateContract(Long id, ContractDTO contractDTO) {
        Contract existingContract = contractRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found with id: " + id));

        existingContract.setTitle(contractDTO.getTitle());
        existingContract.setStatus(contractDTO.getStatus());
//        existingContract.setUpdatedAt(contractDTO.getUpdatedAt());

        contractRepository.save(existingContract);
        return convertToResponse(existingContract);
    }

    @Override
    public void deleteContract(Long id) {
        contractRepository.deleteById(id);
    }

    private Contract convertToEntity(ContractDTO contractDTO) {
        return Contract.builder()
                .title(contractDTO.getTitle())
                .contractNumber(contractDTO.getContractNumber())
                .status(contractDTO.getStatus())
                .startDate(contractDTO.getStartDate())
                .createdBy(contractDTO.getCreatedBy())
//                .createdAt(contractDTO.getCreatedAt())
//                .updatedAt(contractDTO.getUpdatedAt())
                .build();
    }

    @Transactional
    @Override
    public Contract createContractFromTemplate(ContractDTO dto) {
        // 1. Load mẫu hợp đồng, Party và User từ database
        ContractTemplate template = contractTemplateRepository.findById(dto.getTemplateId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mẫu hợp đồng với id: " + dto.getTemplateId()));
        Party party = partyRepository.findById(dto.getPartyId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Party với id: " + dto.getPartyId()));
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User với id: " + dto.getUserId()));

        // 2. Tạo hợp đồng mới (copy các trường từ mẫu; cho phép override từ DTO nếu cần)
        Contract contract = Contract.builder()
                .title(dto.getTitle() != null ? dto.getTitle() : template.getContractTitle())
                .contractNumber(generateContractNumber())
                .template(template)
                .party(party)
                .user(user)
                .specialTermsA(template.getSpecialTermsA())
                .specialTermsB(template.getSpecialTermsB())
                .contractContent(template.getContractContent())
                .appendixEnabled(template.getAppendixEnabled())
                .transferEnabled(template.getTransferEnabled())
                .autoAddVAT(template.getAutoAddVAT())
                .vatPercentage(template.getVatPercentage())
                .isDateLateChecked(template.getIsDateLateChecked())
                .maxDateLate(template.getMaxDateLate())
                .autoRenew(template.getAutoRenew())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .status(ContractStatus.DRAFT)
                .build();

        // 3. Copy các điều khoản thông thường sang ContractTerm (snapshot)
        List<ContractTerm> contractTerms = new ArrayList<>();
        template.getLegalBasisTerms().forEach(term ->
                contractTerms.add(copyTermToContractTerm(term, TypeTermIdentifier.LEGAL_BASIS, contract))
        );
        template.getGeneralTerms().forEach(term ->
                contractTerms.add(copyTermToContractTerm(term, TypeTermIdentifier.GENERAL_TERMS, contract))
        );
        template.getOtherTerms().forEach(term ->
                contractTerms.add(copyTermToContractTerm(term, TypeTermIdentifier.OTHER_TERMS, contract))
        );
        contract.setContractTerms(contractTerms);

        // 4. Xử lý điều khoản bổ sung:
        // Giả sử mẫu hợp đồng có trường additionalTermConfigs (vẫn thuộc ContractTemplateAdditionalTermDetail)
        // và chúng ta sẽ chuyển dữ liệu đó sang ContractAdditionalTerm & ContractAdditionalTermDetail
        List<ContractAdditionalTerm> contractAdditionalTerms = new ArrayList<>();
        if (template.getAdditionalTermConfigs() != null) {
            for (ContractTemplateAdditionalTermDetail config : template.getAdditionalTermConfigs()) {
                // Xử lý nhóm COMMON
                if (config.getCommonTermIds() != null && !config.getCommonTermIds().isEmpty()) {
                    ContractAdditionalTerm additionalCommon = ContractAdditionalTerm.builder()
                            .group(TermGroup.COMMON)
                            .contractTemplate(template)
                            .contract(contract)
                            .build();
                    List<ContractAdditionalTermDetail> commonDetails = new ArrayList<>();
                    for (Long termId : config.getCommonTermIds()) {
                        Term term = termRepository.findById(termId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy điều khoản với id: " + termId));
                        ContractAdditionalTermDetail detail = ContractAdditionalTermDetail.builder()
                                .content(term.getLabel())
                                .term(term)
                                .additionalTerm(additionalCommon)
                                .build();
                        commonDetails.add(detail);
                    }
                    additionalCommon.setDetails(commonDetails);
                    contractAdditionalTerms.add(additionalCommon);
                }
                // Xử lý nhóm A
                if (config.getATermIds() != null && !config.getATermIds().isEmpty()) {
                    ContractAdditionalTerm additionalA = ContractAdditionalTerm.builder()
                            .group(TermGroup.A)
                            .contractTemplate(template)
                            .contract(contract)
                            .build();
                    List<ContractAdditionalTermDetail> aDetails = new ArrayList<>();
                    for (Long termId : config.getATermIds()) {
                        Term term = termRepository.findById(termId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy điều khoản với id: " + termId));
                        ContractAdditionalTermDetail detail = ContractAdditionalTermDetail.builder()
                                .content(term.getLabel())
                                .term(term)
                                .additionalTerm(additionalA)
                                .build();
                        aDetails.add(detail);
                    }
                    additionalA.setDetails(aDetails);
                    contractAdditionalTerms.add(additionalA);
                }
                // Xử lý nhóm B
                if (config.getBTermIds() != null && !config.getBTermIds().isEmpty()) {
                    ContractAdditionalTerm additionalB = ContractAdditionalTerm.builder()
                            .group(TermGroup.B)
                            .contractTemplate(template)
                            .contract(contract)
                            .build();
                    List<ContractAdditionalTermDetail> bDetails = new ArrayList<>();
                    for (Long termId : config.getBTermIds()) {
                        Term term = termRepository.findById(termId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy điều khoản với id: " + termId));
                        ContractAdditionalTermDetail detail = ContractAdditionalTermDetail.builder()
                                .content(term.getLabel())
                                .term(term)
                                .additionalTerm(additionalB)
                                .build();
                        bDetails.add(detail);
                    }
                    additionalB.setDetails(bDetails);
                    contractAdditionalTerms.add(additionalB);
                }
            }
        }
        contract.setAdditionalTerms(contractAdditionalTerms);

        // 5. Lưu hợp đồng (với tất cả các snapshot điều khoản)
        return contractRepository.save(contract);
    }

    // Hàm tiện ích copy điều khoản thông thường sang ContractTerm
    private ContractTerm copyTermToContractTerm(Term term, TypeTermIdentifier termType, Contract contract) {
        return ContractTerm.builder()
                .originalTermId(term.getId())
                .termContent(term.getLabel())
                .termType(termType)
                .contract(contract)
                .build();
    }

    // Hàm tạo số hợp đồng (ví dụ đơn giản)
    private String generateContractNumber() {
        return "HD" + System.currentTimeMillis();
    }

    @Transactional
    @Override
    public ContractResponse getContractById(Long id) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng với id: " + id));
        return convertToResponse(contract);
    }

    private ContractResponse convertToResponse(Contract contract) {
        // Map các term snapshot
        List<ContractTermResponse> termResponses = contract.getContractTerms().stream()
                .map(term -> ContractTermResponse.builder()
                        .id(term.getId())
                        .originalTermId(term.getOriginalTermId())
                        .termContent(term.getTermContent())
                        .termType(term.getTermType().toString())
                        .additionalGroup(term.getAdditionalGroup())
                        .build())
                .collect(Collectors.toList());

        // Map các nhóm additional term
        List<AdditionalTermResponse> additionalTermResponses = contract.getAdditionalTerms().stream()
                .map(group -> AdditionalTermResponse.builder()
                        .id(group.getId())
                        .group(group.getGroup().toString())
                        .details(
                                group.getDetails().stream()
                                        .map(detail -> AdditionalTermDetailResponse.builder()
                                                .id(detail.getId())
                                                .content(detail.getContent())
                                                .termId(detail.getTerm() != null ? detail.getTerm().getId() : null)
                                                .build())
                                        .collect(Collectors.toList())
                        )
                        .build())
                .collect(Collectors.toList());

        return ContractResponse.builder()
                .id(contract.getId())
                .title(contract.getTitle())
                .contractNumber(contract.getContractNumber())
                .description(contract.getDescription())
                .status(contract.getStatus())
                .startDate(contract.getStartDate())
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt())
                .createdBy(contract.getCreatedBy())
                .contractTerms(termResponses)
                .additionalTerms(additionalTermResponses)
                .build();
    }
}

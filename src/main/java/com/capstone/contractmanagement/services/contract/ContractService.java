package com.capstone.contractmanagement.services.contract;

import com.capstone.contractmanagement.components.SecurityUtils;
import com.capstone.contractmanagement.dtos.IdDTO;
import com.capstone.contractmanagement.dtos.contract.ContractDTO;
import com.capstone.contractmanagement.dtos.contract.TermSnapshotDTO;
import com.capstone.contractmanagement.entities.*;
import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.enums.TermGroup;
import com.capstone.contractmanagement.enums.TypeTermIdentifier;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.*;
import com.capstone.contractmanagement.responses.contract.*;
import com.capstone.contractmanagement.responses.term.TermResponse;
import com.capstone.contractmanagement.responses.term.TypeTermResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractService implements IContractService{


    private final IContractRepository contractRepository;
    private final IContractTemplateRepository contractTemplateRepository;
    private final IUserRepository userRepository;
    private final IPartyRepository partyRepository;
    private final ITermRepository termRepository;
    private final IContractTypeRepository contractTypeRepository;
    private final SecurityUtils currentUser;
    private final ITypeTermRepository typeTermRepository;
    @Override
    public List<ContractResponse> getAllContracts() {
        return null;
    }

    @Override
    public ContractResponse updateContract(Long id, ContractDTO contractDTO) {
        Contract existingContract = contractRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found with id: " + id));

        existingContract.setTitle(contractDTO.getTitle());
        existingContract.setStatus(contractDTO.getStatus());
        contractRepository.save(existingContract);
        return null;
    }

    @Override
    public void deleteContract(Long id) {
        contractRepository.deleteById(id);
    }

    @Transactional
    @Override
    public Contract createContractFromTemplate(ContractDTO dto) {
        // 1. Load các entity cần thiết
        ContractTemplate template = contractTemplateRepository.findById(dto.getTemplateId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mẫu hợp đồng với id: " + dto.getTemplateId()));
        Party party = partyRepository.findById(dto.getPartyId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Party với id: " + dto.getPartyId()));
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User với id: " + dto.getUserId()));

        // 2. Tạo hợp đồng mới, lấy dữ liệu từ DTO hoặc từ templateSnapshot
        Contract contract = Contract.builder()
                .title(dto.getTitle() != null ? dto.getTitle() : dto.getTemplateSnapshot().getContractTitle())
                .contractNumber(dto.getContractNumber() != null ? dto.getContractNumber() : generateContractNumber())
                .template(template)  // Ghi nhận nguồn gốc mẫu
                .party(party)
                .user(user)
                .specialTermsA(dto.getTemplateSnapshot().getSpecialTermsA())
                .specialTermsB(dto.getTemplateSnapshot().getSpecialTermsB())
                .contractContent(dto.getTemplateSnapshot().getContractContent())
                .appendixEnabled(dto.getTemplateSnapshot().getAppendixEnabled())
                .transferEnabled(dto.getTemplateSnapshot().getTransferEnabled())
                .autoAddVAT(dto.getTemplateSnapshot().getAutoAddVAT())
                .vatPercentage(dto.getTemplateSnapshot().getVatPercentage())
                .isDateLateChecked(dto.getTemplateSnapshot().getIsDateLateChecked())
                .maxDateLate(dto.getTemplateSnapshot().getMaxDateLate())
                .autoRenew(dto.getTemplateSnapshot().getAutoRenew())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .status(ContractStatus.DRAFT)
                .build();

        // 3. Map các điều khoản đơn giản sang ContractTerm
        List<ContractTerm> contractTerms = new ArrayList<>();
        if (dto.getTemplateSnapshot().getLegalBasisTerms() != null) {
            for (TermSnapshotDTO termDTO : dto.getTemplateSnapshot().getLegalBasisTerms()) {
                contractTerms.add(ContractTerm.builder()
                        .originalTermId(termDTO.getId())
                        .termLabel(termDTO.getLabel())
                        .termValue(termDTO.getValue())
                        .termType(TypeTermIdentifier.LEGAL_BASIS)
                        .contract(contract)
                        .build());
            }
        }
        if (dto.getTemplateSnapshot().getGeneralTerms() != null) {
            for (TermSnapshotDTO termDTO : dto.getTemplateSnapshot().getGeneralTerms()) {
                contractTerms.add(ContractTerm.builder()
                        .originalTermId(termDTO.getId())
                        .termLabel(termDTO.getLabel())
                        .termValue(termDTO.getValue())
                        .termType(TypeTermIdentifier.GENERAL_TERMS)
                        .contract(contract)
                        .build());
            }
        }
        if (dto.getTemplateSnapshot().getOtherTerms() != null) {
            for (TermSnapshotDTO termDTO : dto.getTemplateSnapshot().getOtherTerms()) {
                contractTerms.add(ContractTerm.builder()
                        .originalTermId(termDTO.getId())
                        .termLabel(termDTO.getLabel())
                        .termValue(termDTO.getValue())
                        .termType(TypeTermIdentifier.OTHER_TERMS)
                        .contract(contract)
                        .build());
            }
        }
        contract.setContractTerms(contractTerms);

        // 4. Map additionalConfig sang ContractAdditionalTermDetail
        // additionalConfig: Map<String, Map<String, List<TermSnapshotDTO>>>
        List<ContractAdditionalTermDetail> additionalDetails = new ArrayList<>();
        if (dto.getTemplateSnapshot().getAdditionalConfig() != null) {
            Map<String, Map<String, List<TermSnapshotDTO>>> configMap = dto.getTemplateSnapshot().getAdditionalConfig();
            for (Map.Entry<String, Map<String, List<TermSnapshotDTO>>> entry : configMap.entrySet()) {
                String key = entry.getKey();
                Long configTypeTermId;
                try {
                    configTypeTermId = Long.parseLong(key);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Key trong additionalConfig phải là số đại diện cho type term id. Key sai: " + key);
                }
                Map<String, List<TermSnapshotDTO>> groupConfig = entry.getValue();

                // Map nhóm Common
                List<AdditionalTermSnapshot> commonSnapshots = new ArrayList<>();
                if (groupConfig.containsKey("Common")) {
                    for (TermSnapshotDTO termDTO : groupConfig.get("Common")) {
                        commonSnapshots.add(AdditionalTermSnapshot.builder()
                                .termId(termDTO.getId())
                                .termLabel(termDTO.getLabel())
                                .termValue(termDTO.getValue())
                                .build());
                    }
                }
                // Map nhóm A
                List<AdditionalTermSnapshot> aSnapshots = new ArrayList<>();
                if (groupConfig.containsKey("A")) {
                    for (TermSnapshotDTO termDTO : groupConfig.get("A")) {
                        aSnapshots.add(AdditionalTermSnapshot.builder()
                                .termId(termDTO.getId())
                                .termLabel(termDTO.getLabel())
                                .termValue(termDTO.getValue())
                                .build());
                    }
                }
                // Map nhóm B
                List<AdditionalTermSnapshot> bSnapshots = new ArrayList<>();
                if (groupConfig.containsKey("B")) {
                    for (TermSnapshotDTO termDTO : groupConfig.get("B")) {
                        bSnapshots.add(AdditionalTermSnapshot.builder()
                                .termId(termDTO.getId())
                                .termLabel(termDTO.getLabel())
                                .termValue(termDTO.getValue())
                                .build());
                    }
                }

                // Thực hiện các kiểm tra trùng lặp
                Set<Long> unionCommonA = new HashSet<>(commonSnapshots.stream().map(AdditionalTermSnapshot::getTermId).toList());
                unionCommonA.retainAll(aSnapshots.stream().map(AdditionalTermSnapshot::getTermId).toList());
                if (!unionCommonA.isEmpty()) {
                    throw new IllegalArgumentException("Các điều khoản không được chọn đồng thời ở 'Common' và 'A'");
                }
                Set<Long> unionCommonB = new HashSet<>(commonSnapshots.stream().map(AdditionalTermSnapshot::getTermId).toList());
                unionCommonB.retainAll(bSnapshots.stream().map(AdditionalTermSnapshot::getTermId).toList());
                if (!unionCommonB.isEmpty()) {
                    throw new IllegalArgumentException("Các điều khoản không được chọn đồng thời ở 'Common' và 'B'");
                }
                Set<Long> unionAB = new HashSet<>(aSnapshots.stream().map(AdditionalTermSnapshot::getTermId).toList());
                unionAB.retainAll(bSnapshots.stream().map(AdditionalTermSnapshot::getTermId).toList());
                if (!unionAB.isEmpty()) {
                    throw new IllegalArgumentException("Các điều khoản không được chọn đồng thời ở 'A' và 'B'");
                }

                // Tạo đối tượng ContractAdditionalTermDetail snapshot
                ContractAdditionalTermDetail configDetail = ContractAdditionalTermDetail.builder()
                        .typeTermId(configTypeTermId)
                        .commonTerms(commonSnapshots)
                        .aTerms(aSnapshots)
                        .bTerms(bSnapshots)
                        .contract(contract)
                        .build();
                additionalDetails.add(configDetail);
            }
        }
        contract.setAdditionalTermDetails(additionalDetails);

        // 5. Lưu hợp đồng với toàn bộ snapshot điều khoản và additional config
        return contractRepository.save(contract);
    }

    private String generateContractNumber() {
        return "HD" + System.currentTimeMillis();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ContractResponse> getContractById(Long id) {
        return contractRepository.findById(id)
                .map(contract -> {
                    // Force lazy loading của các collection khi session còn mở.
                    contract.getContractTerms().size();
                    contract.getAdditionalTermDetails().forEach(detail -> {
                        detail.getCommonTerms().size();
                        detail.getATerms().size();
                        detail.getBTerms().size();
                    });
                    return convertContractToResponse(contract);
                });
    }

    private ContractResponse convertContractToResponse(Contract contract) {
        // Map các ContractTerm thành ContractTermResponse
        List<TermResponse> legalBasisTerms = contract.getContractTerms().stream()
                .filter(term -> term.getTermType() == TypeTermIdentifier.LEGAL_BASIS)
                .map(term -> TermResponse.builder()
                        .id(term.getId())
                        .label(term.getTermLabel())
                        .value(term.getTermValue())
                        .build())
                .collect(Collectors.toList());

        List<TermResponse> generalTerms = contract.getContractTerms().stream()
                .filter(term -> term.getTermType() == TypeTermIdentifier.GENERAL_TERMS)
                .map(term -> TermResponse.builder()
                        .id(term.getId())
                        .label(term.getTermLabel())
                        .value(term.getTermValue())
                        .build())
                .collect(Collectors.toList());

        List<TermResponse> otherTerms = contract.getContractTerms().stream()
                .filter(term -> term.getTermType() == TypeTermIdentifier.OTHER_TERMS)
                .map(term -> TermResponse.builder()
                        .id(term.getId())
                        .label(term.getTermLabel())
                        .value(term.getTermValue())
                        .build())
                .collect(Collectors.toList());


        List<TypeTermResponse> additionalTerms = contract.getAdditionalTermDetails().stream()
                .map(detail -> typeTermRepository.findById(detail.getTypeTermId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(typeTerm -> TypeTermResponse.builder()
                        .id(typeTerm.getId())
                        .name(typeTerm.getName())
                        .identifier(typeTerm.getIdentifier())
                        .build())
                .distinct()
                .collect(Collectors.toList());


        // Map additionalConfig từ ContractAdditionalTermDetail sang Map<String, Map<String, List<TermResponse>>>
        Map<String, Map<String, List<TermResponse>>> additionalConfig = contract.getAdditionalTermDetails()
                .stream()
                .collect(Collectors.toMap(
                        detail -> String.valueOf(detail.getTypeTermId()),
                        detail -> {
                            Map<String, List<TermResponse>> innerMap = new HashMap<>();
                            innerMap.put("Common", convertAdditionalTermSnapshotsToTermResponseList(detail.getCommonTerms()));
                            innerMap.put("A", convertAdditionalTermSnapshotsToTermResponseList(detail.getATerms()));
                            innerMap.put("B", convertAdditionalTermSnapshotsToTermResponseList(detail.getBTerms()));
                            return innerMap;
                        }
                ));

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
                .legalBasisTerms(legalBasisTerms)
                .generalTerms(generalTerms)
                .otherTerms(otherTerms)
                .additionalTerms(additionalTerms)
                .additionalConfig(additionalConfig)
                .build();
    }

    // Helper method: chuyển List<AdditionalTermSnapshot> sang List<TermResponse>
    private List<TermResponse> convertAdditionalTermSnapshotsToTermResponseList(List<AdditionalTermSnapshot> snapshots) {
        return snapshots.stream()
                .map(snap -> TermResponse.builder()
                        .id(snap.getTermId())
                        .label(snap.getTermLabel())
                        .value(snap.getTermValue())
                        .build())
                .collect(Collectors.toList());
    }


}

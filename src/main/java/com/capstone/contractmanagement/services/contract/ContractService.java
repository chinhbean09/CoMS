package com.capstone.contractmanagement.services.contract;

import com.capstone.contractmanagement.components.SecurityUtils;
import com.capstone.contractmanagement.dtos.contract.ContractDTO;
import com.capstone.contractmanagement.dtos.contract.TermSnapshotDTO;
import com.capstone.contractmanagement.dtos.payment.PaymentDTO;
import com.capstone.contractmanagement.entities.*;
import com.capstone.contractmanagement.entities.contract.*;
import com.capstone.contractmanagement.entities.contract_template.ContractTemplate;
import com.capstone.contractmanagement.entities.term.Term;
import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.enums.PaymentStatus;
import com.capstone.contractmanagement.enums.TypeTermIdentifier;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.*;
import com.capstone.contractmanagement.responses.User.UserContractResponse;
import com.capstone.contractmanagement.responses.contract.*;
import com.capstone.contractmanagement.responses.payment_one_time.PaymentOneTimeResponse;
import com.capstone.contractmanagement.responses.payment_schedule.PaymentScheduleResponse;
import com.capstone.contractmanagement.responses.term.TermResponse;
import com.capstone.contractmanagement.responses.term.TypeTermResponse;
import com.capstone.contractmanagement.utils.MessageKeys;
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
public class ContractService implements IContractService{


    private final IContractRepository contractRepository;
    private final IContractTemplateRepository contractTemplateRepository;
    private final IUserRepository userRepository;
    private final IPartyRepository partyRepository;
    private final ITermRepository termRepository;
    private final IContractTypeRepository contractTypeRepository;
    private final SecurityUtils currentUser;
    private final ITypeTermRepository typeTermRepository;


    @Transactional
    @Override
    public Contract createContractFromTemplate(ContractDTO dto) {
        // 1. Load các entity cần thiết
        ContractTemplate template = contractTemplateRepository.findById(dto.getTemplateId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mẫu hợp đồng với id: " + dto.getTemplateId()));
        Party party = partyRepository.findById(dto.getPartyId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Party với id: " + dto.getPartyId()));
        User user = currentUser.getLoggedInUser();

        // 2. Tạo hợp đồng mới, lấy dữ liệu từ DTO hoặc từ templateSnapshot
        Contract contract = Contract.builder()
                .title(dto.getTemplateData().getContractTitle())
                .contractNumber(dto.getContractNumber())
                .party(party)
                .user(user)
                .template(template)
                .signingDate(dto.getSigningDate())
                .contractLocation(dto.getContractLocation())
                .amount(dto.getTotalValue())
                .effectiveDate(dto.getEffectiveDate())
                .expiryDate(dto.getExpiryDate())
                .notifyEffectiveDate(dto.getNotifyEffectiveDate())
                .notifyExpiryDate(dto.getNotifyExpiryDate())
                .notifyEffectiveContent(dto.getNotifyEffectiveContent())
                .notifyExpiryContent(dto.getNotifyExpiryContent())
                .title(dto.getTemplateData().getContractTitle())
                .specialTermsA(dto.getTemplateData().getSpecialTermsA())
                .specialTermsB(dto.getTemplateData().getSpecialTermsB())
                .contractContent(dto.getTemplateData().getContractContent())
                .appendixEnabled(dto.getTemplateData().getAppendixEnabled())
                .transferEnabled(dto.getTemplateData().getTransferEnabled())
                .autoAddVAT(dto.getTemplateData().getAutoAddVAT())
                .vatPercentage(dto.getTemplateData().getVatPercentage())
                .isDateLateChecked(dto.getTemplateData().getIsDateLateChecked())
                .maxDateLate(dto.getTemplateData().getMaxDateLate())
                .autoRenew(dto.getTemplateData().getAutoRenew())
                .violate(dto.getTemplateData().getViolate())
                .contractType(template.getContractType())
                .suspend(dto.getTemplateData().getSuspend())
                .suspendContent(dto.getTemplateData().getSuspendContent())
                .status(ContractStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();
        // 3. Map các điều khoản đơn giản sang ContractTerm
        List<ContractTerm> contractTerms = new ArrayList<>();

        // Legal Basis
        if (dto.getTemplateData().getLegalBasisTerms() != null) {
            for (TermSnapshotDTO termDTO : dto.getTemplateData().getLegalBasisTerms()) {
                Term term = termRepository.findById(termDTO.getId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy điều khoản với id: " + termDTO.getId()));
                if (!term.getTypeTerm().getIdentifier().equals(TypeTermIdentifier.LEGAL_BASIS)) {
                    throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại Căn cứ pháp lí (LEGAL_BASIS)");
                }
                contractTerms.add(ContractTerm.builder()
                        .originalTermId(term.getId())
                        .termLabel(term.getLabel())
                        .termValue(term.getValue())
                        .termType(TypeTermIdentifier.LEGAL_BASIS)
                        .contract(contract)
                        .build());
            }
        }
        // General Terms
        if (dto.getTemplateData().getGeneralTerms() != null) {
            for (TermSnapshotDTO termDTO : dto.getTemplateData().getGeneralTerms()) {
                Term term = termRepository.findById(termDTO.getId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy điều khoản với id: " + termDTO.getId()));
                if (!term.getTypeTerm().getIdentifier().equals(TypeTermIdentifier.GENERAL_TERMS)) {
                    throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại Các điều khoản chung (GENERAL_TERMS)");
                }
                contractTerms.add(ContractTerm.builder()
                        .originalTermId(term.getId())
                        .termLabel(term.getLabel())
                        .termValue(term.getValue())
                        .termType(TypeTermIdentifier.GENERAL_TERMS)
                        .contract(contract)
                        .build());
            }
        }
        // Other Terms
        if (dto.getTemplateData().getOtherTerms() != null) {
            for (TermSnapshotDTO termDTO : dto.getTemplateData().getOtherTerms()) {
                Term term = termRepository.findById(termDTO.getId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy điều khoản với id: " + termDTO.getId()));
                if (!term.getTypeTerm().getIdentifier().equals(TypeTermIdentifier.OTHER_TERMS)) {
                    throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại Các điều khoản khác (OTHER_TERMS)");
                }

                contractTerms.add(ContractTerm.builder()
                        .originalTermId(term.getId())
                        .termLabel(term.getLabel())
                        .termValue(term.getValue())
                        .termType(TypeTermIdentifier.OTHER_TERMS)
                        .contract(contract)
                        .build());
            }
        }

        contract.setContractTerms(contractTerms);

        // 4. Map additionalConfig sang ContractAdditionalTermDetail
        // additionalConfig: Map<String, Map<String, List<TermSnapshotDTO>>>
        List<ContractAdditionalTermDetail> additionalDetails = new ArrayList<>();
        if (dto.getTemplateData().getAdditionalConfig() != null) {
            Map<String, Map<String, List<TermSnapshotDTO>>> configMap = dto.getTemplateData().getAdditionalConfig();
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
                        // Lấy term gốc từ DB
                        Term term = termRepository.findById(termDTO.getId())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy điều khoản với id: " + termDTO.getId()));

                        // Gán label, value từ term gốc
                        commonSnapshots.add(AdditionalTermSnapshot.builder()
                                .termId(term.getId())          // Lấy id thực của term
                                .termLabel(term.getLabel())    // Lấy label từ DB
                                .termValue(term.getValue())    // Lấy value từ DB
                                .build());
                    }
                }
                // Map nhóm A
                List<AdditionalTermSnapshot> aSnapshots = new ArrayList<>();
                if (groupConfig.containsKey("A")) {
                    for (TermSnapshotDTO termDTO : groupConfig.get("A")) {
                        Term term = termRepository.findById(termDTO.getId())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy điều khoản với id: " + termDTO.getId()));

                        aSnapshots.add(AdditionalTermSnapshot.builder()
                                .termId(term.getId())
                                .termLabel(term.getLabel())
                                .termValue(term.getValue())
                                .build());
                    }
                }
                // Map nhóm B
                List<AdditionalTermSnapshot> bSnapshots = new ArrayList<>();
                if (groupConfig.containsKey("B")) {
                    for (TermSnapshotDTO termDTO : groupConfig.get("B")) {
                        Term term = termRepository.findById(termDTO.getId())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy điều khoản với id: " + termDTO.getId()));

                        bSnapshots.add(AdditionalTermSnapshot.builder()
                                .termId(term.getId())
                                .termLabel(term.getLabel())
                                .termValue(term.getValue())
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


                // Thêm bước kiểm tra: đảm bảo tất cả các term trong từng nhóm thuộc đúng type term (so sánh với configTypeTermId)
                for (AdditionalTermSnapshot snap : commonSnapshots) {
                    Term term = termRepository.findById(snap.getTermId())
                            .orElseThrow(() -> new IllegalArgumentException("Không tồn tại điều khoản: " + snap.getTermId()));
                    if (!term.getTypeTerm().getId().equals(configTypeTermId)) {
                        throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại điều khoản: \""
                                + term.getTypeTerm().getName() + "\"");
                    }
                }
                for (AdditionalTermSnapshot snap : aSnapshots) {
                    Term term = termRepository.findById(snap.getTermId())
                            .orElseThrow(() -> new IllegalArgumentException("Không tồn tại điều khoản: " + snap.getTermId()));
                    if (!term.getTypeTerm().getId().equals(configTypeTermId)) {
                        throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại điều khoản: \""
                                + term.getTypeTerm().getName() + "\"");
                    }
                }
                for (AdditionalTermSnapshot snap : bSnapshots) {
                    Term term = termRepository.findById(snap.getTermId())
                            .orElseThrow(() -> new IllegalArgumentException("Không tồn tại điều khoản: " + snap.getTermId()));
                    if (!term.getTypeTerm().getId().equals(configTypeTermId)) {
                        throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại điều khoản: \""
                                + term.getTypeTerm().getName() + "\"");
                    }
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

        // Ánh xạ payments
        List<PaymentSchedule> paymentSchedules = new ArrayList<>();
        if (dto.getPayments() != null) {
            int order = 1;
            for (PaymentDTO paymentDTO : dto.getPayments()) {
                PaymentSchedule paymentSchedule = PaymentSchedule.builder()
                        .amount(paymentDTO.getAmount())
                        .paymentDate(paymentDTO.getPaymentDate())
                        .notifyPaymentDate(paymentDTO.getNotifyPaymentDate())
                        .paymentOrder(order++)
                        .status(PaymentStatus.UNPAID)
                        .paymentMethod(paymentDTO.getPaymentMethod())
                        .notifyPaymentContent(paymentDTO.getNotifyPaymentContent())
                        .contract(contract)
                        .build();
                paymentSchedules.add(paymentSchedule);
            }
        }


        contract.setPaymentSchedules(paymentSchedules);

        // 5. Lưu hợp đồng với toàn bộ snapshot điều khoản và additional config
        return contractRepository.save(contract);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<GetAllContractReponse> getAllContracts(Pageable pageable, String keyword, ContractStatus status) {
        boolean hasSearch = keyword != null && !keyword.trim().isEmpty();
        boolean hasStatusFilter = status != null;
        Page<Contract> contracts;

        if (hasSearch && hasStatusFilter) {
            keyword = keyword.trim();
            contracts = contractRepository.findByTitleContainingIgnoreCaseAndStatus(keyword, status, pageable);
        } else if (hasSearch) {
            keyword = keyword.trim();
            contracts = contractRepository.findByTitleContainingIgnoreCaseAndStatusNot(keyword, ContractStatus.DELETED, pageable);
        } else if (hasStatusFilter) {
            contracts = contractRepository.findByStatus(status, pageable);
        } else {
            // Mặc định loại bỏ các hợp đồng có trạng thái DELETED
            contracts = contractRepository.findByStatusNot(ContractStatus.DELETED, pageable);
        }

        return contracts.map(this::convertToGetAllContractResponse);
    }


    private ContractResponse convertToResponseDTO(Contract contract) {
        return ContractResponse.builder()
                .id(contract.getId())
                .title(contract.getTitle())
                .contractNumber(contract.getContractNumber())
                .status(contract.getStatus())
                .createdAt(contract.getCreatedAt())
                .amount(contract.getAmount())
                .build();
    }

    private GetAllContractReponse convertToGetAllContractResponse(Contract contract) {
        return GetAllContractReponse.builder()
                .id(contract.getId())
                .title(contract.getTitle())
                .contractNumber(contract.getContractNumber())
                .status(contract.getStatus())
                .createdAt(contract.getCreatedAt())
                .amount(contract.getAmount())
                .contractType(contract.getContractType())
                .party(Party.builder()
                        .id(contract.getParty().getId())
                        .partnerName(contract.getParty().getPartnerName())
                        .build())
                .user(convertUserToUserContractResponse(contract.getUser()))
                .build();
    }

    private UserContractResponse convertUserToUserContractResponse(User user) {
        return UserContractResponse.builder()
                .fullName(user.getFullName())
                .userId(user.getId())
                .build();
    }


    @Override
    public void deleteContract(Long id) {
        contractRepository.deleteById(id);
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
                        .id(term.getOriginalTermId())
                        .label(term.getTermLabel())
                        .value(term.getTermValue())
                        .build())
                .collect(Collectors.toList());

        List<TermResponse> generalTerms = contract.getContractTerms().stream()
                .filter(term -> term.getTermType() == TypeTermIdentifier.GENERAL_TERMS)
                .map(term -> TermResponse.builder()
                        .id(term.getOriginalTermId())
                        .label(term.getTermLabel())
                        .value(term.getTermValue())
                        .build())
                .collect(Collectors.toList());

        List<TermResponse> otherTerms = contract.getContractTerms().stream()
                .filter(term -> term.getTermType() == TypeTermIdentifier.OTHER_TERMS)
                .map(term -> TermResponse.builder()
                        .id(term.getOriginalTermId())
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
                .user(convertUserToUserContractResponse(contract.getUser()))
                .party(contract.getParty())
                .contractNumber(contract.getContractNumber())
                .status(contract.getStatus())
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt())
                .signingDate(contract.getSigningDate())
                .contractLocation(contract.getContractLocation())
                .amount(contract.getAmount())
                .effectiveDate(contract.getEffectiveDate())
                .expiryDate(contract.getExpiryDate())
                .notifyEffectiveDate(contract.getNotifyEffectiveDate())
                .notifyExpiryDate(contract.getNotifyExpiryDate())
                .notifyEffectiveContent(contract.getNotifyEffectiveContent())
                .notifyExpiryContent(contract.getNotifyExpiryContent())
                .specialTermsA(contract.getSpecialTermsA())
                .specialTermsB(contract.getSpecialTermsB())
                .contractContent(contract.getContractContent())
                .appendixEnabled(contract.getAppendixEnabled())
                .transferEnabled(contract.getTransferEnabled())
                .autoAddVAT(contract.getAutoAddVAT())
                .vatPercentage(contract.getVatPercentage())
                .isDateLateChecked(contract.getIsDateLateChecked())
                .maxDateLate(contract.getMaxDateLate())
                .autoRenew(contract.getAutoRenew())
                .violate(contract.getViolate())
                .suspend(contract.getSuspend())
                .suspendContent(contract.getSuspendContent())
                .legalBasisTerms(legalBasisTerms)
                .generalTerms(generalTerms)
                .otherTerms(otherTerms)
                .paymentOneTime(convertPaymentOneTime(contract.getPaymentOneTime()))
                .paymentSchedules(convertPaymentSchedules(contract.getPaymentSchedules()))
                .additionalTerms(additionalTerms)
                .additionalConfig(additionalConfig)
                .build();
    }
    private PaymentOneTimeResponse convertPaymentOneTime(PaymentOneTime paymentOneTime) {
        if (paymentOneTime == null) {
            return null;
        }
        return PaymentOneTimeResponse.builder()
                .id(paymentOneTime.getId())
                .amount(paymentOneTime.getAmount())
                .currency(paymentOneTime.getCurrency())
                .dueDate(paymentOneTime.getDueDate())
                .status(paymentOneTime.getStatus())
                .build();
    }

    // Helper chuyển đổi danh sách PaymentSchedule thành danh sách PaymentScheduleResponse
    private List<PaymentScheduleResponse> convertPaymentSchedules(List<PaymentSchedule> paymentSchedules) {
        if (paymentSchedules == null || paymentSchedules.isEmpty()) {
            return Collections.emptyList();
        }
        return paymentSchedules.stream()
                .map(schedule -> PaymentScheduleResponse.builder()
                        .id(schedule.getId())
                        .paymentOrder(schedule.getPaymentOrder())
                        .amount(schedule.getAmount())
                        .notifyPaymentDate(schedule.getNotifyPaymentDate())
                        .paymentDate(schedule.getPaymentDate())
                        .status(schedule.getStatus())
                        .paymentMethod(schedule.getPaymentMethod())
                        .notifyPaymentContent(schedule.getNotifyPaymentContent())
                        .reminderEmailSent(schedule.isReminderEmailSent())
                        .overdueEmailSent(schedule.isOverdueEmailSent())
                        .build())
                .collect(Collectors.toList());
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

    @Override
    public ContractResponse updateContract(Long id, ContractDTO contractDTO) {
        return null;
    }

    @Override
    @Transactional
    public Contract duplicateContract(Long contractId) {
        // 1. Lấy hợp đồng gốc từ cơ sở dữ liệu
        Contract originalContract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng với id: " + contractId));

        //hop dong nay da co bao nhieu bang copy
        long copyCount = contractRepository.countByOriginalContractId(originalContract.getId());
        long copyNumber = copyCount + 1;

        // 2. Tạo hợp đồng mới và sao chép các trường từ hợp đồng gốc
        Contract duplicateContract = Contract.builder()
                .title(originalContract.getTitle() + " (Copy " + copyNumber + ")") //Hợp đồng thuê nhà (Copy 2)
                .contractNumber(originalContract.getContractNumber() + "-COPY-" + copyNumber) //HD-001-COPY-2
                .originalContractId(originalContract.getId()) // Liên kết với hợp đồng gốc
                .party(originalContract.getParty())
                .user(originalContract.getUser())
                .template(originalContract.getTemplate())
                .signingDate(null) // Đặt null hoặc giữ nguyên tùy yêu cầu
                .contractLocation(originalContract.getContractLocation())
                .amount(originalContract.getAmount())
                .effectiveDate(null) // Đặt null hoặc giữ nguyên tùy yêu cầu
                .expiryDate(null) // Đặt null hoặc giữ nguyên tùy yêu cầu
                .notifyEffectiveDate(null)
                .notifyExpiryDate(null)
                .notifyEffectiveContent(originalContract.getNotifyEffectiveContent())
                .notifyExpiryContent(originalContract.getNotifyExpiryContent())
                .specialTermsA(originalContract.getSpecialTermsA())
                .specialTermsB(originalContract.getSpecialTermsB())
                .contractContent(originalContract.getContractContent())
                .appendixEnabled(originalContract.getAppendixEnabled())
                .transferEnabled(originalContract.getTransferEnabled())
                .autoAddVAT(originalContract.getAutoAddVAT())
                .vatPercentage(originalContract.getVatPercentage())
                .isDateLateChecked(originalContract.getIsDateLateChecked())
                .maxDateLate(originalContract.getMaxDateLate())
                .autoRenew(originalContract.getAutoRenew())
                .violate(originalContract.getViolate())
                .suspend(originalContract.getSuspend())
                .suspendContent(originalContract.getSuspendContent())
                .contractType(originalContract.getContractType())
                .status(ContractStatus.DRAFT) // Đặt trạng thái mới, ví dụ DRAFT
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 3. Sao chép các điều khoản (ContractTerm) từ hợp đồng gốc
        List<ContractTerm> duplicateTerms = new ArrayList<>();
        for (ContractTerm originalTerm : originalContract.getContractTerms()) {
            ContractTerm newTerm = ContractTerm.builder()
                    .originalTermId(originalTerm.getOriginalTermId())
                    .termLabel(originalTerm.getTermLabel())
                    .termValue(originalTerm.getTermValue())
                    .termType(originalTerm.getTermType())
                    .contract(duplicateContract)
                    .build();
            duplicateTerms.add(newTerm);
        }
        duplicateContract.setContractTerms(duplicateTerms);

        // 4. Sao chép các chi tiết điều khoản bổ sung (ContractAdditionalTermDetail)
        List<ContractAdditionalTermDetail> duplicateAdditionalDetails = new ArrayList<>();
        for (ContractAdditionalTermDetail originalDetail : originalContract.getAdditionalTermDetails()) {
            ContractAdditionalTermDetail newDetail = ContractAdditionalTermDetail.builder()
                    .typeTermId(originalDetail.getTypeTermId())
                    .commonTerms(new ArrayList<>(originalDetail.getCommonTerms()))
                    .aTerms(new ArrayList<>(originalDetail.getATerms()))
                    .bTerms(new ArrayList<>(originalDetail.getBTerms()))
                    .contract(duplicateContract)
                    .build();
            duplicateAdditionalDetails.add(newDetail);
        }
        duplicateContract.setAdditionalTermDetails(duplicateAdditionalDetails);

        // 5. Sao chép các lịch thanh toán (PaymentSchedule)
        List<PaymentSchedule> duplicatePaymentSchedules = new ArrayList<>();
        for (PaymentSchedule originalPayment : originalContract.getPaymentSchedules()) {
            PaymentSchedule newPayment = PaymentSchedule.builder()
                    .amount(originalPayment.getAmount())
                    .paymentDate(null) // Đặt null hoặc giữ nguyên tùy yêu cầu
                    .notifyPaymentDate(null)
                    .paymentOrder(originalPayment.getPaymentOrder())
                    .status(PaymentStatus.UNPAID) // Đặt trạng thái chưa thanh toán
                    .paymentMethod(originalPayment.getPaymentMethod())
                    .notifyPaymentContent(originalPayment.getNotifyPaymentContent())
                    .contract(duplicateContract)
                    .build();
            duplicatePaymentSchedules.add(newPayment);
        }
        duplicateContract.setPaymentSchedules(duplicatePaymentSchedules);

        // 6. Lưu hợp đồng mới vào cơ sở dữ liệu
        return contractRepository.save(duplicateContract);
    }

    @Override
    public boolean softDelete(Long id) {
        Optional<Contract> optionalContract = contractRepository.findById(id);
        if (optionalContract.isPresent()) {
            Contract contract = optionalContract.get();
            contract.setStatus(ContractStatus.DELETED);
            contract.setUpdatedAt(LocalDateTime.now());
            contractRepository.save(contract);
            return true;
        }
        return false;
    }

}

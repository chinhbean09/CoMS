package com.capstone.contractmanagement.services.contract;

import com.capstone.contractmanagement.components.SecurityUtils;
import com.capstone.contractmanagement.dtos.contract.ContractDTO;
import com.capstone.contractmanagement.dtos.contract.ContractUpdateDTO;
import com.capstone.contractmanagement.dtos.contract.TermSnapshotDTO;
import com.capstone.contractmanagement.dtos.payment.PaymentDTO;
import com.capstone.contractmanagement.dtos.payment.PaymentScheduleDTO;
import com.capstone.contractmanagement.entities.*;
import com.capstone.contractmanagement.entities.contract.*;
import com.capstone.contractmanagement.entities.contract_template.ContractTemplate;
import com.capstone.contractmanagement.entities.term.Term;
import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.enums.PaymentStatus;
import com.capstone.contractmanagement.enums.TypeTermIdentifier;
import com.capstone.contractmanagement.repositories.*;
import com.capstone.contractmanagement.responses.User.UserContractResponse;
import com.capstone.contractmanagement.responses.contract.*;
import com.capstone.contractmanagement.responses.payment_one_time.PaymentOneTimeResponse;
import com.capstone.contractmanagement.responses.payment_schedule.PaymentScheduleResponse;
import com.capstone.contractmanagement.responses.term.TermResponse;
import com.capstone.contractmanagement.responses.term.TypeTermResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
    private final IAuditTrailRepository auditTrailRepository;
    private final ObjectMapper objectMapper; // Để serialize object thành JSON


    @Transactional
    @Override
    public Contract createContractFromTemplate(ContractDTO dto) {
        // 1. Load các entity cần thiết
        ContractTemplate template = contractTemplateRepository.findById(dto.getTemplateId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mẫu hợp đồng với id: " + dto.getTemplateId()));
        Party party = partyRepository.findById(dto.getPartyId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Party với id: " + dto.getPartyId()));
        User user = currentUser.getLoggedInUser();

        LocalDateTime createdAt = LocalDateTime.now();
        String contractNumber = generateContractNumber(createdAt, dto.getContractTitle());

        // 2. Tạo hợp đồng mới, lấy dữ liệu từ DTO hoặc từ templateSnapshot
        Contract contract = Contract.builder()
                .title(dto.getTemplateData().getContractTitle())
                .contractNumber(contractNumber)
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
                .title(dto.getContractTitle())
                .specialTermsA(dto.getTemplateData().getSpecialTermsA())
                .specialTermsB(dto.getTemplateData().getSpecialTermsB())
                .contractContent(dto.getTemplateData().getContractContent())
                .appendixEnabled(dto.getTemplateData().getAppendixEnabled())
                .transferEnabled(dto.getTemplateData().getTransferEnabled())
                .autoAddVAT(dto.getTemplateData().getAutoAddVAT())
                .vatPercentage(dto.getTemplateData().getVatPercentage())
                .isDateLateChecked(dto.getTemplateData().getIsDateLateChecked())
                .autoRenew(dto.getTemplateData().getAutoRenew())
                .violate(dto.getTemplateData().getViolate())
                .contractType(template.getContractType())
                .suspend(dto.getTemplateData().getSuspend())
                .suspendContent(dto.getTemplateData().getSuspendContent())
                .status(ContractStatus.CREATED)
                .maxDateLate(dto.getTemplateData().getMaxDateLate())
                .contractType(template.getContractType())
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
        Contract savedContract = contractRepository.save(contract);

        // 6. Ghi audit trail cho các trường quan trọng
        List<AuditTrail> auditTrails = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        String changedBy = user.getFullName(); // Giả định lấy username từ user

        // Ghi audit cho từng trường
        auditTrails.add(createAuditTrail(savedContract, "title", null, savedContract.getTitle(), now, changedBy));
        auditTrails.add(createAuditTrail(savedContract, "contractNumber", null, savedContract.getContractNumber(), now, changedBy));
        auditTrails.add(createAuditTrail(savedContract, "party", null, savedContract.getParty().getId().toString(), now, changedBy));
        auditTrails.add(createAuditTrail(savedContract, "user", null, savedContract.getUser().getId().toString(), now, changedBy));
        auditTrails.add(createAuditTrail(savedContract, "template", null, savedContract.getTemplate().getId().toString(), now, changedBy));
        auditTrails.add(createAuditTrail(savedContract, "signingDate", null,
                savedContract.getSigningDate() != null ? savedContract.getSigningDate().toString() : null, now, changedBy));
        auditTrails.add(createAuditTrail(savedContract, "contractLocation", null, savedContract.getContractLocation(), now, changedBy));
        auditTrails.add(createAuditTrail(savedContract, "amount", null,
                savedContract.getAmount() != null ? savedContract.getAmount().toString() : null, now, changedBy));
        auditTrails.add(createAuditTrail(savedContract, "effectiveDate", null,
                savedContract.getEffectiveDate() != null ? savedContract.getEffectiveDate().toString() : null, now, changedBy));
        auditTrails.add(createAuditTrail(savedContract, "expiryDate", null,
                savedContract.getExpiryDate() != null ? savedContract.getExpiryDate().toString() : null, now, changedBy));
        auditTrails.add(createAuditTrail(savedContract, "status", null, savedContract.getStatus().name(), now, changedBy));
        auditTrails.add(createAuditTrail(savedContract, "createdAt", null, savedContract.getCreatedAt().toString(), now, changedBy));


        for (ContractTerm term : savedContract.getContractTerms()) {
            String newValue = String.format("Term ID: %d, Label: %s, Value: %s, Type: %s",
                    term.getOriginalTermId(), term.getTermLabel(), term.getTermValue(), term.getTermType().name());
            auditTrails.add(AuditTrail.builder()
                    .contract(savedContract)
                    .entityName("ContractTerm")
                    .entityId(term.getId())
                    .action("CREATE")
                    .fieldName("contractTerms")
                    .oldValue(null)
                    .newValue(newValue)
                    .changedAt(now)
                    .changedBy(changedBy)
                    .changeSummary("Created contract term for contract")
                    .build());
        }

        for (ContractAdditionalTermDetail detail : savedContract.getAdditionalTermDetails()) {
            try {
                String newValue = objectMapper.writeValueAsString(detail);
                auditTrails.add(AuditTrail.builder()
                        .contract(savedContract)
                        .entityName("ContractAdditionalTermDetail")
                        .entityId(detail.getId())
                        .action("CREATE")
                        .fieldName("additionalTermDetails")
                        .oldValue(null)
                        .newValue(newValue)
                        .changedAt(now)
                        .changedBy(changedBy)
                        .changeSummary("Created additional term detail for contract")
                        .build());
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize ContractAdditionalTermDetail to JSON", e);
            }
        }
        // Lưu tất cả bản ghi audit trail
        auditTrailRepository.saveAll(auditTrails);

        return savedContract;
    }

    private String generateContractNumber(LocalDateTime createdAt, String contractTitle) {
        String datePart = createdAt.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String titleAbbreviation = generateTitleAbbreviation(contractTitle);
        String prefix = datePart + "-";

        LocalDateTime startOfDay = createdAt.truncatedTo(ChronoUnit.DAYS);
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        int count = contractRepository.countByContractNumberStartingWithAndDate(prefix + "%-" + titleAbbreviation, startOfDay, endOfDay) + 1;
        String sequencePart = String.format("%03d", count);

        return datePart + "-" + sequencePart + "-" + titleAbbreviation;
    }

    // Hàm tạo tên viết tắt từ title
    private String generateTitleAbbreviation(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "HD";
        }

        // Tách các từ, lấy chữ cái đầu, viết hoa
        String[] words = title.trim().split("\\s+");
        return Arrays.stream(words)
                .map(word -> word.isEmpty() ? "" : word.substring(0, 1).toUpperCase())
                .collect(Collectors.joining());
    }



    private AuditTrail createAuditTrail(Contract contract,
                                        String fieldName,
                                        String oldValue,
                                        String newValue,
                                        LocalDateTime changedAt,
                                        String changedBy) {

        AuditTrail auditTrail = new AuditTrail();
        auditTrail.setEntityName("Contract");
        auditTrail.setEntityId(contract.getId());
        auditTrail.setContract(contract);
        auditTrail.setAction("CREATE");
        auditTrail.setFieldName(fieldName);
        auditTrail.setOldValue(oldValue); // null vì là tạo mới
        auditTrail.setNewValue(newValue);
        auditTrail.setChangedAt(changedAt);
        auditTrail.setChangedBy(changedBy);

        // Kiểm tra trước khi trả về
        if (auditTrail.getContract() == null || auditTrail.getContract().getId() == null) {
            throw new IllegalStateException("Contract in AuditTrail is null or has no ID");
        }

        return auditTrail;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GetAllContractReponse> getAllContracts(Pageable pageable, String keyword, ContractStatus status, Long contractTypeId) {
        boolean hasSearch = keyword != null && !keyword.trim().isEmpty();
        boolean hasStatusFilter = status != null;
        boolean hasContractTypeFilter = contractTypeId != null;
        Page<Contract> contracts;

        if (hasStatusFilter) {
            if (hasContractTypeFilter) {
                if (hasSearch) {
                    keyword = keyword.trim();
                    contracts = contractRepository.findByTitleContainingIgnoreCaseAndStatusAndContractTypeId(
                            keyword, status, contractTypeId, pageable);
                } else {
                    contracts = contractRepository.findByStatusAndContractTypeId(status, contractTypeId, pageable);
                }
            } else {
                if (hasSearch) {
                    keyword = keyword.trim();
                    contracts = contractRepository.findByTitleContainingIgnoreCaseAndStatus(keyword, status, pageable);
                } else {
                    contracts = contractRepository.findByStatus(status, pageable);
                }
            }
        } else {
            // Mặc định loại bỏ các hợp đồng có trạng thái DELETED
            if (hasContractTypeFilter) {
                if (hasSearch) {
                    keyword = keyword.trim();
                    contracts = contractRepository.findByTitleContainingIgnoreCaseAndStatusNotAndContractTypeId(
                            keyword, ContractStatus.DELETED, contractTypeId, pageable);
                } else {
                    contracts = contractRepository.findByStatusNotAndContractTypeId(ContractStatus.DELETED, contractTypeId, pageable);
                }
            } else {
                if (hasSearch) {
                    keyword = keyword.trim();
                    contracts = contractRepository.findByTitleContainingIgnoreCaseAndStatusNot(keyword, ContractStatus.DELETED, pageable);
                } else {
                    contracts = contractRepository.findByStatusNot(ContractStatus.DELETED, pageable);
                }
            }
        }

        return contracts.map(this::convertToGetAllContractResponse);
    }

    private GetAllContractReponse convertToGetAllContractResponse(Contract contract) {
        return GetAllContractReponse.builder()
                .id(contract.getId())
                .title(contract.getTitle())
                .contractNumber(contract.getContractNumber())
                .status(contract.getStatus())
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt())
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
                .autoRenew(contract.getAutoRenew())
                .violate(contract.getViolate())
                .suspend(contract.getSuspend())
                .suspendContent(contract.getSuspendContent())
                .legalBasisTerms(legalBasisTerms)
                .generalTerms(generalTerms)
                .contractTypeId(contract.getContractType().getId())
                .otherTerms(otherTerms)
                .maxDateLate(contract.getMaxDateLate())
                .paymentSchedules(convertPaymentSchedules(contract.getPaymentSchedules()))
                .additionalTerms(additionalTerms)
                .version(contract.getVersion())
                .additionalConfig(additionalConfig)
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
                .autoRenew(originalContract.getAutoRenew())
                .violate(originalContract.getViolate())
                .maxDateLate(originalContract.getMaxDateLate())
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

    public int calculateNewVersion(Long originalContractId, Contract currentContract) {
        Integer maxVersion = contractRepository.findMaxVersionByOriginalContractId(originalContractId);
        return (maxVersion != null ? maxVersion : currentContract.getVersion()) + 1;
    }

//    @Transactional
//    @Override
//    public Contract updateContract(Long contractId, ContractUpdateDTO dto) {
//
//        // 1. Tìm hợp đồng hiện tại
//        Contract currentContract = contractRepository.findById(contractId)
//                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng với id: " + contractId));
//
//
//        List<AuditTrail> auditTrails = new ArrayList<>();
//        LocalDateTime now = LocalDateTime.now();
//        String changedBy = currentUser.getLoggedInUser().getFullName();
//
//        // 2. Cập nhật tất cả các trường của Contract
//        //title
//        if (dto.getTitle() != null && !dto.getTitle().equals(currentContract.getTitle())) {
//            auditTrails.add(createAuditTrail(currentContract, "title", currentContract.getTitle(), dto.getTitle(), now,
//                    changedBy, "UPDATE", "Cập nhật tiêu đề hợp đồng"));
//            currentContract.setTitle(dto.getTitle());
//        }
//
//        //ContractNumber
//        if (dto.getContractNumber() != null && !dto.getContractNumber().equals(currentContract.getContractNumber())) {
//            boolean exists = contractRepository.existsByContractNumber(dto.getContractNumber());
//
//            if (exists) {
//                throw new IllegalArgumentException("Contract number already exists in the database");
//            }
//            auditTrails.add(createAuditTrail(currentContract, "contractNumber", currentContract.getContractNumber(),
//                    dto.getContractNumber(), now,
//                    changedBy, "UPDATE", "Cập nhật mã hợp đồng"));
//            currentContract.setContractNumber(dto.getContractNumber());
//        }
//
//        //SigningDate
//        if (dto.getSigningDate() != null && !Objects.equals(dto.getSigningDate(), currentContract.getSigningDate())) {
//            auditTrails.add(createAuditTrail(currentContract, "signingDate",
//                    currentContract.getSigningDate() != null ? currentContract.getSigningDate().toString() : null,
//                    dto.getSigningDate() != null ? dto.getSigningDate().toString() : null, now,
//                    changedBy, "UPDATE", "Cập nhật ngày ky hợp đồng"));
//            currentContract.setSigningDate(dto.getSigningDate());
//        }
//
//        //ContractLocation
//        if (dto.getContractLocation() != null && !dto.getContractLocation().equals(currentContract.getContractLocation())) {
//            auditTrails.add(createAuditTrail(currentContract, "contractLocation", currentContract.getContractLocation(), dto.getContractLocation(),
//                    now, changedBy, "UPDATE", "Cập nhật địa điểm hợp đồng"));
//            currentContract.setContractLocation(dto.getContractLocation());
//        }
//
//        //Amount
//        if (dto.getAmount() != null && !Objects.equals(dto.getAmount(), currentContract.getAmount())) {
//            auditTrails.add(createAuditTrail(currentContract, "amount",
//                    currentContract.getAmount() != null ? currentContract.getAmount().toString() : null,
//                    dto.getAmount() != null ? dto.getAmount().toString() : null, now, changedBy, "UPDATE", "Cập nhật số tiền hợp đồng"));
//            currentContract.setAmount(dto.getAmount());
//        }
//
//        //EffectiveDate
//        if (dto.getEffectiveDate() != null && !Objects.equals(dto.getEffectiveDate(), currentContract.getEffectiveDate())) {
//            auditTrails.add(createAuditTrail(currentContract, "effectiveDate",
//                    currentContract.getEffectiveDate() != null ? currentContract.getEffectiveDate().toString() : null,
//                    dto.getEffectiveDate() != null ? dto.getEffectiveDate().toString() : null, now,
//                    changedBy, "UPDATE", "Cập nhật ngày hiệu lực hợp đồng"));
//            currentContract.setEffectiveDate(dto.getEffectiveDate());
//        }
//
//        //ExpiryDate
//        if (dto.getExpiryDate() != null && !Objects.equals(dto.getExpiryDate(), currentContract.getExpiryDate())) {
//            auditTrails.add(createAuditTrail(currentContract, "expiryDate",
//                    currentContract.getExpiryDate() != null ? currentContract.getExpiryDate().toString() : null,
//                    dto.getExpiryDate() != null ? dto.getExpiryDate().toString() : null, now,
//                    changedBy, "UPDATE", " Cập nhật ngày hết hạn"));
//            currentContract.setExpiryDate(dto.getExpiryDate());
//        }
//
//        //NotifyEffectiveDate
//        if (dto.getNotifyEffectiveDate() != null && !Objects.equals(dto.getNotifyEffectiveDate(), currentContract.getNotifyEffectiveDate())) {
//            auditTrails.add(createAuditTrail(currentContract, "notifyEffectiveDate",
//                    currentContract.getNotifyEffectiveDate() != null ? currentContract.getNotifyEffectiveDate().toString() : null,
//                    dto.getNotifyEffectiveDate() != null ? dto.getNotifyEffectiveDate().toString() : null,
//                    now, changedBy, "UPDATE", "Cập nhật ngày thông báo hiệu lực hợp đồng"));
//            currentContract.setNotifyEffectiveDate(dto.getNotifyEffectiveDate());
//        }
//
//        //NotifyExpiryDate
//
//        if (dto.getNotifyExpiryDate() != null && !Objects.equals(dto.getNotifyExpiryDate(), currentContract.getNotifyExpiryDate())) {
//            auditTrails.add(createAuditTrail(currentContract, "notifyExpiryDate",
//                    currentContract.getNotifyExpiryDate() != null ? currentContract.getNotifyExpiryDate().toString() : null,
//                    dto.getNotifyExpiryDate() != null ? dto.getNotifyExpiryDate().toString() : null, now,
//                    changedBy, "UPDATE", "Cập nhật ngày thông báo hết hạn"));
//            currentContract.setNotifyExpiryDate(dto.getNotifyExpiryDate());
//        }
//
//        //NotifyEffectiveContent
//
//        if (dto.getNotifyEffectiveContent() != null && !dto.getNotifyEffectiveContent().equals(currentContract.getNotifyEffectiveContent())) {
//            auditTrails.add(createAuditTrail(currentContract, "notifyEffectiveContent", currentContract.getNotifyEffectiveContent(),
//                    dto.getNotifyEffectiveContent(), now, changedBy, "UPDATE", "Cập nhật nội dung thống báo hiệu lực hợp đồng"));
//            currentContract.setNotifyEffectiveContent(dto.getNotifyEffectiveContent());
//        }
//
//        //NotifyExpiryContent
//
//        if (dto.getNotifyExpiryContent() != null && !dto.getNotifyExpiryContent().equals(currentContract.getNotifyExpiryContent())) {
//            auditTrails.add(createAuditTrail(currentContract, "notifyExpiryContent", currentContract.getNotifyExpiryContent(),
//                    dto.getNotifyExpiryContent(), now, changedBy, "UPDATE", "Cập nhật nội dung thống báo hết hạn"));
//            currentContract.setNotifyExpiryContent(dto.getNotifyExpiryContent());
//        }
//
//        //SpecialTermsA
//
//        if (dto.getSpecialTermsA() != null && !dto.getSpecialTermsA().equals(currentContract.getSpecialTermsA())) {
//            auditTrails.add(createAuditTrail(currentContract, "specialTermsA", currentContract.getSpecialTermsA(),
//                    dto.getSpecialTermsA(), now, changedBy, "UPDATE", "Cập nhật điều khoản bên A"));
//            currentContract.setSpecialTermsA(dto.getSpecialTermsA());
//        }
//
//        //SpecialTermsB
//
//        if (dto.getSpecialTermsB() != null && !dto.getSpecialTermsB().equals(currentContract.getSpecialTermsB())) {
//            auditTrails.add(createAuditTrail(currentContract, "specialTermsB", currentContract.getSpecialTermsB(), dto.getSpecialTermsB(),
//                    now, changedBy, "UPDATE", "Cập nhật điều khoản bên B"));
//            currentContract.setSpecialTermsB(dto.getSpecialTermsB());
//        }
//
//        //ContractContent
//        if (dto.getContractContent() != null && !dto.getContractContent().equals(currentContract.getContractContent())) {
//            auditTrails.add(createAuditTrail(currentContract, "contractContent", currentContract.getContractContent(), dto.getContractContent()
//                    , now, changedBy, "UPDATE", "Cập nhật nội dung hợp đồng"));
//            currentContract.setContractContent(dto.getContractContent());
//        }
//
//        //AppendixEnabled
//        if (dto.getAppendixEnabled() != null && !dto.getAppendixEnabled().equals(currentContract.getAppendixEnabled())) {
//            auditTrails.add(createAuditTrail(currentContract, "appendixEnabled",
//                    currentContract.getAppendixEnabled() != null ? currentContract.getAppendixEnabled().toString() : null,
//                    dto.getAppendixEnabled() != null ? dto.getAppendixEnabled().toString() : null, now,
//                    changedBy, "UPDATE", "Cập nhật phụ lục"));
//            currentContract.setAppendixEnabled(dto.getAppendixEnabled());
//        }
//
//        //TransferEnabled
//        if (dto.getTransferEnabled() != null && !dto.getTransferEnabled().equals(currentContract.getTransferEnabled())) {
//            auditTrails.add(createAuditTrail(currentContract, "transferEnabled",
//                    currentContract.getTransferEnabled() != null ? currentContract.getTransferEnabled().toString() : null,
//                    dto.getTransferEnabled() != null ? dto.getTransferEnabled().toString() : null, now,
//                    changedBy, "UPDATE", "Cập nhật chuyển giao"));
//            currentContract.setTransferEnabled(dto.getTransferEnabled());
//        }
//
//        //AutoAddVAT
//        if (dto.getAutoAddVAT() != null && !dto.getAutoAddVAT().equals(currentContract.getAutoAddVAT())) {
//            auditTrails.add(createAuditTrail(currentContract, "autoAddVAT",
//                    currentContract.getAutoAddVAT() != null ? currentContract.getAutoAddVAT().toString() : null,
//                    dto.getAutoAddVAT() != null ? dto.getAutoAddVAT().toString() : null, now,
//                    changedBy, "UPDATE", "Cập nhật VAT"));
//            currentContract.setAutoAddVAT(dto.getAutoAddVAT());
//        }
//
//
//        //VatPercentage
//        if (dto.getVatPercentage() != null && !Objects.equals(dto.getVatPercentage(), currentContract.getVatPercentage())) {
//            auditTrails.add(createAuditTrail(currentContract, "vatPercentage",
//                    currentContract.getVatPercentage() != null ? currentContract.getVatPercentage().toString() : null,
//                    dto.getVatPercentage() != null ? dto.getVatPercentage().toString() : null,
//                    now, changedBy, "UPDATE", "Cập nhật VAT %"));
//            currentContract.setVatPercentage(dto.getVatPercentage());
//        }
//
//        //IsDateLateChecked
//        if (dto.getIsDateLateChecked() != null && !dto.getIsDateLateChecked().equals(currentContract.getIsDateLateChecked())) {
//            auditTrails.add(createAuditTrail(currentContract, "isDateLateChecked",
//                    currentContract.getIsDateLateChecked() != null ? currentContract.getIsDateLateChecked().toString() : null,
//                    dto.getIsDateLateChecked() != null ? dto.getIsDateLateChecked().toString() : null, now,
//                    changedBy, "UPDATE", "Cập nhật trễ ngày hợp đồng"));
//            currentContract.setIsDateLateChecked(dto.getIsDateLateChecked());
//        }
//
//        //AutoRenew
//        if (dto.getAutoRenew() != null && !dto.getAutoRenew().equals(currentContract.getAutoRenew())) {
//            auditTrails.add(createAuditTrail(currentContract, "autoRenew",
//                    currentContract.getAutoRenew() != null ? currentContract.getAutoRenew().toString() : null,
//                    dto.getAutoRenew() != null ? dto.getAutoRenew().toString() : null, now,
//                    changedBy, "UPDATE", "Cập nhật renew cho hợp đồng"));
//            currentContract.setAutoRenew(dto.getAutoRenew());
//        }
//
//        //Violate
//        if (dto.getViolate() != null && !dto.getViolate().equals(currentContract.getViolate())) {
//            auditTrails.add(createAuditTrail(currentContract, "violate",
//                    currentContract.getViolate() != null ? currentContract.getViolate().toString() : null,
//                    dto.getViolate() != null ? dto.getViolate().toString() : null, now,
//                    changedBy, "UPDATE", "Cập nhật vi phạm cho hợp đồng"));
//            currentContract.setViolate(dto.getViolate());
//        }
//
//        //Suspend
//        if (dto.getSuspend() != null && !dto.getSuspend().equals(currentContract.getSuspend())) {
//            auditTrails.add(createAuditTrail(currentContract, "suspend",
//                    currentContract.getSuspend() != null ? currentContract.getSuspend().toString() : null,
//                    dto.getSuspend() != null ? dto.getSuspend().toString() : null, now,
//                    changedBy, "UPDATE", "Cập nhật suspend cho hợp đồng"));
//            currentContract.setSuspend(dto.getSuspend());
//        }
//
//
//        //SuspendContent
//        if (dto.getSuspendContent() != null && !dto.getSuspendContent().equals(currentContract.getSuspendContent())) {
//            auditTrails.add(createAuditTrail(currentContract, "suspendContent", currentContract.getSuspendContent(),
//                    dto.getSuspendContent(), now, changedBy, "UPDATE", "Cập nhật nội dung suspend cho hợp đồng"));
//            currentContract.setSuspendContent(dto.getSuspendContent());
//        }
//
//        //MaxDateLate
//        if (dto.getMaxDateLate() != null && !dto.getMaxDateLate().equals(currentContract.getMaxDateLate())) {
//            auditTrails.add(createAuditTrail(currentContract, "maxDateLate", String.valueOf(currentContract.getMaxDateLate()),
//                    String.valueOf(dto.getMaxDateLate()), now, changedBy, "UPDATE", "Cập nhật nội dung suspend cho hợp đồng"));
//            currentContract.setMaxDateLate(dto.getMaxDateLate());
//        }
//
//        //Status
//        if (dto.getStatus() != null && !dto.getStatus().equals(currentContract.getStatus())) {
//            auditTrails.add(createAuditTrail(currentContract, "status", currentContract.getStatus().name(), dto.getStatus().name(),
//                    now, changedBy, "UPDATE", "Cập nhật trạng thái hợp đồng"));
//            currentContract.setStatus(dto.getStatus());
//        }
//
//        // 3. ContractTerm
//
///*
////nếu term id có trong dto thì check db
//[DTO Terms] --> (Kiểm tra tồn tại trong DB?)
//                |
//                |-- Có --> (Đúng loại term?) --> [So sánh với ContractTerm]
//                |               |                    |
//                |               |-- Giống --> [NO ACTION]
//                |               |-- Khác --> [UPDATE]
//                |
//                |-- Không --> [ Trả Lỗi]
//
////nếu term id cũ không có trong dto thì xóa đi và ghi audit trail, có thì giữ lại không ghi audit trail
//[ContractTerm cũ] --> (Có trong DTO?)
//                        |
//                        |-- Có --> [Giữ lại]
//                        |-- Không --> [DELETE] --
//
//*/
//        /* danh sách term mới sau khi update của hợp đồng
//
//         * Term mới (CREATE).
//
//         * Term cập nhật (UPDATE).
//
//         * Term không thay đổi (NO ACTION nhưng vẫn giữ lại).
//
//         */
//        List<ContractTerm> updatedTerms = new ArrayList<>();
//
//        //theo dõi các term ID hiện có từ DTO, tập hợp tất cả OriginalTermId từ DTO, dùng để xác định term nào bị xóa.
//        Set<Long> newTermIds = new HashSet<>();
//
//        //lưu audit trail cho các thay đổi của term
//        List<AuditTrail> termAuditTrails = new ArrayList<>();
//
//        // Xử lý LegalBasisTerms
//        if (dto.getLegalBasisTerms() != null) {
//            for (TermSnapshotDTO termDTO : dto.getLegalBasisTerms()) {
//
//                // check tồn tại term trong database
//                Term term = termRepository.findById(termDTO.getId())
//                        .orElseThrow(() -> new RuntimeException("Không tìm thấy Term với id: " + termDTO.getId()));
//                // validate loại term
//                if (!term.getTypeTerm().getIdentifier().equals(TypeTermIdentifier.LEGAL_BASIS)) {
//                    throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại LEGAL_BASIS");
//                }
//
//                // Đánh dấu term ID đang được xử lý chủ yếu để theo dõi
//                newTermIds.add(term.getId());
//
//
//                //tìm trong hợp đồng hiện tại xem đã có term này chưa
//                ContractTerm existingTerm = currentContract.getContractTerms().stream()
//                        .filter(t -> t.getOriginalTermId().equals(term.getId())
//                                && t.getTermType().equals(TypeTermIdentifier.LEGAL_BASIS))
//                        .findFirst()
//                        .orElse(null);
//
//                //CREATE: Nếu existingTerm == null → nghĩa là term này không có trong contract_term nên thêm vào ContractTerm. => tạo audit trail
//                //UPDATE: Nếu existingTerm != null và có thay đổi giá trị → nghĩa là term này có trong contract_term và có thay đổi giá trị nên Cập nhật. => tạo audit trail
//                //        Nếu existingTerm != null không có thay đổi giá trị → nghĩa là term này có trong contract_term nên ta không tạo audit trail.
//
//                // chuẩn bị giá trị cho audit trail
//                String oldValue = null;
//                String newValue = String.format("Term ID: %d, Label: %s, Value: %s, Type: %s",
//                        term.getId(), term.getLabel(), term.getValue(), TypeTermIdentifier.LEGAL_BASIS.name());
//
//
//                if (existingTerm != null) {
//                    // Xử lý term đã tồn tại trong currentContract term
//                    oldValue = String.format("Term ID: %d, Label: %s, Value: %s, Type: %s",
//                            existingTerm.getOriginalTermId(), existingTerm.getTermLabel(), existingTerm.getTermValue(), existingTerm.getTermType().name());
//
//                    // Xử lý term đã tồn tại trong currentContract term và có thay đổi giá trị
//                    if (!oldValue.equals(newValue)) {
//                        //UPDATE: Nếu existingTerm != null và có thay đổi giá trị → Cập nhật. và ghi audit trail
//                        // Cập nhật thông tin term
//                        existingTerm.setTermLabel(term.getLabel());
//                        existingTerm.setTermValue(term.getValue());
//
//                        // Tạo audit trail UPDATE
//                        termAuditTrails.add(AuditTrail.builder()
//                                .contract(currentContract)
//                                .entityName("ContractTerm")
//                                .entityId(existingTerm.getId())
//                                .action("UPDATE")
//                                .fieldName("legalBasisTerms")
//                                .oldValue(oldValue)
//                                .newValue(newValue)
//                                .changedAt(now)
//                                .changedBy(changedBy)
//                                .changeSummary("Đã cập nhật điều khoản cơ sở pháp lý với Term ID: " + term.getId())
//                                .build());
//                    }
//
//                    //existingTerm != null không có thay đổi giá trị → nghĩa là term này có trong contract_term nên ta không tạo audit trail
//                    //ghi vào updatedTerms để theo dõi term update
//                    updatedTerms.add(existingTerm);
//                } else {
//                    //CREATE: existingTerm == null → nghĩa là term này không có trong contract_term nên thêm vào ContractTerm. => tạo audit trail
//                    // Xử lý CREATE term
//                    existingTerm = ContractTerm.builder()
//                            .originalTermId(term.getId())
//                            .termLabel(term.getLabel())
//                            .termValue(term.getValue())
//                            .termType(TypeTermIdentifier.LEGAL_BASIS)
//                            .contract(currentContract)
//                            .build();
//
//                    // Tạo audit trail CREATE
//                    termAuditTrails.add(AuditTrail.builder()
//                            .contract(currentContract)
//                            .entityName("ContractTerm")
//                            .entityId(null) // ID sẽ được cập nhật sau khi lưu
//                            .action("CREATE")
//                            .fieldName("legalBasisTerms")
//                            .oldValue(null)
//                            .newValue(newValue)
//                            .changedAt(now)
//                            .changedBy(changedBy)
//                            .changeSummary("Đã tạo điều khoản cơ sở pháp lý với Term ID: " + term.getId())
//                            .build());
//
//                    //ghi vào updatedTerms để theo dõi term mới
//                    updatedTerms.add(existingTerm);
//                }
//            }
//        }
//
//        // Xử lý GeneralTerms
//        if (dto.getGeneralTerms() != null) {
//            for (TermSnapshotDTO termDTO : dto.getGeneralTerms()) {
//                Term term = termRepository.findById(termDTO.getId())
//                        .orElseThrow(() -> new RuntimeException("Không tìm thấy Term với id: " + termDTO.getId()));
//                if (!term.getTypeTerm().getIdentifier().equals(TypeTermIdentifier.GENERAL_TERMS)) {
//                    throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại GENERAL_TERMS");
//                }
//                newTermIds.add(term.getId());
//                ContractTerm existingTerm = currentContract.getContractTerms().stream()
//                        .filter(t -> t.getOriginalTermId().equals(term.getId()) && t.getTermType().equals(TypeTermIdentifier.GENERAL_TERMS))
//                        .findFirst()
//                        .orElse(null);
//
//                String oldValue = null;
//                String newValue = String.format("Term ID: %d, Label: %s, Value: %s, Type: %s",
//                        term.getId(), term.getLabel(), term.getValue(), TypeTermIdentifier.GENERAL_TERMS.name());
//
//                if (existingTerm != null) {
//                    oldValue = String.format("Term ID: %d, Label: %s, Value: %s, Type: %s",
//                            existingTerm.getOriginalTermId(), existingTerm.getTermLabel(), existingTerm.getTermValue(), existingTerm.getTermType().name());
//                    if (!oldValue.equals(newValue)) {
//                        existingTerm.setTermLabel(term.getLabel());
//                        existingTerm.setTermValue(term.getValue());
//                        termAuditTrails.add(AuditTrail.builder()
//                                .contract(currentContract)
//                                .entityName("ContractTerm")
//                                .entityId(existingTerm.getId())
//                                .action("UPDATE")
//                                .fieldName("generalTerms")
//                                .oldValue(oldValue)
//                                .newValue(newValue)
//                                .changedAt(now)
//                                .changedBy(changedBy)
//                                .changeSummary("Đã cập nhật điều khoản chung với Term ID: " + term.getId())
//                                .build());
//                    }
//                    updatedTerms.add(existingTerm);
//                } else {
//                    existingTerm = ContractTerm.builder()
//                            .originalTermId(term.getId())
//                            .termLabel(term.getLabel())
//                            .termValue(term.getValue())
//                            .termType(TypeTermIdentifier.GENERAL_TERMS)
//                            .contract(currentContract)
//                            .build();
//                    termAuditTrails.add(AuditTrail.builder()
//                            .contract(currentContract)
//                            .entityName("ContractTerm")
//                            .entityId(null)
//                            .action("CREATE")
//                            .fieldName("generalTerms")
//                            .oldValue(null)
//                            .newValue(newValue)
//                            .changedAt(now)
//                            .changedBy(changedBy)
//                            .changeSummary("Đã tạo điều khoản chung với Term ID: " + term.getId())
//                            .build());
//                    updatedTerms.add(existingTerm);
//                }
//            }
//        }
//
//// Xử lý OtherTerms
//        if (dto.getOtherTerms() != null) {
//            for (TermSnapshotDTO termDTO : dto.getOtherTerms()) {
//                Term term = termRepository.findById(termDTO.getId())
//                        .orElseThrow(() -> new RuntimeException("Không tìm thấy Term với id: " + termDTO.getId()));
//                if (!term.getTypeTerm().getIdentifier().equals(TypeTermIdentifier.OTHER_TERMS)) {
//                    throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại OTHER_TERMS");
//                }
//                newTermIds.add(term.getId());
//                ContractTerm existingTerm = currentContract.getContractTerms().stream()
//                        .filter(t -> t.getOriginalTermId().equals(term.getId()) && t.getTermType().equals(TypeTermIdentifier.OTHER_TERMS))
//                        .findFirst()
//                        .orElse(null);
//
//                String oldValue = null;
//                String newValue = String.format("Term ID: %d, Label: %s, Value: %s, Type: %s",
//                        term.getId(), term.getLabel(), term.getValue(), TypeTermIdentifier.OTHER_TERMS.name());
//
//                if (existingTerm != null) {
//                    oldValue = String.format("Term ID: %d, Label: %s, Value: %s, Type: %s",
//                            existingTerm.getOriginalTermId(), existingTerm.getTermLabel(), existingTerm.getTermValue(), existingTerm.getTermType().name());
//                    if (!oldValue.equals(newValue)) {
//                        existingTerm.setTermLabel(term.getLabel());
//                        existingTerm.setTermValue(term.getValue());
//                        termAuditTrails.add(AuditTrail.builder()
//                                .contract(currentContract)
//                                .entityName("ContractTerm")
//                                .entityId(existingTerm.getId())
//                                .action("UPDATE")
//                                .fieldName("otherTerms")
//                                .oldValue(oldValue)
//                                .newValue(newValue)
//                                .changedAt(now)
//                                .changedBy(changedBy)
//                                .changeSummary("Đã cập nhật điều khoản khác với Term ID: " + term.getId())
//                                .build());
//                    }
//
//                    updatedTerms.add(existingTerm);
//                } else {
//
//                    existingTerm = ContractTerm.builder()
//                            .originalTermId(term.getId())
//                            .termLabel(term.getLabel())
//                            .termValue(term.getValue())
//                            .termType(TypeTermIdentifier.OTHER_TERMS)
//                            .contract(currentContract)
//                            .build();
//
//                    termAuditTrails.add(AuditTrail.builder()
//                            .contract(currentContract)
//                            .entityName("ContractTerm")
//                            .entityId(null)
//                            .action("CREATE")
//                            .fieldName("otherTerms")
//                            .oldValue(null)
//                            .newValue(newValue)
//                            .changedAt(now)
//                            .changedBy(changedBy)
//                            .changeSummary("Đã tạo điều khoản khác với Term ID: " + term.getId())
//                            .build());
//                    updatedTerms.add(existingTerm);
//                }
//            }
//        }
//
//        // 4. Cập nhật ContractAdditionalTermDetail
//        if (dto.getAdditionalConfig() != null) {
//            //danh sách chi tiết điều khoản bổ sung sau khi cập nhật
//            List<ContractAdditionalTermDetail> updatedDetails = new ArrayList<>();
//
//            //Danh sách tạm để lưu Audit Trail cho phần điều khoản bổ sung
//            List<AuditTrail> additionalTermAuditTrails = new ArrayList<>();
//
//            // lấy config từ DTO (Map<typeTermId, Map<nhóm, List<Term>> => {1: {A: (A1, A2, A3), B: (B1, B2, B3), Common: (C1, C2, C3)}}
//            Map<String, Map<String, List<TermSnapshotDTO>>> configMap = dto.getAdditionalConfig();
//
//            //tập hợp tất cả typeTermId từ DTO để kiểm tra xóa.
//            Set<Long> newTypeTermIds = configMap.keySet().stream()
//                    .map(Long::parseLong) //String typetermId trong config sang Long
//                    .collect(Collectors.toSet());
//
//            // Record tạm lưu thông tin audit trail chưa có ID
//            record AuditTrailPending(
//                    ContractAdditionalTermDetail detail,
//                    String oldValue,
//                    String action,
//                    Long typeTermId
//            ){}
//
//            // Danh sách audit trail tạm thời
//            List<AuditTrailPending> pendingAudits = new ArrayList<>();
//
//            // Duyệt qua từng entry trong configMap (theo từng Map typeTermId)
//            for (Map.Entry<String, Map<String, List<TermSnapshotDTO>>> entry : configMap.entrySet()) {
//
//                // Lấy typeTermId từ key của Map
//                Long configTypeTermId = Long.parseLong(entry.getKey());
//
//                // Lấy cấu hình nhóm (Common, A, B) cho typeTermId này
//                Map<String, List<TermSnapshotDTO>> groupConfig = entry.getValue();
//
//                // Xử lý các nhóm Common, A, B
//                List<AdditionalTermSnapshot> commonSnapshots = new ArrayList<>();
//                List<AdditionalTermSnapshot> aSnapshots = new ArrayList<>();
//                List<AdditionalTermSnapshot> bSnapshots = new ArrayList<>();
//
//                // Xử lý nhóm Common trong dto
//                if (groupConfig.containsKey("Common")) {
//                    commonSnapshots = groupConfig.get("Common").stream()
//                            .map(termDTO -> {
//
//                                // Kiểm tra term có tồn tại trong database không
//                                Term term = termRepository.findById(termDTO.getId())
//                                        .orElseThrow(() -> new RuntimeException("Không tìm thấy Term với id: " + termDTO.getId()));
//
//                                // Tạo snapshot từ term
//                                return AdditionalTermSnapshot.builder()
//                                        .termId(term.getId())
//                                        .termLabel(term.getLabel())
//                                        .termValue(term.getValue())
//                                        .build();
//                            })
//                            .collect(Collectors.toList());
//                }
//
//                if (groupConfig.containsKey("A")) {
//                    aSnapshots = groupConfig.get("A").stream()
//                            .map(termDTO -> {
//                                Term term = termRepository.findById(termDTO.getId())
//                                        .orElseThrow(() -> new RuntimeException("Không tìm thấy Term với id: " + termDTO.getId()));
//                                return AdditionalTermSnapshot.builder()
//                                        .termId(term.getId())
//                                        .termLabel(term.getLabel())
//                                        .termValue(term.getValue())
//                                        .build();
//                            })
//                            .collect(Collectors.toList());
//                }
//
//                if (groupConfig.containsKey("B")) {
//                    bSnapshots = groupConfig.get("B").stream()
//                            .map(termDTO -> {
//                                Term term = termRepository.findById(termDTO.getId())
//                                        .orElseThrow(() -> new RuntimeException("Không tìm thấy Term với id: " + termDTO.getId()));
//                                return AdditionalTermSnapshot.builder()
//                                        .termId(term.getId())
//                                        .termLabel(term.getLabel())
//                                        .termValue(term.getValue())
//                                        .build();
//                            })
//                            .collect(Collectors.toList());
//                }
//
//                Set<Long> unionCommonA = new HashSet<>(commonSnapshots.stream().map(AdditionalTermSnapshot::getTermId).toList());
//                unionCommonA.retainAll(aSnapshots.stream().map(AdditionalTermSnapshot::getTermId).toList());
//                if (!unionCommonA.isEmpty()) {
//                    throw new IllegalArgumentException("Các điều khoản không được chọn đồng thời ở 'Common' và 'A'");
//                }
//                Set<Long> unionCommonB = new HashSet<>(commonSnapshots.stream().map(AdditionalTermSnapshot::getTermId).toList());
//                unionCommonB.retainAll(bSnapshots.stream().map(AdditionalTermSnapshot::getTermId).toList());
//                if (!unionCommonB.isEmpty()) {
//                    throw new IllegalArgumentException("Các điều khoản không được chọn đồng thời ở 'Common' và 'B'");
//                }
//                Set<Long> unionAB = new HashSet<>(aSnapshots.stream().map(AdditionalTermSnapshot::getTermId).toList());
//                unionAB.retainAll(bSnapshots.stream().map(AdditionalTermSnapshot::getTermId).toList());
//                if (!unionAB.isEmpty()) {
//                    throw new IllegalArgumentException("Các điều khoản không được chọn đồng thời ở 'A' và 'B'");
//                }
//
//                //Tìm hoặc tạo ContractAdditionalTermDetail trong currentContract (Term của hợp đồng hiện tại)
//                ContractAdditionalTermDetail detail = currentContract.getAdditionalTermDetails().stream()
//                        //tìm ra điều khoản bổ sung trong currentContract có giống với điều khoản bổ sung trong dto không (Theo typeTermId)
//                        .filter(d -> d.getTypeTermId().equals(configTypeTermId))
//                        .findFirst()
//                        .orElse(null); // Không tìm thấy -> nghĩa là điều khoản bổ sung mới chưa có trong hợp đồng
//
//                // CREATE: detail = null, Nếu không tìm thấy -> tạo mới => tạo audit trail CREATE
//                // UPDATE: detail != null, Nếu tìm thấy và giá trị cũ của hợp đồng khác với giá trị mới thì ta sẽ cập nhật => tạo audit trail UPDATE
//                //         detail != null, Nếu tìm thấy và giá trị cũ của hợp đồng giống với giá trị mới thì ta không tạo audit trail./
//
//                // Chuẩn bị giá trị cũ cho audit trail
//                String oldValue = null;
//                boolean isExisting = (detail != null);
//
//                // Tạo một bản sao để kiểm tra thay đổi (không ảnh hưởng đến detail gốc)
//                ContractAdditionalTermDetail tempDetail = null;
//                if (isExisting) {
//                    try {
//                        oldValue = objectMapper.writeValueAsString(detail);
//                        tempDetail = objectMapper.readValue(oldValue, ContractAdditionalTermDetail.class); // Clone
//                    } catch (JsonProcessingException e) {
//                        throw new RuntimeException("Failed to serialize ContractAdditionalTermDetail to JSON", e);
//                    }
//                } else {
//                    tempDetail = ContractAdditionalTermDetail.builder()
//                            .contract(currentContract)
//                            .typeTermId(configTypeTermId)
//                            .build();
//                }
//
//                tempDetail.setCommonTerms(commonSnapshots);
//                tempDetail.setATerms(aSnapshots);
//                tempDetail.setBTerms(bSnapshots);
//
//                String newValue;
//
//                try {
//                    newValue = objectMapper.writeValueAsString(tempDetail);
//                } catch (JsonProcessingException e) {
//                    throw new RuntimeException("Failed to serialize ContractAdditionalTermDetail to JSON", e);
//                }
//
//                if (!isExisting) {
//                    // CREATE: Thêm mới vào danh sách và ghi Audit Trail
//                    detail = tempDetail;
//                    updatedDetails.add(detail);
//                    pendingAudits.add(new AuditTrailPending(detail, null, "CREATE", configTypeTermId));
//                } else {
//                    // CASE 2: Đã tồn tại
//                    // LUÔN THÊM VÀO updatedDetails DÙ KHÔNG THAY ĐỔI
//                    // thêm một đối tượng detail
//                    updatedDetails.add(detail);
//
//                    if (!Objects.equals(oldValue, newValue)) {
//                        // UPDATE: Có thay đổi -> Cập nhật và ghi Audit Trail
//                        detail.setCommonTerms(commonSnapshots);
//                        detail.setATerms(aSnapshots);
//                        detail.setBTerms(bSnapshots);
//                        updatedDetails.add(detail);
//                        pendingAudits.add(new AuditTrailPending(detail, oldValue, "UPDATE", configTypeTermId));
//                    }
//                }
//
//            }
//
//            // Xử lý các chi tiết bị xóa (có trong DB nhưng không có trong DTO)
//            for (ContractAdditionalTermDetail oldDetail : currentContract.getAdditionalTermDetails()) {
//                if (!newTypeTermIds.contains(oldDetail.getTypeTermId())) {
//                    String oldValue;
//                    try {
//                        oldValue = objectMapper.writeValueAsString(oldDetail);
//                    } catch (JsonProcessingException e) {
//                        throw new RuntimeException("Failed to serialize old ContractAdditionalTermDetail to JSON", e);
//                    }
//                    additionalTermAuditTrails.add(AuditTrail.builder()
//                            .contract(currentContract)
//                            .entityName("ContractAdditionalTermDetail")
//                            .entityId(oldDetail.getId())
//                            .action("DELETE")
//                            .fieldName("additionalTermDetails")
//                            .oldValue(oldValue)
//                            .newValue(null)
//                            .changedAt(now)
//                            .changedBy(changedBy)
//                            .changeSummary("Đã xóa chi tiết điều khoản bổ sung với ID: " + oldDetail.getId() + " và TypeTerm ID: " + oldDetail.getTypeTermId())
//                            .build());
//                }
//            }
//
//            // Cập nhật danh sách chi tiết các điều khoản bổ sung trong hợp đồng
//            currentContract.getAdditionalTermDetails().clear(); // Xóa toàn bộ cũ
//            currentContract.getAdditionalTermDetails().addAll(updatedDetails); // Thêm mới
//
//            // Xử lý audit trail sau khi lưu để có ID
//            if (!pendingAudits.isEmpty() || !additionalTermAuditTrails.isEmpty()) {
//                Contract savedContract = contractRepository.save(currentContract); // Lưu để sinh ID
//
//                // Duyệt qua các audit trail tạm
//                for (AuditTrailPending pending : pendingAudits) {
//
//                    // Lấy chi tiết đã lưu (có ID)
//                    ContractAdditionalTermDetail savedDetail = savedContract.getAdditionalTermDetails().stream()
//                            .filter(d -> d.getTypeTermId().equals(pending.typeTermId()))
//                            .findFirst()
//                            .orElseThrow(() -> new RuntimeException("Không tìm thấy ContractAdditionalTermDetail vừa tạo"));
//
//                    String updatedNewValue;
//
//                    try {
//                        // Cập nhật giá trị mới với ID
//                        updatedNewValue = objectMapper.writeValueAsString(savedDetail); // newValue với id đã được cập nhật
//                    } catch (JsonProcessingException e) {
//                        throw new RuntimeException("Failed to serialize updated ContractAdditionalTermDetail to JSON", e);
//                    }
//
//                    // Thêm vào audit trail chính thức
//                    additionalTermAuditTrails.add(AuditTrail.builder()
//                            .contract(savedContract)
//                            .entityName("ContractAdditionalTermDetail")
//                            .entityId(savedDetail.getId())
//                            .action(pending.action())
//                            .fieldName("additionalTermDetails")
//                            .oldValue(pending.oldValue())
//                            .newValue(updatedNewValue)
//                            .changedAt(now)
//                            .changedBy(changedBy)
//                            .changeSummary(pending.action().equals("CREATE") ?
//                                    "Đã tạo chi tiết điều khoản bổ sung với ID: " + savedDetail.getId() + " và TypeTerm ID: " + savedDetail.getTypeTermId() :
//                                    "Đã cập nhật chi tiết điều khoản bổ sung với ID: " + savedDetail.getId())
//                            .build());
//                }
//
//                // Merge vào audit tổng
//                auditTrails.addAll(additionalTermAuditTrails);
//            }
//        }
//
//        // Kiểm tra term bị xóa
//        for (ContractTerm oldTerm : currentContract.getContractTerms()) {
//
//            //newTermIds là tập hợp các originalTermId từ DTO (dữ liệu mới gửi lên).
//            if (!newTermIds.contains(oldTerm.getOriginalTermId())) {
//
//        // Nếu oldTerm.getOriginalTermId() không có trong newTermIds, điều này nghĩa là term đã bị xóa trong DTO
//                // → tạo audit trail với hành động "DELETE".
//                String oldValue = String.format("Term ID: %d, Label: %s, Value: %s, Type: %s",
//                        oldTerm.getOriginalTermId(), oldTerm.getTermLabel(),
//                        oldTerm.getTermValue(), oldTerm.getTermType().name());
//
//
//                String fieldName = switch (oldTerm.getTermType()) {
//                    case LEGAL_BASIS -> "legalBasisTerms";
//                    case GENERAL_TERMS -> "generalTerms";
//                    case OTHER_TERMS -> "otherTerms";
//                    default -> "contractTerms";
//                };
//                String changeSummary;
//                switch (fieldName) {
//                    case "legalBasisTerms":
//                        changeSummary = "Đã xóa điều khoản cơ sở pháp lý với Term ID: " + oldTerm.getOriginalTermId();
//                        break;
//                    case "generalTerms":
//                        changeSummary = "Đã xóa điều khoản chung với Term ID: " + oldTerm.getOriginalTermId();
//                        break;
//                    case "otherTerms":
//                        changeSummary = "Đã xóa điều khoản khác với Term ID: " + oldTerm.getOriginalTermId();
//                        break;
//                    default:
//                        changeSummary = "Đã xóa điều khoản hợp đồng với Term ID: " + oldTerm.getOriginalTermId();
//                }
//                termAuditTrails.add(AuditTrail.builder()
//                        .contract(currentContract)
//                        .entityName("ContractTerm")
//                        .entityId(oldTerm.getId())
//                        .action("DELETE")
//                        .fieldName(fieldName)
//                        .oldValue(oldValue)
//                        .newValue(null)
//                        .changedAt(now)
//                        .changedBy(changedBy)
//                        .changeSummary(changeSummary)
//                        .build());
//            } else {
//                //Trường hợp oldTerm vẫn tồn tại
//                //Kiểm tra xem oldTerm có trong updatedTerms hay không bằng cách so sánh originalTermId và termType.
//                if (updatedTerms.stream().noneMatch(
//                        t -> t.getOriginalTermId().equals(oldTerm.getOriginalTermId())
//                        && t.getTermType().equals(oldTerm.getTermType()))) {
//                    updatedTerms.add(oldTerm); //Nếu old term không có trong updatedTerms, thêm oldTerm vào updatedTerms
//                }
//            }
//        }
//
//        // Thay thế danh sách ContractTerm cũ bằng danh sách mới
//        currentContract.getContractTerms().clear();
//        currentContract.getContractTerms().addAll(updatedTerms);
//
//
//        // 5. Cập nhật PaymentSchedule
//        if (dto.getPayments() != null) {
//            List<PaymentSchedule> updatedPayments = new ArrayList<>();
//            List<AuditTrail> paymentAuditTrails = new ArrayList<>();
//
//            //check xem có phải thêm payment mới không
//            Set<Long> newPaymentIds = dto.getPayments().stream()
//                    .filter(p -> p.getId() != null)
//                    .map(PaymentScheduleDTO::getId)
//                    .collect(Collectors.toSet());
//
//            // Xử lý từng PaymentSchedule trong DTO
//            for (PaymentScheduleDTO paymentDTO : dto.getPayments()) {
//                PaymentSchedule payment;
//                String oldValue = null;
//                String newValue;
//                String action;
//
//                if (paymentDTO.getId() != null) {
//                    // Cập nhật PaymentSchedule hiện có, nghĩa là paymentid không thay đổi nhưng ta sẽ check value có thay đồi không
//                    payment = currentContract.getPaymentSchedules().stream()
//                            .filter(p -> p.getId().equals(paymentDTO.getId()))
//                            .findFirst()
//                            .orElseThrow(() -> new RuntimeException("Không tìm thấy PaymentSchedule với id: " + paymentDTO.getId()));
//                    oldValue = serializePaymentSchedule(payment);
//                    action = "UPDATE";
//                } else {
//                    // Tạo mới PaymentSchedule
//                    payment = new PaymentSchedule();
//                    payment.setContract(currentContract);
//                    action = "CREATE";
//                }
//
//                // Cập nhật các trường
//                payment.setPaymentOrder(paymentDTO.getPaymentOrder());
//                payment.setAmount(paymentDTO.getAmount());
//                payment.setNotifyPaymentDate(paymentDTO.getNotifyPaymentDate());
//                payment.setPaymentDate(paymentDTO.getPaymentDate());
//                payment.setStatus(paymentDTO.getStatus());
//                payment.setPaymentMethod(paymentDTO.getPaymentMethod());
//                payment.setNotifyPaymentContent(paymentDTO.getNotifyPaymentContent());
//                payment.setReminderEmailSent(paymentDTO.isReminderEmailSent());
//                payment.setOverdueEmailSent(paymentDTO.isOverdueEmailSent());
//
//                newValue = serializePaymentSchedule(payment);
//
//                //check value nếu có thay đồi thì ta sẽ tạo audit trails
//                if (!Objects.equals(oldValue, newValue)) {
//                    paymentAuditTrails.add(AuditTrail.builder()
//                            .contract(currentContract)
//                            .entityName("PaymentSchedule")
//                            .entityId(payment.getId()) // Có thể null nếu là CREATE
//                            .action(action)
//                            .fieldName("paymentSchedules")
//                            .oldValue(oldValue)
//                            .newValue(newValue)
//                            .changedAt(now)
//                            .changedBy(changedBy)
//                            .changeSummary(action.equals("CREATE") ? "Đã tạo PaymentSchedule mới" : "Đã cập nhật PaymentSchedule với ID: " + payment.getId())
//                            .build());
//                }
//                updatedPayments.add(payment);
//            }
//
//            // Kiểm tra và ghi log các PaymentSchedule bị xóa
//            for (PaymentSchedule oldPayment : currentContract.getPaymentSchedules()) {
//                if (!newPaymentIds.contains(oldPayment.getId())) {
//                    String oldValue = serializePaymentSchedule(oldPayment);
//                    paymentAuditTrails.add(AuditTrail.builder()
//                            .contract(currentContract)
//                            .entityName("PaymentSchedule")
//                            .entityId(oldPayment.getId())
//                            .action("DELETE")
//                            .fieldName("paymentSchedules")
//                            .oldValue(oldValue)
//                            .newValue(null)
//                            .changedAt(now)
//                            .changedBy(changedBy)
//                            .changeSummary("Đã xóa PaymentSchedule với ID: " + oldPayment.getId())
//                            .build());
//                }
//            }
//
//            // Cập nhật danh sách PaymentSchedule
//            currentContract.getPaymentSchedules().clear();
//            currentContract.getPaymentSchedules().addAll(updatedPayments);
//
//            // Thêm audit trail của PaymentSchedule vào danh sách chung
//            auditTrails.addAll(paymentAuditTrails);
//        }
//
//
//        // Cập nhật thời gian và lưu
//        if (!auditTrails.isEmpty() || !termAuditTrails.isEmpty()) {
//            currentContract.setUpdatedAt(now);
//            Contract savedContract = contractRepository.save(currentContract);
//
//            // Cập nhật entityId cho các audit trail của ContractTerm mới
//            for (AuditTrail auditTrail : termAuditTrails) {
//                if ("CREATE".equals(auditTrail.getAction())) {
//                    ContractTerm savedTerm = savedContract.getContractTerms().stream()
//                            .filter(t -> t.getOriginalTermId().equals(getTermIdFromNewValue(auditTrail.getNewValue())))
//                            .findFirst()
//                            .orElseThrow(() -> new RuntimeException("Không tìm thấy ContractTerm vừa tạo"));
//                    auditTrail.setEntityId(savedTerm.getId());
//                    // changeSummary đã có Term ID từ lúc tạo, không cần cập nhật lại
//                }
//                auditTrails.add(auditTrail);
//            }
//
//            for (AuditTrail auditTrail : auditTrails) {
//                if ("CREATE".equals(auditTrail.getAction()) && "PaymentSchedule".equals(auditTrail.getEntityName())) {
//                    PaymentSchedule savedPayment = savedContract.getPaymentSchedules().stream()
//                            .filter(p -> p.getPaymentOrder().equals(getPaymentOrderFromNewValue(auditTrail.getNewValue())))
//                            .findFirst()
//                            .orElseThrow(() -> new RuntimeException("Không tìm thấy PaymentSchedule vừa tạo"));
//                    auditTrail.setEntityId(savedPayment.getId());
//                }
//            }
//
//            // Lưu tất cả audit trail
//            auditTrailRepository.saveAll(auditTrails);
//
//        }
//
//        Contract updatedContract = contractRepository.save(currentContract); // Lưu hợp đồng hiện tại sau khi cập nhật
//
//        // Xác định hợp đồng gốc và phiên bản mới
//        Long originalContractId = currentContract.getOriginalContractId() != null
//                ? currentContract.getOriginalContractId()
//                : currentContract.getId();
//        int newVersion = calculateNewVersion(originalContractId, currentContract);
//
//        Contract newContract = Contract.builder()
//                .originalContractId(originalContractId)
//                .version(newVersion)
//                .signingDate(updatedContract.getSigningDate())
//                .contractLocation(updatedContract.getContractLocation())
//                .contractNumber(updatedContract.getContractNumber() + "-v" + newVersion) // Ví dụ: HD001-v2
//                .specialTermsA(updatedContract.getSpecialTermsA())
//                .specialTermsB(updatedContract.getSpecialTermsB())
//                .status(updatedContract.getStatus())
//                .createdAt(LocalDateTime.now())
//                .updatedAt(LocalDateTime.now())
//                .effectiveDate(updatedContract.getEffectiveDate())
//                .expiryDate(updatedContract.getExpiryDate())
//                .notifyEffectiveDate(updatedContract.getNotifyEffectiveDate())
//                .notifyExpiryDate(updatedContract.getNotifyExpiryDate())
//                .notifyEffectiveContent(updatedContract.getNotifyEffectiveContent())
//                .notifyExpiryContent(updatedContract.getNotifyExpiryContent())
//                .title(updatedContract.getTitle())
//                .amount(updatedContract.getAmount())
//                .user(updatedContract.getUser())
//                .isDateLateChecked(updatedContract.getIsDateLateChecked())
//                .template(updatedContract.getTemplate())
//                .party(updatedContract.getParty())
//                .appendixEnabled(updatedContract.getAppendixEnabled())
//                .transferEnabled(updatedContract.getTransferEnabled())
//                .autoAddVAT(updatedContract.getAutoAddVAT())
//                .vatPercentage(updatedContract.getVatPercentage())
//                .autoRenew(updatedContract.getAutoRenew())
//                .violate(updatedContract.getViolate())
//                .suspend(updatedContract.getSuspend())
//                .suspendContent(updatedContract.getSuspendContent())
//                .contractContent(updatedContract.getContractContent())
//                .approvalWorkflow(updatedContract.getApprovalWorkflow())
//                .maxDateLate(updatedContract.getMaxDateLate())
//                .contractType(updatedContract.getContractType())
//                .build();
//
//        // 7. Sao chép ContractTerm
//        List<ContractTerm> newTerms = new ArrayList<>();
//        for (ContractTerm oldTerm : updatedContract.getContractTerms()) {
//            ContractTerm newTerm = ContractTerm.builder()
//                    .originalTermId(oldTerm.getOriginalTermId())
//                    .termLabel(oldTerm.getTermLabel())
//                    .termValue(oldTerm.getTermValue())
//                    .termType(oldTerm.getTermType())
//                    .contract(newContract)
//                    .build();
//            newTerms.add(newTerm);
//        }
//        newContract.setContractTerms(newTerms);
//
//        // 8. Sao chép ContractAdditionalTermDetail
//        List<ContractAdditionalTermDetail> newDetails = new ArrayList<>();
//        for (ContractAdditionalTermDetail oldDetail : updatedContract.getAdditionalTermDetails()) {
//            ContractAdditionalTermDetail newDetail = ContractAdditionalTermDetail.builder()
//                    .contract(newContract)
//                    .typeTermId(oldDetail.getTypeTermId())
//                    .commonTerms(new ArrayList<>(oldDetail.getCommonTerms()))
//                    .aTerms(new ArrayList<>(oldDetail.getATerms()))
//                    .bTerms(new ArrayList<>(oldDetail.getBTerms()))
//                    .build();
//            newDetails.add(newDetail);
//        }
//        newContract.setAdditionalTermDetails(newDetails);
//
//        // 9. Sao chép PaymentSchedule
//        List<PaymentSchedule> newPayments = new ArrayList<>();
//        for (PaymentSchedule oldPayment : updatedContract.getPaymentSchedules()) {
//            PaymentSchedule newPayment = PaymentSchedule.builder()
//                    .contract(newContract)
//                    .paymentOrder(oldPayment.getPaymentOrder())
//                    .amount(oldPayment.getAmount())
//                    .notifyPaymentDate(oldPayment.getNotifyPaymentDate())
//                    .paymentDate(oldPayment.getPaymentDate())
//                    .status(oldPayment.getStatus())
//                    .paymentMethod(oldPayment.getPaymentMethod())
//                    .notifyPaymentContent(oldPayment.getNotifyPaymentContent())
//                    .reminderEmailSent(oldPayment.isReminderEmailSent())
//                    .overdueEmailSent(oldPayment.isOverdueEmailSent())
//                    .build();
//            newPayments.add(newPayment);
//        }
//        newContract.setPaymentSchedules(newPayments);
//
//        // 10. Lưu bản hợp đồng mới
//        Contract savedNewContract = contractRepository.save(newContract);
//
//        // 11. Ghi log audit trail cho hành động tạo phiên bản mới
//        AuditTrail auditTrail = AuditTrail.builder()
//                .contract(savedNewContract)
//                .entityName("Contract")
//                .entityId(savedNewContract.getId())
//                .action("CREATE_VERSION")
//                .fieldName("contract")
//                .oldValue(serializeContract(updatedContract)) // Hàm serializeContract cần được định nghĩa
//                .newValue(serializeContract(savedNewContract))
//                .changedAt(LocalDateTime.now())
//                .changedBy(changedBy)
//                .changeSummary("Đã tạo phiên bản " + newVersion + " của hợp đồng " + updatedContract.getContractNumber())
//                .build();
//        auditTrailRepository.save(auditTrail);
//
//        return savedNewContract;
//    }



    @Transactional
    @Override
    public Contract updateContract(Long contractId, ContractUpdateDTO dto) {

        // 1. Tìm hợp đồng gốc (không thay đổi nó)
        Contract originalContract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng với id: " + contractId));

        LocalDateTime now = LocalDateTime.now();
        String changedBy = currentUser.getLoggedInUser().getFullName();
        List<AuditTrail> auditTrails = new ArrayList<>();

        // 2. Tạo bản sao mới từ hợp đồng gốc
        Long originalContractId = originalContract.getOriginalContractId() != null
                ? originalContract.getOriginalContractId()
                : originalContract.getId();
        int newVersion = calculateNewVersion(originalContractId, originalContract);

        Contract newContract = Contract.builder()
                .originalContractId(originalContractId)
                .version(newVersion)
                .signingDate(originalContract.getSigningDate())
                .contractLocation(originalContract.getContractLocation())
                .contractNumber(originalContract.getContractNumber() + "-v" + newVersion)
                .specialTermsA(originalContract.getSpecialTermsA())
                .specialTermsB(originalContract.getSpecialTermsB())
                .status(originalContract.getStatus())
                .createdAt(now)
                .updatedAt(now)
                .effectiveDate(originalContract.getEffectiveDate())
                .expiryDate(originalContract.getExpiryDate())
                .notifyEffectiveDate(originalContract.getNotifyEffectiveDate())
                .notifyExpiryDate(originalContract.getNotifyExpiryDate())
                .notifyEffectiveContent(originalContract.getNotifyEffectiveContent())
                .notifyExpiryContent(originalContract.getNotifyExpiryContent())
                .title(originalContract.getTitle())
                .amount(originalContract.getAmount())
                .user(originalContract.getUser())
                .isDateLateChecked(originalContract.getIsDateLateChecked())
                .template(originalContract.getTemplate())
                .party(originalContract.getParty())
                .appendixEnabled(originalContract.getAppendixEnabled())
                .transferEnabled(originalContract.getTransferEnabled())
                .autoAddVAT(originalContract.getAutoAddVAT())
                .vatPercentage(originalContract.getVatPercentage())
                .autoRenew(originalContract.getAutoRenew())
                .violate(originalContract.getViolate())
                .suspend(originalContract.getSuspend())
                .suspendContent(originalContract.getSuspendContent())
                .contractContent(originalContract.getContractContent())
                .approvalWorkflow(originalContract.getApprovalWorkflow())
                .maxDateLate(originalContract.getMaxDateLate())
                .contractType(originalContract.getContractType())
                .build();

        // 3. Áp dụng các thay đổi từ DTO vào bản sao mới và ghi audit trail
        applyChangesFromDto(originalContract, newContract, dto, auditTrails, now, changedBy);

        // 4. Cập nhật các quan hệ (ContractTerm, ContractAdditionalTermDetail, PaymentSchedule)
        updateContractTerms(originalContract, newContract, dto, auditTrails, now, changedBy);
        updateContractAdditionalTermDetails(originalContract, newContract, dto, auditTrails, now, changedBy);
        updatePaymentSchedules(originalContract, newContract, dto, auditTrails, now, changedBy);

        // 5. Lưu bản hợp đồng mới
        Contract savedNewContract = contractRepository.save(newContract);

        // 6. Ghi log audit trail cho hành động tạo phiên bản mới
        AuditTrail versionAuditTrail = AuditTrail.builder()
                .contract(savedNewContract)
                .entityName("Contract")
                .entityId(savedNewContract.getId())
                .action("CREATE_VERSION")
                .fieldName("contract")
                .oldValue(serializeContract(originalContract))
                .newValue(serializeContract(savedNewContract))
                .changedAt(now)
                .changedBy(changedBy)
                .changeSummary("Đã tạo phiên bản " + newVersion + " của hợp đồng " + originalContract.getContractNumber())
                .build();
        auditTrails.add(versionAuditTrail);

        // 7. Lưu tất cả audit trail
        auditTrailRepository.saveAll(auditTrails);

        return savedNewContract;

    }

    private void applyChangesFromDto(Contract originalContract, Contract newContract, ContractUpdateDTO dto,
                                     List<AuditTrail> auditTrails, LocalDateTime now, String changedBy) {
        // Title
        if (dto.getTitle() != null && !dto.getTitle().equals(originalContract.getTitle())) {
            auditTrails.add(createAuditTrail(originalContract, "title", originalContract.getTitle(), dto.getTitle(), now,
                    changedBy, "UPDATE", "Cập nhật tiêu đề hợp đồng"));
            newContract.setTitle(dto.getTitle());
        }

        // ContractNumber
        if (dto.getContractNumber() != null && !dto.getContractNumber().equals(originalContract.getContractNumber())) {
            boolean exists = contractRepository.existsByContractNumber(dto.getContractNumber());
            if (exists) {
                throw new IllegalArgumentException("Contract number already exists in the database");
            }
            auditTrails.add(createAuditTrail(originalContract, "contractNumber", originalContract.getContractNumber(),
                    dto.getContractNumber(), now, changedBy, "UPDATE", "Cập nhật mã hợp đồng"));
            newContract.setContractNumber(dto.getContractNumber() + "-v" + newContract.getVersion());
        }

        // SigningDate
        if (dto.getSigningDate() != null && !Objects.equals(dto.getSigningDate(), originalContract.getSigningDate())) {
            auditTrails.add(createAuditTrail(originalContract, "signingDate",
                    originalContract.getSigningDate() != null ? originalContract.getSigningDate().toString() : null,
                    dto.getSigningDate() != null ? dto.getSigningDate().toString() : null, now,
                    changedBy, "UPDATE", "Cập nhật ngày ký hợp đồng"));
            newContract.setSigningDate(dto.getSigningDate());
        }

        // ContractLocation
        if (dto.getContractLocation() != null && !dto.getContractLocation().equals(originalContract.getContractLocation())) {
            auditTrails.add(createAuditTrail(originalContract, "contractLocation", originalContract.getContractLocation(),
                    dto.getContractLocation(), now, changedBy, "UPDATE", "Cập nhật địa điểm hợp đồng"));
            newContract.setContractLocation(dto.getContractLocation());
        }

        // Amount
        if (dto.getAmount() != null && !Objects.equals(dto.getAmount(), originalContract.getAmount())) {
            auditTrails.add(createAuditTrail(originalContract, "amount",
                    originalContract.getAmount() != null ? originalContract.getAmount().toString() : null,
                    dto.getAmount() != null ? dto.getAmount().toString() : null, now, changedBy, "UPDATE", "Cập nhật số tiền hợp đồng"));
            newContract.setAmount(dto.getAmount());
        }

        // EffectiveDate
        if (dto.getEffectiveDate() != null && !Objects.equals(dto.getEffectiveDate(), originalContract.getEffectiveDate())) {
            auditTrails.add(createAuditTrail(originalContract, "effectiveDate",
                    originalContract.getEffectiveDate() != null ? originalContract.getEffectiveDate().toString() : null,
                    dto.getEffectiveDate() != null ? dto.getEffectiveDate().toString() : null, now,
                    changedBy, "UPDATE", "Cập nhật ngày hiệu lực hợp đồng"));
            newContract.setEffectiveDate(dto.getEffectiveDate());
        }

        // ExpiryDate
        if (dto.getExpiryDate() != null && !Objects.equals(dto.getExpiryDate(), originalContract.getExpiryDate())) {
            auditTrails.add(createAuditTrail(originalContract, "expiryDate",
                    originalContract.getExpiryDate() != null ? originalContract.getExpiryDate().toString() : null,
                    dto.getExpiryDate() != null ? dto.getExpiryDate().toString() : null, now,
                    changedBy, "UPDATE", "Cập nhật ngày hết hạn"));
            newContract.setExpiryDate(dto.getExpiryDate());
        }

        // NotifyEffectiveDate
        if (dto.getNotifyEffectiveDate() != null && !Objects.equals(dto.getNotifyEffectiveDate(), originalContract.getNotifyEffectiveDate())) {
            auditTrails.add(createAuditTrail(originalContract, "notifyEffectiveDate",
                    originalContract.getNotifyEffectiveDate() != null ? originalContract.getNotifyEffectiveDate().toString() : null,
                    dto.getNotifyEffectiveDate() != null ? dto.getNotifyEffectiveDate().toString() : null, now,
                    changedBy, "UPDATE", "Cập nhật ngày thông báo hiệu lực hợp đồng"));
            newContract.setNotifyEffectiveDate(dto.getNotifyEffectiveDate());
        }

        // NotifyExpiryDate
        if (dto.getNotifyExpiryDate() != null && !Objects.equals(dto.getNotifyExpiryDate(), originalContract.getNotifyExpiryDate())) {
            auditTrails.add(createAuditTrail(originalContract, "notifyExpiryDate",
                    originalContract.getNotifyExpiryDate() != null ? originalContract.getNotifyExpiryDate().toString() : null,
                    dto.getNotifyExpiryDate() != null ? dto.getNotifyExpiryDate().toString() : null, now,
                    changedBy, "UPDATE", "Cập nhật ngày thông báo hết hạn"));
            newContract.setNotifyExpiryDate(dto.getNotifyExpiryDate());
        }

        // NotifyEffectiveContent
        if (dto.getNotifyEffectiveContent() != null && !dto.getNotifyEffectiveContent().equals(originalContract.getNotifyEffectiveContent())) {
            auditTrails.add(createAuditTrail(originalContract, "notifyEffectiveContent", originalContract.getNotifyEffectiveContent(),
                    dto.getNotifyEffectiveContent(), now, changedBy, "UPDATE", "Cập nhật nội dung thông báo hiệu lực hợp đồng"));
            newContract.setNotifyEffectiveContent(dto.getNotifyEffectiveContent());
        }

        // NotifyExpiryContent
        if (dto.getNotifyExpiryContent() != null && !dto.getNotifyExpiryContent().equals(originalContract.getNotifyExpiryContent())) {
            auditTrails.add(createAuditTrail(originalContract, "notifyExpiryContent", originalContract.getNotifyExpiryContent(),
                    dto.getNotifyExpiryContent(), now, changedBy, "UPDATE", "Cập nhật nội dung thông báo hết hạn"));
            newContract.setNotifyExpiryContent(dto.getNotifyExpiryContent());
        }

        // SpecialTermsA
        if (dto.getSpecialTermsA() != null && !dto.getSpecialTermsA().equals(originalContract.getSpecialTermsA())) {
            auditTrails.add(createAuditTrail(originalContract, "specialTermsA", originalContract.getSpecialTermsA(),
                    dto.getSpecialTermsA(), now, changedBy, "UPDATE", "Cập nhật điều khoản bên A"));
            newContract.setSpecialTermsA(dto.getSpecialTermsA());
        }

        // SpecialTermsB
        if (dto.getSpecialTermsB() != null && !dto.getSpecialTermsB().equals(originalContract.getSpecialTermsB())) {
            auditTrails.add(createAuditTrail(originalContract, "specialTermsB", originalContract.getSpecialTermsB(),
                    dto.getSpecialTermsB(), now, changedBy, "UPDATE", "Cập nhật điều khoản bên B"));
            newContract.setSpecialTermsB(dto.getSpecialTermsB());
        }

        // ContractContent
        if (dto.getContractContent() != null && !dto.getContractContent().equals(originalContract.getContractContent())) {
            auditTrails.add(createAuditTrail(originalContract, "contractContent", originalContract.getContractContent(),
                    dto.getContractContent(), now, changedBy, "UPDATE", "Cập nhật nội dung hợp đồng"));
            newContract.setContractContent(dto.getContractContent());
        }

        // AppendixEnabled
        if (dto.getAppendixEnabled() != null && !dto.getAppendixEnabled().equals(originalContract.getAppendixEnabled())) {
            auditTrails.add(createAuditTrail(originalContract, "appendixEnabled",
                    originalContract.getAppendixEnabled() != null ? originalContract.getAppendixEnabled().toString() : null,
                    dto.getAppendixEnabled() != null ? dto.getAppendixEnabled().toString() : null, now,
                    changedBy, "UPDATE", "Cập nhật phụ lục"));
            newContract.setAppendixEnabled(dto.getAppendixEnabled());
        }

        // TransferEnabled
        if (dto.getTransferEnabled() != null && !dto.getTransferEnabled().equals(originalContract.getTransferEnabled())) {
            auditTrails.add(createAuditTrail(originalContract, "transferEnabled",
                    originalContract.getTransferEnabled() != null ? originalContract.getTransferEnabled().toString() : null,
                    dto.getTransferEnabled() != null ? dto.getTransferEnabled().toString() : null, now,
                    changedBy, "UPDATE", "Cập nhật chuyển giao"));
            newContract.setTransferEnabled(dto.getTransferEnabled());
        }

        // AutoAddVAT
        if (dto.getAutoAddVAT() != null && !dto.getAutoAddVAT().equals(originalContract.getAutoAddVAT())) {
            auditTrails.add(createAuditTrail(originalContract, "autoAddVAT",
                    originalContract.getAutoAddVAT() != null ? originalContract.getAutoAddVAT().toString() : null,
                    dto.getAutoAddVAT() != null ? dto.getAutoAddVAT().toString() : null, now,
                    changedBy, "UPDATE", "Cập nhật VAT"));
            newContract.setAutoAddVAT(dto.getAutoAddVAT());
        }

        // VatPercentage
        if (dto.getVatPercentage() != null && !Objects.equals(dto.getVatPercentage(), originalContract.getVatPercentage())) {
            auditTrails.add(createAuditTrail(originalContract, "vatPercentage",
                    originalContract.getVatPercentage() != null ? originalContract.getVatPercentage().toString() : null,
                    dto.getVatPercentage() != null ? dto.getVatPercentage().toString() : null, now,
                    changedBy, "UPDATE", "Cập nhật VAT %"));
            newContract.setVatPercentage(dto.getVatPercentage());
        }

        // IsDateLateChecked
        if (dto.getIsDateLateChecked() != null && !dto.getIsDateLateChecked().equals(originalContract.getIsDateLateChecked())) {
            auditTrails.add(createAuditTrail(originalContract, "isDateLateChecked",
                    originalContract.getIsDateLateChecked() != null ? originalContract.getIsDateLateChecked().toString() : null,
                    dto.getIsDateLateChecked() != null ? dto.getIsDateLateChecked().toString() : null, now,
                    changedBy, "UPDATE", "Cập nhật trễ ngày hợp đồng"));
            newContract.setIsDateLateChecked(dto.getIsDateLateChecked());
        }

        // AutoRenew
        if (dto.getAutoRenew() != null && !dto.getAutoRenew().equals(originalContract.getAutoRenew())) {
            auditTrails.add(createAuditTrail(originalContract, "autoRenew",
                    originalContract.getAutoRenew() != null ? originalContract.getAutoRenew().toString() : null,
                    dto.getAutoRenew() != null ? dto.getAutoRenew().toString() : null, now,
                    changedBy, "UPDATE", "Cập nhật renew cho hợp đồng"));
            newContract.setAutoRenew(dto.getAutoRenew());
        }

        // Violate
        if (dto.getViolate() != null && !dto.getViolate().equals(originalContract.getViolate())) {
            auditTrails.add(createAuditTrail(originalContract, "violate",
                    originalContract.getViolate() != null ? originalContract.getViolate().toString() : null,
                    dto.getViolate() != null ? dto.getViolate().toString() : null, now,
                    changedBy, "UPDATE", "Cập nhật vi phạm cho hợp đồng"));
            newContract.setViolate(dto.getViolate());
        }

        // Suspend
        if (dto.getSuspend() != null && !dto.getSuspend().equals(originalContract.getSuspend())) {
            auditTrails.add(createAuditTrail(originalContract, "suspend",
                    originalContract.getSuspend() != null ? originalContract.getSuspend().toString() : null,
                    dto.getSuspend() != null ? dto.getSuspend().toString() : null, now,
                    changedBy, "UPDATE", "Cập nhật suspend cho hợp đồng"));
            newContract.setSuspend(dto.getSuspend());
        }

        // SuspendContent
        if (dto.getSuspendContent() != null && !dto.getSuspendContent().equals(originalContract.getSuspendContent())) {
            auditTrails.add(createAuditTrail(originalContract, "suspendContent", originalContract.getSuspendContent(),
                    dto.getSuspendContent(), now, changedBy, "UPDATE", "Cập nhật nội dung suspend cho hợp đồng"));
            newContract.setSuspendContent(dto.getSuspendContent());
        }

        // MaxDateLate
        if (dto.getMaxDateLate() != null && !dto.getMaxDateLate().equals(originalContract.getMaxDateLate())) {
            auditTrails.add(createAuditTrail(originalContract, "maxDateLate", String.valueOf(originalContract.getMaxDateLate()),
                    String.valueOf(dto.getMaxDateLate()), now, changedBy, "UPDATE", "Cập nhật số ngày trễ tối đa"));
            newContract.setMaxDateLate(dto.getMaxDateLate());
        }

        // Status
        if (dto.getStatus() != null && !dto.getStatus().equals(originalContract.getStatus())) {
            auditTrails.add(createAuditTrail(originalContract, "status", originalContract.getStatus().name(),
                    dto.getStatus().name(), now, changedBy, "UPDATE", "Cập nhật trạng thái hợp đồng"));
            newContract.setStatus(dto.getStatus());
        }
    }

    private void updateContractTerms(Contract originalContract, Contract newContract, ContractUpdateDTO dto,
                                     List<AuditTrail> auditTrails, LocalDateTime now, String changedBy) {
        List<ContractTerm> updatedTerms = new ArrayList<>();
        Set<Long> newTermIds = new HashSet<>();
        List<AuditTrail> termAuditTrails = new ArrayList<>();

        // Xử lý LegalBasisTerms
        if (dto.getLegalBasisTerms() != null) {
            for (TermSnapshotDTO termDTO : dto.getLegalBasisTerms()) {
                Term term = termRepository.findById(termDTO.getId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy Term với id: " + termDTO.getId()));
                if (!term.getTypeTerm().getIdentifier().equals(TypeTermIdentifier.LEGAL_BASIS)) {
                    throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại LEGAL_BASIS");
                }

                newTermIds.add(term.getId());
                ContractTerm existingTerm = originalContract.getContractTerms().stream()
                        .filter(t -> t.getOriginalTermId().equals(term.getId()) && t.getTermType().equals(TypeTermIdentifier.LEGAL_BASIS))
                        .findFirst()
                        .orElse(null);

                String oldValue = null;
                String newValue = String.format("Term ID: %d, Label: %s, Value: %s, Type: %s",
                        term.getId(), term.getLabel(), term.getValue(), TypeTermIdentifier.LEGAL_BASIS.name());

                ContractTerm newTerm;
                if (existingTerm != null) {
                    oldValue = String.format("Term ID: %d, Label: %s, Value: %s, Type: %s",
                            existingTerm.getOriginalTermId(), existingTerm.getTermLabel(), existingTerm.getTermValue(), existingTerm.getTermType().name());
                    newTerm = ContractTerm.builder()
                            .originalTermId(existingTerm.getOriginalTermId())
                            .termLabel(term.getLabel())
                            .termValue(term.getValue())
                            .termType(TypeTermIdentifier.LEGAL_BASIS)
                            .contract(newContract)
                            .build();
                    if (!oldValue.equals(newValue)) {
                        termAuditTrails.add(AuditTrail.builder()
                                .contract(newContract)
                                .entityName("ContractTerm")
                                .entityId(null)
                                .action("UPDATE")
                                .fieldName("legalBasisTerms")
                                .oldValue(oldValue)
                                .newValue(newValue)
                                .changedAt(now)
                                .changedBy(changedBy)
                                .changeSummary("Đã cập nhật điều khoản cơ sở pháp lý với Term ID: " + term.getId())
                                .build());
                    }
                } else {
                    newTerm = ContractTerm.builder()
                            .originalTermId(term.getId())
                            .termLabel(term.getLabel())
                            .termValue(term.getValue())
                            .termType(TypeTermIdentifier.LEGAL_BASIS)
                            .contract(newContract)
                            .build();
                    termAuditTrails.add(AuditTrail.builder()
                            .contract(newContract)
                            .entityName("ContractTerm")
                            .entityId(null)
                            .action("CREATE")
                            .fieldName("legalBasisTerms")
                            .oldValue(null)
                            .newValue(newValue)
                            .changedAt(now)
                            .changedBy(changedBy)
                            .changeSummary("Đã tạo điều khoản cơ sở pháp lý với Term ID: " + term.getId())
                            .build());
                }
                updatedTerms.add(newTerm);
            }
        }

        // Xử lý GeneralTerms
        if (dto.getGeneralTerms() != null) {
            for (TermSnapshotDTO termDTO : dto.getGeneralTerms()) {
                Term term = termRepository.findById(termDTO.getId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy Term với id: " + termDTO.getId()));
                if (!term.getTypeTerm().getIdentifier().equals(TypeTermIdentifier.GENERAL_TERMS)) {
                    throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại GENERAL_TERMS");
                }
                newTermIds.add(term.getId());
                ContractTerm existingTerm = originalContract.getContractTerms().stream()
                        .filter(t -> t.getOriginalTermId().equals(term.getId()) && t.getTermType().equals(TypeTermIdentifier.GENERAL_TERMS))
                        .findFirst()
                        .orElse(null);

                String oldValue = null;
                String newValue = String.format("Term ID: %d, Label: %s, Value: %s, Type: %s",
                        term.getId(), term.getLabel(), term.getValue(), TypeTermIdentifier.GENERAL_TERMS.name());

                ContractTerm newTerm;
                if (existingTerm != null) {
                    oldValue = String.format("Term ID: %d, Label: %s, Value: %s, Type: %s",
                            existingTerm.getOriginalTermId(), existingTerm.getTermLabel(), existingTerm.getTermValue(), existingTerm.getTermType().name());
                    newTerm = ContractTerm.builder()
                            .originalTermId(existingTerm.getOriginalTermId())
                            .termLabel(term.getLabel())
                            .termValue(term.getValue())
                            .termType(TypeTermIdentifier.GENERAL_TERMS)
                            .contract(newContract)
                            .build();
                    if (!oldValue.equals(newValue)) {
                        termAuditTrails.add(AuditTrail.builder()
                                .contract(newContract)
                                .entityName("ContractTerm")
                                .entityId(null)
                                .action("UPDATE")
                                .fieldName("generalTerms")
                                .oldValue(oldValue)
                                .newValue(newValue)
                                .changedAt(now)
                                .changedBy(changedBy)
                                .changeSummary("Đã cập nhật điều khoản chung với Term ID: " + term.getId())
                                .build());
                    }
                } else {
                    newTerm = ContractTerm.builder()
                            .originalTermId(term.getId())
                            .termLabel(term.getLabel())
                            .termValue(term.getValue())
                            .termType(TypeTermIdentifier.GENERAL_TERMS)
                            .contract(newContract)
                            .build();
                    termAuditTrails.add(AuditTrail.builder()
                            .contract(newContract)
                            .entityName("ContractTerm")
                            .entityId(null)
                            .action("CREATE")
                            .fieldName("generalTerms")
                            .oldValue(null)
                            .newValue(newValue)
                            .changedAt(now)
                            .changedBy(changedBy)
                            .changeSummary("Đã tạo điều khoản chung với Term ID: " + term.getId())
                            .build());
                }
                updatedTerms.add(newTerm);
            }
        }

        // Xử lý OtherTerms
        if (dto.getOtherTerms() != null) {
            for (TermSnapshotDTO termDTO : dto.getOtherTerms()) {
                Term term = termRepository.findById(termDTO.getId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy Term với id: " + termDTO.getId()));
                if (!term.getTypeTerm().getIdentifier().equals(TypeTermIdentifier.OTHER_TERMS)) {
                    throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại OTHER_TERMS");
                }
                newTermIds.add(term.getId());
                ContractTerm existingTerm = originalContract.getContractTerms().stream()
                        .filter(t -> t.getOriginalTermId().equals(term.getId()) && t.getTermType().equals(TypeTermIdentifier.OTHER_TERMS))
                        .findFirst()
                        .orElse(null);

                String oldValue = null;
                String newValue = String.format("Term ID: %d, Label: %s, Value: %s, Type: %s",
                        term.getId(), term.getLabel(), term.getValue(), TypeTermIdentifier.OTHER_TERMS.name());

                ContractTerm newTerm;
                if (existingTerm != null) {
                    oldValue = String.format("Term ID: %d, Label: %s, Value: %s, Type: %s",
                            existingTerm.getOriginalTermId(), existingTerm.getTermLabel(), existingTerm.getTermValue(), existingTerm.getTermType().name());
                    newTerm = ContractTerm.builder()
                            .originalTermId(existingTerm.getOriginalTermId())
                            .termLabel(term.getLabel())
                            .termValue(term.getValue())
                            .termType(TypeTermIdentifier.OTHER_TERMS)
                            .contract(newContract)
                            .build();
                    if (!oldValue.equals(newValue)) {
                        termAuditTrails.add(AuditTrail.builder()
                                .contract(newContract)
                                .entityName("ContractTerm")
                                .entityId(null)
                                .action("UPDATE")
                                .fieldName("otherTerms")
                                .oldValue(oldValue)
                                .newValue(newValue)
                                .changedAt(now)
                                .changedBy(changedBy)
                                .changeSummary("Đã cập nhật điều khoản khác với Term ID: " + term.getId())
                                .build());
                    }
                } else {
                    newTerm = ContractTerm.builder()
                            .originalTermId(term.getId())
                            .termLabel(term.getLabel())
                            .termValue(term.getValue())
                            .termType(TypeTermIdentifier.OTHER_TERMS)
                            .contract(newContract)
                            .build();
                    termAuditTrails.add(AuditTrail.builder()
                            .contract(newContract)
                            .entityName("ContractTerm")
                            .entityId(null)
                            .action("CREATE")
                            .fieldName("otherTerms")
                            .oldValue(null)
                            .newValue(newValue)
                            .changedAt(now)
                            .changedBy(changedBy)
                            .changeSummary("Đã tạo điều khoản khác với Term ID: " + term.getId())
                            .build());
                }
                updatedTerms.add(newTerm);
            }
        }

        // Kiểm tra term bị xóa
        for (ContractTerm oldTerm : originalContract.getContractTerms()) {
            if (!newTermIds.contains(oldTerm.getOriginalTermId())) {
                String oldValue = String.format("Term ID: %d, Label: %s, Value: %s, Type: %s",
                        oldTerm.getOriginalTermId(), oldTerm.getTermLabel(),
                        oldTerm.getTermValue(), oldTerm.getTermType().name());
                String fieldName = switch (oldTerm.getTermType()) {
                    case LEGAL_BASIS -> "legalBasisTerms";
                    case GENERAL_TERMS -> "generalTerms";
                    case OTHER_TERMS -> "otherTerms";
                    default -> "contractTerms";
                };
                String changeSummary = switch (fieldName) {
                    case "legalBasisTerms" -> "Đã xóa điều khoản cơ sở pháp lý với Term ID: " + oldTerm.getOriginalTermId();
                    case "generalTerms" -> "Đã xóa điều khoản chung với Term ID: " + oldTerm.getOriginalTermId();
                    case "otherTerms" -> "Đã xóa điều khoản khác với Term ID: " + oldTerm.getOriginalTermId();
                    default -> "Đã xóa điều khoản hợp đồng với Term ID: " + oldTerm.getOriginalTermId();
                };
                termAuditTrails.add(AuditTrail.builder()
                        .contract(newContract)
                        .entityName("ContractTerm")
                        .entityId(null)
                        .action("DELETE")
                        .fieldName(fieldName)
                        .oldValue(oldValue)
                        .newValue(null)
                        .changedAt(now)
                        .changedBy(changedBy)
                        .changeSummary(changeSummary)
                        .build());
            } else {
                if (updatedTerms.stream().noneMatch(
                        t -> t.getOriginalTermId().equals(oldTerm.getOriginalTermId()) && t.getTermType().equals(oldTerm.getTermType()))) {
                    ContractTerm newTerm = ContractTerm.builder()
                            .originalTermId(oldTerm.getOriginalTermId())
                            .termLabel(oldTerm.getTermLabel())
                            .termValue(oldTerm.getTermValue())
                            .termType(oldTerm.getTermType())
                            .contract(newContract)
                            .build();
                    updatedTerms.add(newTerm);
                }
            }
        }

        newContract.setContractTerms(updatedTerms);
        auditTrails.addAll(termAuditTrails);
    }

    private void updateContractAdditionalTermDetails(Contract originalContract, Contract newContract, ContractUpdateDTO dto,
                                                     List<AuditTrail> auditTrails, LocalDateTime now, String changedBy) {
        if (dto.getAdditionalConfig() == null) return;

        List<ContractAdditionalTermDetail> updatedDetails = new ArrayList<>();
        List<AuditTrail> additionalTermAuditTrails = new ArrayList<>();
        Map<String, Map<String, List<TermSnapshotDTO>>> configMap = dto.getAdditionalConfig();
        Set<Long> newTypeTermIds = configMap.keySet().stream()
                .map(Long::parseLong)
                .collect(Collectors.toSet());

        record AuditTrailPending(
                ContractAdditionalTermDetail detail,
                String oldValue,
                String action,
                Long typeTermId
        ) {}

        List<AuditTrailPending> pendingAudits = new ArrayList<>();

        for (Map.Entry<String, Map<String, List<TermSnapshotDTO>>> entry : configMap.entrySet()) {
            Long configTypeTermId = Long.parseLong(entry.getKey());
            Map<String, List<TermSnapshotDTO>> groupConfig = entry.getValue();

            List<AdditionalTermSnapshot> commonSnapshots = new ArrayList<>();
            List<AdditionalTermSnapshot> aSnapshots = new ArrayList<>();
            List<AdditionalTermSnapshot> bSnapshots = new ArrayList<>();

            if (groupConfig.containsKey("Common")) {
                commonSnapshots = groupConfig.get("Common").stream()
                        .map(termDTO -> {
                            Term term = termRepository.findById(termDTO.getId())
                                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Term với id: " + termDTO.getId()));
                            return AdditionalTermSnapshot.builder()
                                    .termId(term.getId())
                                    .termLabel(term.getLabel())
                                    .termValue(term.getValue())
                                    .build();
                        })
                        .collect(Collectors.toList());
            }

            if (groupConfig.containsKey("A")) {
                aSnapshots = groupConfig.get("A").stream()
                        .map(termDTO -> {
                            Term term = termRepository.findById(termDTO.getId())
                                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Term với id: " + termDTO.getId()));
                            return AdditionalTermSnapshot.builder()
                                    .termId(term.getId())
                                    .termLabel(term.getLabel())
                                    .termValue(term.getValue())
                                    .build();
                        })
                        .collect(Collectors.toList());
            }

            if (groupConfig.containsKey("B")) {
                bSnapshots = groupConfig.get("B").stream()
                        .map(termDTO -> {
                            Term term = termRepository.findById(termDTO.getId())
                                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Term với id: " + termDTO.getId()));
                            return AdditionalTermSnapshot.builder()
                                    .termId(term.getId())
                                    .termLabel(term.getLabel())
                                    .termValue(term.getValue())
                                    .build();
                        })
                        .collect(Collectors.toList());
            }

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

            // Tạo mới hoặc sao chép từ originalContract
            ContractAdditionalTermDetail detail = originalContract.getAdditionalTermDetails().stream()
                    .filter(d -> d.getTypeTermId().equals(configTypeTermId))
                    .findFirst()
                    .map(oldDetail -> ContractAdditionalTermDetail.builder()
                            .contract(newContract)
                            .typeTermId(oldDetail.getTypeTermId())
                            .commonTerms(new ArrayList<>(oldDetail.getCommonTerms())) // Sao chép thủ công
                            .aTerms(new ArrayList<>(oldDetail.getATerms()))
                            .bTerms(new ArrayList<>(oldDetail.getBTerms()))
                            .build())
                    .orElseGet(() -> ContractAdditionalTermDetail.builder()
                            .contract(newContract)
                            .typeTermId(configTypeTermId)
                            .build());

            // Cập nhật giá trị từ DTO
            detail.setCommonTerms(commonSnapshots);
            detail.setATerms(aSnapshots);
            detail.setBTerms(bSnapshots);

            String oldValue = null;
            String newValue;
            try {
                newValue = objectMapper.writeValueAsString(detail);
                if (originalContract.getAdditionalTermDetails().stream().anyMatch(d -> d.getTypeTermId().equals(configTypeTermId))) {
                    ContractAdditionalTermDetail oldDetail = originalContract.getAdditionalTermDetails().stream()
                            .filter(d -> d.getTypeTermId().equals(configTypeTermId))
                            .findFirst().get();
                    oldValue = objectMapper.writeValueAsString(oldDetail);
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize ContractAdditionalTermDetail to JSON", e);
            }

            if (oldValue == null) {
                pendingAudits.add(new AuditTrailPending(detail, null, "CREATE", configTypeTermId));
            } else if (!Objects.equals(oldValue, newValue)) {
                pendingAudits.add(new AuditTrailPending(detail, oldValue, "UPDATE", configTypeTermId));
            }

            updatedDetails.add(detail);
        }

        // Xử lý các chi tiết bị xóa
        for (ContractAdditionalTermDetail oldDetail : originalContract.getAdditionalTermDetails()) {
            if (!newTypeTermIds.contains(oldDetail.getTypeTermId())) {
                String oldValue;
                try {
                    oldValue = objectMapper.writeValueAsString(oldDetail);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to serialize old ContractAdditionalTermDetail to JSON", e);
                }
                additionalTermAuditTrails.add(AuditTrail.builder()
                        .contract(newContract)
                        .entityName("ContractAdditionalTermDetail")
                        .entityId(null)
                        .action("DELETE")
                        .fieldName("additionalTermDetails")
                        .oldValue(oldValue)
                        .newValue(null)
                        .changedAt(now)
                        .changedBy(changedBy)
                        .changeSummary("Đã xóa chi tiết điều khoản bổ sung với TypeTerm ID: " + oldDetail.getTypeTermId())
                        .build());
            } else {
                if (updatedDetails.stream().noneMatch(d -> d.getTypeTermId().equals(oldDetail.getTypeTermId()))) {
                    ContractAdditionalTermDetail newDetail = ContractAdditionalTermDetail.builder()
                            .contract(newContract)
                            .typeTermId(oldDetail.getTypeTermId())
                            .commonTerms(new ArrayList<>(oldDetail.getCommonTerms()))
                            .aTerms(new ArrayList<>(oldDetail.getATerms()))
                            .bTerms(new ArrayList<>(oldDetail.getBTerms()))
                            .build();
                    updatedDetails.add(newDetail);
                }
            }
        }

        newContract.setAdditionalTermDetails(updatedDetails);
        auditTrails.addAll(additionalTermAuditTrails);

        // Xử lý audit trail sau khi lưu để có ID
        if (!pendingAudits.isEmpty()) {
            Contract savedContract = contractRepository.save(newContract); // Lưu để sinh ID
            for (AuditTrailPending pending : pendingAudits) {
                ContractAdditionalTermDetail savedDetail = savedContract.getAdditionalTermDetails().stream()
                        .filter(d -> d.getTypeTermId().equals(pending.typeTermId()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy ContractAdditionalTermDetail vừa tạo"));
                String updatedNewValue;
                try {
                    updatedNewValue = objectMapper.writeValueAsString(savedDetail);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to serialize updated ContractAdditionalTermDetail to JSON", e);
                }
                additionalTermAuditTrails.add(AuditTrail.builder()
                        .contract(savedContract)
                        .entityName("ContractAdditionalTermDetail")
                        .entityId(savedDetail.getId())
                        .action(pending.action())
                        .fieldName("additionalTermDetails")
                        .oldValue(pending.oldValue())
                        .newValue(updatedNewValue)
                        .changedAt(now)
                        .changedBy(changedBy)
                        .changeSummary(pending.action().equals("CREATE") ?
                                "Đã tạo chi tiết điều khoản bổ sung với TypeTerm ID: " + pending.typeTermId() :
                                "Đã cập nhật chi tiết điều khoản bổ sung với TypeTerm ID: " + pending.typeTermId())
                        .build());
            }
            auditTrails.addAll(additionalTermAuditTrails);
        }
    }
    private void updatePaymentSchedules(Contract originalContract, Contract newContract, ContractUpdateDTO dto,
                                        List<AuditTrail> auditTrails, LocalDateTime now, String changedBy) {
        if (dto.getPayments() == null) return;

        List<PaymentSchedule> updatedPayments = new ArrayList<>();
        List<AuditTrail> paymentAuditTrails = new ArrayList<>();
        Set<Long> newPaymentIds = dto.getPayments().stream()
                .filter(p -> p.getId() != null)
                .map(PaymentScheduleDTO::getId)
                .collect(Collectors.toSet());

        for (PaymentScheduleDTO paymentDTO : dto.getPayments()) {
            PaymentSchedule payment;
            String oldValue = null;
            String newValue;
            String action;

            if (paymentDTO.getId() != null) {
                payment = originalContract.getPaymentSchedules().stream()
                        .filter(p -> p.getId().equals(paymentDTO.getId()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy PaymentSchedule với id: " + paymentDTO.getId()));
                oldValue = serializePaymentSchedule(payment);
                action = "UPDATE";
                payment = PaymentSchedule.builder()
                        .contract(newContract)
                        .paymentOrder(payment.getPaymentOrder())
                        .amount(payment.getAmount())
                        .notifyPaymentDate(payment.getNotifyPaymentDate())
                        .paymentDate(payment.getPaymentDate())
                        .status(payment.getStatus())
                        .paymentMethod(payment.getPaymentMethod())
                        .notifyPaymentContent(payment.getNotifyPaymentContent())
                        .reminderEmailSent(payment.isReminderEmailSent())
                        .overdueEmailSent(payment.isOverdueEmailSent())
                        .build();
            } else {
                payment = new PaymentSchedule();
                payment.setContract(newContract);
                action = "CREATE";
            }

            payment.setPaymentOrder(paymentDTO.getPaymentOrder());
            payment.setAmount(paymentDTO.getAmount());
            payment.setNotifyPaymentDate(paymentDTO.getNotifyPaymentDate());
            payment.setPaymentDate(paymentDTO.getPaymentDate());
            payment.setStatus(paymentDTO.getStatus());
            payment.setPaymentMethod(paymentDTO.getPaymentMethod());
            payment.setNotifyPaymentContent(paymentDTO.getNotifyPaymentContent());
            payment.setReminderEmailSent(paymentDTO.isReminderEmailSent());
            payment.setOverdueEmailSent(paymentDTO.isOverdueEmailSent());

            newValue = serializePaymentSchedule(payment);

            if (!Objects.equals(oldValue, newValue)) {
                paymentAuditTrails.add(AuditTrail.builder()
                        .contract(newContract)
                        .entityName("PaymentSchedule")
                        .entityId(null)
                        .action(action)
                        .fieldName("paymentSchedules")
                        .oldValue(oldValue)
                        .newValue(newValue)
                        .changedAt(now)
                        .changedBy(changedBy)
                        .changeSummary(action.equals("CREATE") ? "Đã tạo PaymentSchedule mới" : "Đã cập nhật PaymentSchedule")
                        .build());
            }
            updatedPayments.add(payment);
        }

        for (PaymentSchedule oldPayment : originalContract.getPaymentSchedules()) {
            if (!newPaymentIds.contains(oldPayment.getId())) {
                String oldValue = serializePaymentSchedule(oldPayment);
                paymentAuditTrails.add(AuditTrail.builder()
                        .contract(newContract)
                        .entityName("PaymentSchedule")
                        .entityId(null)
                        .action("DELETE")
                        .fieldName("paymentSchedules")
                        .oldValue(oldValue)
                        .newValue(null)
                        .changedAt(now)
                        .changedBy(changedBy)
                        .changeSummary("Đã xóa PaymentSchedule")
                        .build());
            } else {
                if (updatedPayments.stream().noneMatch(p -> p.getId() != null && p.getId().equals(oldPayment.getId()))) {
                    PaymentSchedule newPayment = PaymentSchedule.builder()
                            .contract(newContract)
                            .paymentOrder(oldPayment.getPaymentOrder())
                            .amount(oldPayment.getAmount())
                            .notifyPaymentDate(oldPayment.getNotifyPaymentDate())
                            .paymentDate(oldPayment.getPaymentDate())
                            .status(oldPayment.getStatus())
                            .paymentMethod(oldPayment.getPaymentMethod())
                            .notifyPaymentContent(oldPayment.getNotifyPaymentContent())
                            .reminderEmailSent(oldPayment.isReminderEmailSent())
                            .overdueEmailSent(oldPayment.isOverdueEmailSent())
                            .build();
                    updatedPayments.add(newPayment);
                }
            }
        }

        newContract.setPaymentSchedules(updatedPayments);
        auditTrails.addAll(paymentAuditTrails);
    }

    private String serializeContract(Contract contract) {
        return "Contract{id=" + contract.getId() + ", contractNumber=" + contract.getContractNumber() + "}";
    }

    private String serializePaymentSchedule(PaymentSchedule payment) {
        if (payment == null) return null;
        String idStr = payment.getId() != null ? payment.getId().toString() : "null";
        String orderStr = payment.getPaymentOrder() != null ? payment.getPaymentOrder().toString() : "null";
        String amountStr = payment.getAmount() != null ? String.format("%.2f", payment.getAmount()) : "null";
        String notifyDateStr = payment.getNotifyPaymentDate() != null ? payment.getNotifyPaymentDate().toString() : "null";
        String paymentDateStr = payment.getPaymentDate() != null ? payment.getPaymentDate().toString() : "null";
        String statusStr = payment.getStatus() != null ? payment.getStatus().name() : "null";
        String methodStr = payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "null";
        String contentStr = payment.getNotifyPaymentContent() != null ? payment.getNotifyPaymentContent() : "null";
        String reminderStr = String.valueOf(payment.isReminderEmailSent());
        String overdueStr = String.valueOf(payment.isOverdueEmailSent());

        return String.format("PaymentSchedule ID: %s, Order: %s, Amount: %s, NotifyPaymentDate: %s, PaymentDate: %s, Status: %s, PaymentMethod: %s, NotifyPaymentContent: %s, ReminderEmailSent: %s, OverdueEmailSent: %s",
                idStr, orderStr, amountStr, notifyDateStr, paymentDateStr, statusStr, methodStr, contentStr, reminderStr, overdueStr);
    }

    private Integer getPaymentOrderFromNewValue(String newValue) {
        if (newValue == null) return null;
        String[] parts = newValue.split(", ");
        for (String part : parts) {
            if (part.startsWith("Order: ")) {
                return Integer.parseInt(part.substring(7));
            }
        }
        throw new RuntimeException("Không thể trích xuất paymentOrder từ newValue: " + newValue);
    }

    private Long getTermIdFromNewValue(String newValue) {
        String[] parts = newValue.split(",")[0].split(":");
        return Long.parseLong(parts[1].trim());
    }

    private AuditTrail createAuditTrail(Contract contract, String fieldName, String oldValue, String newValue,
                                        LocalDateTime changedAt, String changedBy, String action, String changeSummary) {
        AuditTrail auditTrail = new AuditTrail();
        auditTrail.setEntityName("Contract");
        auditTrail.setEntityId(contract.getId());
        auditTrail.setContract(contract);
        auditTrail.setAction(action);
        auditTrail.setFieldName(fieldName);
        auditTrail.setOldValue(oldValue);
        auditTrail.setNewValue(newValue);
        auditTrail.setChangedAt(changedAt);
        auditTrail.setChangedBy(changedBy);
        auditTrail.setChangeSummary(changeSummary);
        if (auditTrail.getContract() == null || auditTrail.getContract().getId() == null) {
            throw new IllegalStateException("Contract in AuditTrail is null or has no ID");
        }
        return auditTrail;
    }

    @Transactional
    @Override
    public boolean softDelete(Long id) {
        Optional<Contract> optionalContract = contractRepository.findById(id);
        if (optionalContract.isEmpty()) {
            return false;
        }

        Contract contract = optionalContract.get();
        ContractStatus currentStatus = contract.getStatus();

        // Kiểm tra trạng thái hiện tại có thể chuyển sang DELETED không
        if (!isValidTransition(currentStatus, ContractStatus.DELETED)) {
            throw new IllegalStateException(
                    String.format("Không thể xóa mềm hợp đồng từ trạng thái %s", currentStatus.name())
            );
        }

        // Cập nhật trạng thái
        String oldValue = currentStatus.name();
        contract.setStatus(ContractStatus.DELETED);
        contract.setUpdatedAt(LocalDateTime.now());

        // Lưu hợp đồng
        Contract savedContract = contractRepository.save(contract);

        // Ghi log vào audit trail
        AuditTrail auditTrail = AuditTrail.builder()
                .contract(savedContract)
                .entityName("Contract")
                .entityId(savedContract.getId())
                .action("UPDATE")
                .fieldName("status")
                .oldValue(oldValue)
                .newValue(ContractStatus.DELETED.name())
                .changedAt(LocalDateTime.now())
                .changedBy(currentUser.getLoggedInUser().getFullName())
                .changeSummary(String.format("Đã xóa mềm hợp đồng từ trạng thái %s sang %s", oldValue, ContractStatus.DELETED.name()))
                .build();

        auditTrailRepository.save(auditTrail);

        return true;
    }

    private static final Map<ContractStatus, EnumSet<ContractStatus>> VALID_TRANSITIONS = new HashMap<>();

    static {
        VALID_TRANSITIONS.put(ContractStatus.DRAFT, EnumSet.of(ContractStatus.CREATED, ContractStatus.DELETED));
        VALID_TRANSITIONS.put(ContractStatus.CREATED, EnumSet.of(ContractStatus.APPROVAL_PENDING, ContractStatus.DELETED));
        VALID_TRANSITIONS.put(ContractStatus.APPROVAL_PENDING, EnumSet.of(ContractStatus.APPROVED, ContractStatus.REJECTED, ContractStatus.DELETED));
        VALID_TRANSITIONS.put(ContractStatus.APPROVED, EnumSet.of(ContractStatus.PENDING, ContractStatus.REJECTED, ContractStatus.DELETED));
        VALID_TRANSITIONS.put(ContractStatus.PENDING, EnumSet.of(ContractStatus.SIGNED, ContractStatus.CANCELLED, ContractStatus.DELETED));
        VALID_TRANSITIONS.put(ContractStatus.SIGNED, EnumSet.of(ContractStatus.ACTIVE, ContractStatus.CANCELLED, ContractStatus.DELETED));
        VALID_TRANSITIONS.put(ContractStatus.ACTIVE, EnumSet.of(ContractStatus.COMPLETED, ContractStatus.EXPIRED, ContractStatus.CANCELLED, ContractStatus.ENDED, ContractStatus.DELETED));
        VALID_TRANSITIONS.put(ContractStatus.COMPLETED, EnumSet.of(ContractStatus.ENDED, ContractStatus.DELETED));
        VALID_TRANSITIONS.put(ContractStatus.EXPIRED, EnumSet.of(ContractStatus.ENDED, ContractStatus.DELETED));
        VALID_TRANSITIONS.put(ContractStatus.CANCELLED, EnumSet.noneOf(ContractStatus.class));
        VALID_TRANSITIONS.put(ContractStatus.ENDED, EnumSet.noneOf(ContractStatus.class));
        VALID_TRANSITIONS.put(ContractStatus.DELETED, EnumSet.of(ContractStatus.DRAFT));
        VALID_TRANSITIONS.put(ContractStatus.REJECTED, EnumSet.noneOf(ContractStatus.class));
    }

    @Transactional
    public ContractStatus updateContractStatus(Long contractId, ContractStatus newStatus) {
        // Tìm hợp đồng
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng với id: " + contractId));

        // Kiểm tra trạng thái hiện tại và trạng thái mới có hợp lệ không
        ContractStatus currentStatus = contract.getStatus();

        if (!isValidTransition(currentStatus, newStatus)) {
            throw new IllegalArgumentException(
                    String.format("Không thể chuyển trạng thái từ %s sang %s", currentStatus.name(), newStatus.name())
            );
        }

        // Cập nhật trạng thái
        String oldValue = currentStatus.name();
        contract.setStatus(newStatus);
        contract.setUpdatedAt(LocalDateTime.now());

        // Lưu hợp đồng
        Contract savedContract = contractRepository.save(contract);

        // Ghi log vào audit trail
        AuditTrail auditTrail = AuditTrail.builder()
                .contract(savedContract)
                .entityName("Contract")
                .entityId(savedContract.getId())
                .action("UPDATE")
                .fieldName("status")
                .oldValue(oldValue)
                .newValue(newStatus.name())
                .changedAt(LocalDateTime.now())
                .changedBy(currentUser.getLoggedInUser().getFullName())
                .changeSummary(String.format("Đã cập nhật trạng thái hợp đồng từ %s sang %s", oldValue, newStatus.name()))
                .build();

        auditTrailRepository.save(auditTrail);

        return savedContract.getStatus();
    }

    private boolean isValidTransition(ContractStatus fromStatus, ContractStatus toStatus) {
        if (fromStatus == toStatus) {
            return false; // Không cho phép cập nhật cùng trạng thái
        }
        return VALID_TRANSITIONS.getOrDefault(fromStatus, EnumSet.noneOf(ContractStatus.class)).contains(toStatus);
    }


}

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
    private final IContractTermRepository contractTermRepository;
    private final IPaymentScheduleRepository paymentScheduleRepository;

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
                .version(1)
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
                .version(1)
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

    @Transactional
    public Contract updateContract(Long contractId, ContractUpdateDTO dto) {
        // 1. Tìm hợp đồng hiện tại
        Contract currentContract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng với id: " + contractId));

        List<AuditTrail> auditTrails = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        String changedBy = currentUser.getLoggedInUser().getFullName();

        // 2. Xác định hợp đồng gốc và tính toán phiên bản mới
        Long originalContractId = currentContract.getOriginalContractId() != null
                ? currentContract.getOriginalContractId()
                : currentContract.getId();
        int newVersion = calculateNewVersion(originalContractId, currentContract);

        // 3. Tạo hợp đồng mới với các giá trị từ currentContract và cập nhật từ DTO
        Contract newContract = Contract.builder()
                .originalContractId(originalContractId)
                .version(newVersion)
                .signingDate(dto.getSigningDate() != null ? dto.getSigningDate() : currentContract.getSigningDate())
                .contractLocation(dto.getContractLocation() != null ? dto.getContractLocation() : currentContract.getContractLocation())
                .contractNumber(currentContract.getContractNumber() + "-v" + newVersion)
                .specialTermsA(dto.getSpecialTermsA() != null ? dto.getSpecialTermsA() : currentContract.getSpecialTermsA())
                .specialTermsB(dto.getSpecialTermsB() != null ? dto.getSpecialTermsB() : currentContract.getSpecialTermsB())
                .status(dto.getStatus() != null ? dto.getStatus() : currentContract.getStatus())
                .createdAt(now)
                .updatedAt(now)
                .effectiveDate(dto.getEffectiveDate() != null ? dto.getEffectiveDate() : currentContract.getEffectiveDate())
                .expiryDate(dto.getExpiryDate() != null ? dto.getExpiryDate() : currentContract.getExpiryDate())
                .notifyEffectiveDate(dto.getNotifyEffectiveDate() != null ? dto.getNotifyEffectiveDate() : currentContract.getNotifyEffectiveDate())
                .notifyExpiryDate(dto.getNotifyExpiryDate() != null ? dto.getNotifyExpiryDate() : currentContract.getNotifyExpiryDate())
                .notifyEffectiveContent(dto.getNotifyEffectiveContent() != null ? dto.getNotifyEffectiveContent() : currentContract.getNotifyEffectiveContent())
                .notifyExpiryContent(dto.getNotifyExpiryContent() != null ? dto.getNotifyExpiryContent() : currentContract.getNotifyExpiryContent())
                .title(dto.getTitle() != null ? dto.getTitle() : currentContract.getTitle())
                .amount(dto.getAmount() != null ? dto.getAmount() : currentContract.getAmount())
                .user(currentContract.getUser())
                .isDateLateChecked(dto.getIsDateLateChecked() != null ? dto.getIsDateLateChecked() : currentContract.getIsDateLateChecked())
                .template(currentContract.getTemplate())
                .party(currentContract.getParty())
                .appendixEnabled(dto.getAppendixEnabled() != null ? dto.getAppendixEnabled() : currentContract.getAppendixEnabled())
                .transferEnabled(dto.getTransferEnabled() != null ? dto.getTransferEnabled() : currentContract.getTransferEnabled())
                .autoAddVAT(dto.getAutoAddVAT() != null ? dto.getAutoAddVAT() : currentContract.getAutoAddVAT())
                .vatPercentage(dto.getVatPercentage() != null ? dto.getVatPercentage() : currentContract.getVatPercentage())
                .autoRenew(dto.getAutoRenew() != null ? dto.getAutoRenew() : currentContract.getAutoRenew())
                .violate(dto.getViolate() != null ? dto.getViolate() : currentContract.getViolate())
                .suspend(dto.getSuspend() != null ? dto.getSuspend() : currentContract.getSuspend())
                .suspendContent(dto.getSuspendContent() != null ? dto.getSuspendContent() : currentContract.getSuspendContent())
                .contractContent(dto.getContractContent() != null ? dto.getContractContent() : currentContract.getContractContent())
                .approvalWorkflow(currentContract.getApprovalWorkflow())
                .maxDateLate(dto.getMaxDateLate() != null ? dto.getMaxDateLate() : currentContract.getMaxDateLate())
                .contractType(currentContract.getContractType())
                .build();

        Contract savedNewContract = contractRepository.save(newContract);


        // 5. Xử lý ContractTerm
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
                ContractTerm existingTerm = currentContract.getContractTerms().stream()
                        .filter(t -> t.getOriginalTermId().equals(term.getId()) && t.getTermType().equals(TypeTermIdentifier.LEGAL_BASIS))
                        .findFirst()
                        .orElse(null);
                String oldValue = null;
                String newValue = String.format("Term ID: %d, Label: %s, Value: %s, Type: %s",
                        term.getId(), term.getLabel(), term.getValue(), TypeTermIdentifier.LEGAL_BASIS.name());
                if (existingTerm != null) {
                    oldValue = String.format("Term ID: %d, Label: %s, Value: %s, Type: %s",
                            existingTerm.getOriginalTermId(), existingTerm.getTermLabel(), existingTerm.getTermValue(), existingTerm.getTermType().name());
                    if (!oldValue.equals(newValue)) {
                        ContractTerm newTerm = ContractTerm.builder()
                                .originalTermId(term.getId())
                                .termLabel(term.getLabel())
                                .termValue(term.getValue())
                                .termType(TypeTermIdentifier.LEGAL_BASIS)
                                .contract(newContract)
                                .build();
                        updatedTerms.add(newTerm);
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
                    } else {
                        ContractTerm newTerm = ContractTerm.builder()
                                .originalTermId(existingTerm.getOriginalTermId())
                                .termLabel(existingTerm.getTermLabel())
                                .termValue(existingTerm.getTermValue())
                                .termType(existingTerm.getTermType())
                                .contract(newContract)
                                .build();
                        updatedTerms.add(newTerm);
                    }
                } else {
                    ContractTerm newTerm = ContractTerm.builder()
                            .originalTermId(term.getId())
                            .termLabel(term.getLabel())
                            .termValue(term.getValue())
                            .termType(TypeTermIdentifier.LEGAL_BASIS)
                            .contract(newContract)
                            .build();
                    updatedTerms.add(newTerm);
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
                ContractTerm existingTerm = currentContract.getContractTerms().stream()
                        .filter(t -> t.getOriginalTermId().equals(term.getId()) && t.getTermType().equals(TypeTermIdentifier.GENERAL_TERMS))
                        .findFirst()
                        .orElse(null);
                String oldValue = null;
                String newValue = String.format("Term ID: %d, Label: %s, Value: %s, Type: %s",
                        term.getId(), term.getLabel(), term.getValue(), TypeTermIdentifier.GENERAL_TERMS.name());
                if (existingTerm != null) {
                    oldValue = String.format("Term ID: %d, Label: %s, Value: %s, Type: %s",
                            existingTerm.getOriginalTermId(), existingTerm.getTermLabel(), existingTerm.getTermValue(), existingTerm.getTermType().name());
                    if (!oldValue.equals(newValue)) {
                        ContractTerm newTerm = ContractTerm.builder()
                                .originalTermId(term.getId())
                                .termLabel(term.getLabel())
                                .termValue(term.getValue())
                                .termType(TypeTermIdentifier.GENERAL_TERMS)
                                .contract(newContract)
                                .build();
                        updatedTerms.add(newTerm);
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
                    } else {
                        ContractTerm newTerm = ContractTerm.builder()
                                .originalTermId(existingTerm.getOriginalTermId())
                                .termLabel(existingTerm.getTermLabel())
                                .termValue(existingTerm.getTermValue())
                                .termType(existingTerm.getTermType())
                                .contract(newContract)
                                .build();
                        updatedTerms.add(newTerm);
                    }
                } else {
                    ContractTerm newTerm = ContractTerm.builder()
                            .originalTermId(term.getId())
                            .termLabel(term.getLabel())
                            .termValue(term.getValue())
                            .termType(TypeTermIdentifier.GENERAL_TERMS)
                            .contract(newContract)
                            .build();
                    updatedTerms.add(newTerm);
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
                ContractTerm existingTerm = currentContract.getContractTerms().stream()
                        .filter(t -> t.getOriginalTermId().equals(term.getId()) && t.getTermType().equals(TypeTermIdentifier.OTHER_TERMS))
                        .findFirst()
                        .orElse(null);
                String oldValue = null;
                String newValue = String.format("Term ID: %d, Label: %s, Value: %s, Type: %s",
                        term.getId(), term.getLabel(), term.getValue(), TypeTermIdentifier.OTHER_TERMS.name());
                if (existingTerm != null) {
                    oldValue = String.format("Term ID: %d, Label: %s, Value: %s, Type: %s",
                            existingTerm.getOriginalTermId(), existingTerm.getTermLabel(), existingTerm.getTermValue(), existingTerm.getTermType().name());
                    if (!oldValue.equals(newValue)) {
                        ContractTerm newTerm = ContractTerm.builder()
                                .originalTermId(term.getId())
                                .termLabel(term.getLabel())
                                .termValue(term.getValue())
                                .termType(TypeTermIdentifier.OTHER_TERMS)
                                .contract(newContract)
                                .build();
                        updatedTerms.add(newTerm);
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
                    } else {
                        ContractTerm newTerm = ContractTerm.builder()
                                .originalTermId(existingTerm.getOriginalTermId())
                                .termLabel(existingTerm.getTermLabel())
                                .termValue(existingTerm.getTermValue())
                                .termType(existingTerm.getTermType())
                                .contract(newContract)
                                .build();
                        updatedTerms.add(newTerm);
                    }
                } else {
                    ContractTerm newTerm = ContractTerm.builder()
                            .originalTermId(term.getId())
                            .termLabel(term.getLabel())
                            .termValue(term.getValue())
                            .termType(TypeTermIdentifier.OTHER_TERMS)
                            .contract(newContract)
                            .build();
                    updatedTerms.add(newTerm);
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
            }
        }

        // Xử lý các term cũ không có trong DTO (DELETE)
        for (ContractTerm oldTerm : currentContract.getContractTerms()) {
            if (!newTermIds.contains(oldTerm.getOriginalTermId())) {
                String oldValue = String.format("Term ID: %d, Label: %s, Value: %s, Type: %s",
                        oldTerm.getOriginalTermId(), oldTerm.getTermLabel(), oldTerm.getTermValue(), oldTerm.getTermType().name());
                termAuditTrails.add(AuditTrail.builder()
                        .contract(newContract)
                        .entityName("ContractTerm")
                        .entityId(oldTerm.getId())
                        .action("DELETE")
                        .fieldName("contractTerms")
                        .oldValue(oldValue)
                        .newValue(null)
                        .changedAt(now)
                        .changedBy(changedBy)
                        .changeSummary("Đã xóa điều khoản với Term ID: " + oldTerm.getOriginalTermId())
                        .build());
            }
        }
        newContract.setContractTerms(updatedTerms);

        // 6. Xử lý ContractAdditionalTermDetail
        List<ContractAdditionalTermDetail> updatedDetails = new ArrayList<>();
        List<AuditTrail> additionalTermAuditTrails = new ArrayList<>();

        if (dto.getAdditionalConfig() != null) {
            Map<String, Map<String, List<TermSnapshotDTO>>> configMap = dto.getAdditionalConfig();
            Set<Long> newTypeTermIds = configMap.keySet().stream().map(Long::parseLong).collect(Collectors.toSet());

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

                ContractAdditionalTermDetail newDetail = ContractAdditionalTermDetail.builder()
                        .contract(newContract)
                        .typeTermId(configTypeTermId)
                        .commonTerms(commonSnapshots)
                        .aTerms(aSnapshots)
                        .bTerms(bSnapshots)
                        .build();
                updatedDetails.add(newDetail);

                ContractAdditionalTermDetail oldDetail = currentContract.getAdditionalTermDetails().stream()
                        .filter(d -> d.getTypeTermId().equals(configTypeTermId))
                        .findFirst()
                        .orElse(null);
                String oldValue = oldDetail != null ? serializeAdditionalTermDetail(oldDetail) : null;
                String newValue = serializeAdditionalTermDetail(newDetail);
                if (oldDetail == null) {
                    additionalTermAuditTrails.add(AuditTrail.builder()
                            .contract(newContract)
                            .entityName("ContractAdditionalTermDetail")
                            .entityId(null)
                            .action("CREATE")
                            .fieldName("additionalTermDetails")
                            .oldValue(null)
                            .newValue(newValue)
                            .changedAt(now)
                            .changedBy(changedBy)
                            .changeSummary("Đã tạo chi tiết điều khoản bổ sung với TypeTerm ID: " + configTypeTermId)
                            .build());
                } else if (!oldValue.equals(newValue)) {
                    additionalTermAuditTrails.add(AuditTrail.builder()
                            .contract(newContract)
                            .entityName("ContractAdditionalTermDetail")
                            .entityId(null)
                            .action("UPDATE")
                            .fieldName("additionalTermDetails")
                            .oldValue(oldValue)
                            .newValue(newValue)
                            .changedAt(now)
                            .changedBy(changedBy)
                            .changeSummary("Đã cập nhật chi tiết điều khoản bổ sung với TypeTerm ID: " + configTypeTermId)
                            .build());
                }
            }

            // Xử lý các chi tiết bị xóa
            for (ContractAdditionalTermDetail oldDetail : currentContract.getAdditionalTermDetails()) {
                if (!newTypeTermIds.contains(oldDetail.getTypeTermId())) {
                    String oldValue = serializeAdditionalTermDetail(oldDetail);
                    additionalTermAuditTrails.add(AuditTrail.builder()
                            .contract(newContract)
                            .entityName("ContractAdditionalTermDetail")
                            .entityId(oldDetail.getId())
                            .action("DELETE")
                            .fieldName("additionalTermDetails")
                            .oldValue(oldValue)
                            .newValue(null)
                            .changedAt(now)
                            .changedBy(changedBy)
                            .changeSummary("Đã xóa chi tiết điều khoản bổ sung với TypeTerm ID: " + oldDetail.getTypeTermId())
                            .build());
                }
            }
        } else {
            for (ContractAdditionalTermDetail oldDetail : currentContract.getAdditionalTermDetails()) {
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
        newContract.setAdditionalTermDetails(updatedDetails);

        // 7. Xử lý PaymentSchedule
        List<PaymentSchedule> updatedPayments = new ArrayList<>();

        List<AuditTrail> paymentAuditTrails = new ArrayList<>();

        if (dto.getPayments() != null) {
            Set<Long> newPaymentIds = dto.getPayments().stream()
                    .filter(p -> p.getId() != null)
                    .map(PaymentScheduleDTO::getId)
                    .collect(Collectors.toSet());

            for (PaymentScheduleDTO paymentDTO : dto.getPayments()) {
                // Tìm PaymentSchedule cũ nếu có
                PaymentSchedule oldPayment = paymentDTO.getId() != null ? currentContract.getPaymentSchedules().stream()
                        .filter(p -> p.getId().equals(paymentDTO.getId()))
                        .findFirst()
                        .orElse(null) : null;

                if (oldPayment != null) {
                    // Kiểm tra xem có thay đổi hay không
                    boolean hasChanges = !Objects.equals(oldPayment.getPaymentOrder(), paymentDTO.getPaymentOrder()) ||
                            !Objects.equals(oldPayment.getAmount(), paymentDTO.getAmount()) ||
                            !Objects.equals(oldPayment.getNotifyPaymentDate(), paymentDTO.getNotifyPaymentDate()) ||
                            !Objects.equals(oldPayment.getPaymentDate(), paymentDTO.getPaymentDate()) ||
                            !Objects.equals(oldPayment.getStatus(), paymentDTO.getStatus()) ||
                            !Objects.equals(oldPayment.getPaymentMethod(), paymentDTO.getPaymentMethod()) ||
                            !Objects.equals(oldPayment.getNotifyPaymentContent(), paymentDTO.getNotifyPaymentContent()) ||
                            oldPayment.isReminderEmailSent() != paymentDTO.isReminderEmailSent() ||
                            oldPayment.isOverdueEmailSent() != paymentDTO.isOverdueEmailSent();

                    if (hasChanges) {
                        // Cập nhật oldPayment và lưu trực tiếp
                        String oldValue = serializePaymentSchedule(oldPayment); // Lưu trạng thái trước khi cập nhật
                        oldPayment.setPaymentOrder(paymentDTO.getPaymentOrder());
                        oldPayment.setAmount(paymentDTO.getAmount());
                        oldPayment.setNotifyPaymentDate(paymentDTO.getNotifyPaymentDate());
                        oldPayment.setPaymentDate(paymentDTO.getPaymentDate());
                        oldPayment.setStatus(paymentDTO.getStatus());
                        oldPayment.setPaymentMethod(paymentDTO.getPaymentMethod());
                        oldPayment.setNotifyPaymentContent(paymentDTO.getNotifyPaymentContent());
                        oldPayment.setReminderEmailSent(paymentDTO.isReminderEmailSent());
                        oldPayment.setOverdueEmailSent(paymentDTO.isOverdueEmailSent());
                        oldPayment.setContract(newContract); // Liên kết với newContract

                        // Lưu oldPayment trực tiếp để cập nhật bản ghi trong database
                        paymentScheduleRepository.save(oldPayment);

                        // Ghi log UPDATE
                        String newValue = serializePaymentSchedule(oldPayment);
                        paymentAuditTrails.add(AuditTrail.builder()
                                .contract(newContract)
                                .entityName("PaymentSchedule")
                                .entityId(oldPayment.getId())
                                .action("UPDATE")
                                .fieldName("paymentSchedules")
                                .oldValue(oldValue)
                                .newValue(newValue)
                                .changedAt(now)
                                .changedBy(changedBy)
                                .changeSummary("Đã cập nhật PaymentSchedule với ID: " + oldPayment.getId())
                                .build());
                    }
                    // Thêm oldPayment vào updatedPayments (đã cập nhật hoặc không thay đổi)
                    updatedPayments.add(oldPayment);
                } else {
                    // Tạo mới PaymentSchedule
                    PaymentSchedule newPayment = new PaymentSchedule();
                    newPayment.setContract(newContract);
                    newPayment.setPaymentOrder(paymentDTO.getPaymentOrder());
                    newPayment.setAmount(paymentDTO.getAmount());
                    newPayment.setNotifyPaymentDate(paymentDTO.getNotifyPaymentDate());
                    newPayment.setPaymentDate(paymentDTO.getPaymentDate());
                    newPayment.setStatus(paymentDTO.getStatus());
                    newPayment.setPaymentMethod(paymentDTO.getPaymentMethod());
                    newPayment.setNotifyPaymentContent(paymentDTO.getNotifyPaymentContent());
                    newPayment.setReminderEmailSent(paymentDTO.isReminderEmailSent());
                    newPayment.setOverdueEmailSent(paymentDTO.isOverdueEmailSent());
                    updatedPayments.add(newPayment);

                    // Ghi log CREATE
                    String newValue = serializePaymentSchedule(newPayment);
                    paymentAuditTrails.add(AuditTrail.builder()
                            .contract(newContract)
                            .entityName("PaymentSchedule")
                            .entityId(null) // ID sẽ được gán sau khi lưu
                            .action("CREATE")
                            .fieldName("paymentSchedules")
                            .oldValue(null)
                            .newValue(newValue)
                            .changedAt(now)
                            .changedBy(changedBy)
                            .changeSummary("Đã tạo PaymentSchedule mới")
                            .build());
                }
            }

            // Xử lý DELETE
            for (PaymentSchedule oldPayment : currentContract.getPaymentSchedules()) {
                if (!newPaymentIds.contains(oldPayment.getId())) {
                    String oldValue = serializePaymentSchedule(oldPayment);
                    paymentAuditTrails.add(AuditTrail.builder()
                            .contract(newContract)
                            .entityName("PaymentSchedule")
                            .entityId(oldPayment.getId())
                            .action("DELETE")
                            .fieldName("paymentSchedules")
                            .oldValue(oldValue)
                            .newValue(null)
                            .changedAt(now)
                            .changedBy(changedBy)
                            .changeSummary("Đã xóa PaymentSchedule với ID: " + oldPayment.getId())
                            .build());
                    // Xóa bản ghi cũ khỏi database
                    paymentScheduleRepository.delete(oldPayment);
                }
            }
        } else {
            // Nếu dto.getPayments() là null, sao chép tất cả PaymentSchedule cũ
            for (PaymentSchedule oldPayment : currentContract.getPaymentSchedules()) {
                oldPayment.setContract(newContract);
                updatedPayments.add(oldPayment);
            }
        }
        newContract.setPaymentSchedules(updatedPayments);

        Contract savedNewContractForPayment = contractRepository.save(newContract);
        for (AuditTrail auditTrail : paymentAuditTrails) {
            if ("CREATE".equals(auditTrail.getAction()) && auditTrail.getEntityId() == null) {
                PaymentSchedule savedPayment = savedNewContract.getPaymentSchedules().stream()
                        .filter(p -> p.getPaymentOrder().equals(getPaymentOrderFromNewValue(auditTrail.getNewValue())))
                        .findFirst()
                        .orElse(null);
                if (savedPayment != null) {
                    auditTrail.setEntityId(savedPayment.getId());
                }
            }
            auditTrail.setContract(savedNewContract);
        }

        // 8. Lưu hợp đồng mới
         savedNewContract = contractRepository.save(newContract);

// 4. Ghi audit trail cho các thay đổi trên hợp đồng chính
        if (dto.getTitle() != null && !dto.getTitle().equals(currentContract.getTitle())) {
            auditTrails.add(createAuditTrail(savedNewContract, "title", currentContract.getTitle(), dto.getTitle(), now, changedBy, "UPDATE", "Cập nhật tiêu đề hợp đồng"));
        }
        if (dto.getSigningDate() != null && !Objects.equals(dto.getSigningDate(), currentContract.getSigningDate())) {
            auditTrails.add(createAuditTrail(savedNewContract, "signingDate", currentContract.getSigningDate() != null ? currentContract.getSigningDate().toString() : null, dto.getSigningDate() != null ? dto.getSigningDate().toString() : null, now, changedBy, "UPDATE", "Cập nhật ngày ký hợp đồng"));
        }
        if (dto.getContractLocation() != null && !dto.getContractLocation().equals(currentContract.getContractLocation())) {
            auditTrails.add(createAuditTrail(savedNewContract, "contractLocation", currentContract.getContractLocation(), dto.getContractLocation(), now, changedBy, "UPDATE", "Cập nhật địa điểm hợp đồng"));
        }
        if (dto.getAmount() != null && !Objects.equals(dto.getAmount(), currentContract.getAmount())) {
            auditTrails.add(createAuditTrail(savedNewContract, "amount", currentContract.getAmount() != null ? currentContract.getAmount().toString() : null, dto.getAmount() != null ? dto.getAmount().toString() : null, now, changedBy, "UPDATE", "Cập nhật số tiền hợp đồng"));
        }
        if (dto.getEffectiveDate() != null && !Objects.equals(dto.getEffectiveDate(), currentContract.getEffectiveDate())) {
            auditTrails.add(createAuditTrail(savedNewContract, "effectiveDate", currentContract.getEffectiveDate() != null ? currentContract.getEffectiveDate().toString() : null, dto.getEffectiveDate() != null ? dto.getEffectiveDate().toString() : null, now, changedBy, "UPDATE", "Cập nhật ngày hiệu lực hợp đồng"));
        }
        if (dto.getExpiryDate() != null && !Objects.equals(dto.getExpiryDate(), currentContract.getExpiryDate())) {
            auditTrails.add(createAuditTrail(savedNewContract, "expiryDate", currentContract.getExpiryDate() != null ? currentContract.getExpiryDate().toString() : null, dto.getExpiryDate() != null ? dto.getExpiryDate().toString() : null, now, changedBy, "UPDATE", "Cập nhật ngày hết hạn"));
        }
        if (dto.getNotifyEffectiveDate() != null && !Objects.equals(dto.getNotifyEffectiveDate(), currentContract.getNotifyEffectiveDate())) {
            auditTrails.add(createAuditTrail(savedNewContract, "notifyEffectiveDate", currentContract.getNotifyEffectiveDate() != null ? currentContract.getNotifyEffectiveDate().toString() : null, dto.getNotifyEffectiveDate() != null ? dto.getNotifyEffectiveDate().toString() : null, now, changedBy, "UPDATE", "Cập nhật ngày thông báo hiệu lực hợp đồng"));
        }
        if (dto.getNotifyExpiryDate() != null && !Objects.equals(dto.getNotifyExpiryDate(), currentContract.getNotifyExpiryDate())) {
            auditTrails.add(createAuditTrail(savedNewContract, "notifyExpiryDate", currentContract.getNotifyExpiryDate() != null ? currentContract.getNotifyExpiryDate().toString() : null, dto.getNotifyExpiryDate() != null ? dto.getNotifyExpiryDate().toString() : null, now, changedBy, "UPDATE", "Cập nhật ngày thông báo hết hạn"));
        }
        if (dto.getNotifyEffectiveContent() != null && !dto.getNotifyEffectiveContent().equals(currentContract.getNotifyEffectiveContent())) {
            auditTrails.add(createAuditTrail(savedNewContract, "notifyEffectiveContent", currentContract.getNotifyEffectiveContent(), dto.getNotifyEffectiveContent(), now, changedBy, "UPDATE", "Cập nhật nội dung thông báo hiệu lực hợp đồng"));
        }
        if (dto.getNotifyExpiryContent() != null && !dto.getNotifyExpiryContent().equals(currentContract.getNotifyExpiryContent())) {
            auditTrails.add(createAuditTrail(savedNewContract, "notifyExpiryContent", currentContract.getNotifyExpiryContent(), dto.getNotifyExpiryContent(), now, changedBy, "UPDATE", "Cập nhật nội dung thông báo hết hạn"));
        }
        if (dto.getSpecialTermsA() != null && !dto.getSpecialTermsA().equals(currentContract.getSpecialTermsA())) {
            auditTrails.add(createAuditTrail(savedNewContract, "specialTermsA", currentContract.getSpecialTermsA(), dto.getSpecialTermsA(), now, changedBy, "UPDATE", "Cập nhật điều khoản bên A"));
        }
        if (dto.getSpecialTermsB() != null && !dto.getSpecialTermsB().equals(currentContract.getSpecialTermsB())) {
            auditTrails.add(createAuditTrail(savedNewContract, "specialTermsB", currentContract.getSpecialTermsB(), dto.getSpecialTermsB(), now, changedBy, "UPDATE", "Cập nhật điều khoản bên B"));
        }
        if (dto.getContractContent() != null && !dto.getContractContent().equals(currentContract.getContractContent())) {
            auditTrails.add(createAuditTrail(savedNewContract, "contractContent", currentContract.getContractContent(), dto.getContractContent(), now, changedBy, "UPDATE", "Cập nhật nội dung hợp đồng"));
        }
        if (dto.getAppendixEnabled() != null && !dto.getAppendixEnabled().equals(currentContract.getAppendixEnabled())) {
            auditTrails.add(createAuditTrail(savedNewContract, "appendixEnabled", currentContract.getAppendixEnabled() != null ? currentContract.getAppendixEnabled().toString() : null, dto.getAppendixEnabled() != null ? dto.getAppendixEnabled().toString() : null, now, changedBy, "UPDATE", "Cập nhật phụ lục"));
        }
        if (dto.getTransferEnabled() != null && !dto.getTransferEnabled().equals(currentContract.getTransferEnabled())) {
            auditTrails.add(createAuditTrail(savedNewContract, "transferEnabled", currentContract.getTransferEnabled() != null ? currentContract.getTransferEnabled().toString() : null, dto.getTransferEnabled() != null ? dto.getTransferEnabled().toString() : null, now, changedBy, "UPDATE", "Cập nhật chuyển giao"));
        }
        if (dto.getAutoAddVAT() != null && !dto.getAutoAddVAT().equals(currentContract.getAutoAddVAT())) {
            auditTrails.add(createAuditTrail(savedNewContract, "autoAddVAT", currentContract.getAutoAddVAT() != null ? currentContract.getAutoAddVAT().toString() : null, dto.getAutoAddVAT() != null ? dto.getAutoAddVAT().toString() : null, now, changedBy, "UPDATE", "Cập nhật VAT"));
        }
        if (dto.getVatPercentage() != null && !Objects.equals(dto.getVatPercentage(), currentContract.getVatPercentage())) {
            auditTrails.add(createAuditTrail(savedNewContract, "vatPercentage", currentContract.getVatPercentage() != null ? currentContract.getVatPercentage().toString() : null, dto.getVatPercentage() != null ? dto.getVatPercentage().toString() : null, now, changedBy, "UPDATE", "Cập nhật VAT %"));
        }
        if (dto.getIsDateLateChecked() != null && !dto.getIsDateLateChecked().equals(currentContract.getIsDateLateChecked())) {
            auditTrails.add(createAuditTrail(savedNewContract, "isDateLateChecked", currentContract.getIsDateLateChecked() != null ? currentContract.getIsDateLateChecked().toString() : null, dto.getIsDateLateChecked() != null ? dto.getIsDateLateChecked().toString() : null, now, changedBy, "UPDATE", "Cập nhật trễ ngày hợp đồng"));
        }
        if (dto.getAutoRenew() != null && !dto.getAutoRenew().equals(currentContract.getAutoRenew())) {
            auditTrails.add(createAuditTrail(savedNewContract, "autoRenew", currentContract.getAutoRenew() != null ? currentContract.getAutoRenew().toString() : null, dto.getAutoRenew() != null ? dto.getAutoRenew().toString() : null, now, changedBy, "UPDATE", "Cập nhật renew cho hợp đồng"));
        }
        if (dto.getViolate() != null && !dto.getViolate().equals(currentContract.getViolate())) {
            auditTrails.add(createAuditTrail(savedNewContract, "violate", currentContract.getViolate() != null ? currentContract.getViolate().toString() : null, dto.getViolate() != null ? dto.getViolate().toString() : null, now, changedBy, "UPDATE", "Cập nhật vi phạm cho hợp đồng"));
        }
        if (dto.getSuspend() != null && !dto.getSuspend().equals(currentContract.getSuspend())) {
            auditTrails.add(createAuditTrail(savedNewContract, "suspend", currentContract.getSuspend() != null ? currentContract.getSuspend().toString() : null, dto.getSuspend() != null ? dto.getSuspend().toString() : null, now, changedBy, "UPDATE", "Cập nhật suspend cho hợp đồng"));
        }
        if (dto.getSuspendContent() != null && !dto.getSuspendContent().equals(currentContract.getSuspendContent())) {
            auditTrails.add(createAuditTrail(savedNewContract, "suspendContent", currentContract.getSuspendContent(), dto.getSuspendContent(), now, changedBy, "UPDATE", "Cập nhật nội dung suspend cho hợp đồng"));
        }
        if (dto.getMaxDateLate() != null && !dto.getMaxDateLate().equals(currentContract.getMaxDateLate())) {
            auditTrails.add(createAuditTrail(savedNewContract, "maxDateLate", String.valueOf(currentContract.getMaxDateLate()), String.valueOf(dto.getMaxDateLate()), now, changedBy, "UPDATE", "Cập nhật số ngày trễ tối đa"));
        }
        if (dto.getStatus() != null && !dto.getStatus().equals(currentContract.getStatus())) {
            auditTrails.add(createAuditTrail(savedNewContract, "status", currentContract.getStatus().name(), dto.getStatus().name(), now, changedBy, "UPDATE", "Cập nhật trạng thái hợp đồng"));
        }

        // 9. Cập nhật entityId cho audit trails
        for (AuditTrail auditTrail : termAuditTrails) {
            if ("CREATE".equals(auditTrail.getAction()) || "UPDATE".equals(auditTrail.getAction())) {
                ContractTerm savedTerm = savedNewContract.getContractTerms().stream()
                        .filter(t -> t.getOriginalTermId().equals(getTermIdFromNewValue(auditTrail.getNewValue())))
                        .findFirst()
                        .orElse(null);
                if (savedTerm != null) {
                    auditTrail.setEntityId(savedTerm.getId());
                }
            }
            auditTrail.setContract(savedNewContract);
        }
        for (AuditTrail auditTrail : additionalTermAuditTrails) {
            if ("CREATE".equals(auditTrail.getAction()) || "UPDATE".equals(auditTrail.getAction())) {
                ContractAdditionalTermDetail savedDetail = savedNewContract.getAdditionalTermDetails().stream()
                        .filter(d -> d.getTypeTermId().equals(getTypeTermIdFromNewValue(auditTrail.getNewValue())))
                        .findFirst()
                        .orElse(null);
                if (savedDetail != null) {
                    auditTrail.setEntityId(savedDetail.getId());
                }
            }
            auditTrail.setContract(savedNewContract);
        }
        for (AuditTrail auditTrail : paymentAuditTrails) {
            if ("CREATE".equals(auditTrail.getAction()) || "UPDATE".equals(auditTrail.getAction())) {
                PaymentSchedule savedPayment = savedNewContract.getPaymentSchedules().stream()
                        .filter(p -> p.getPaymentOrder().equals(getPaymentOrderFromNewValue(auditTrail.getNewValue())))
                        .findFirst()
                        .orElse(null);
                if (savedPayment != null) {
                    auditTrail.setEntityId(savedPayment.getId());
                }
            }
            auditTrail.setContract(savedNewContract);
        }

        // 10. Lưu tất cả audit trails
        auditTrailRepository.saveAll(auditTrails);
        auditTrailRepository.saveAll(termAuditTrails);
        auditTrailRepository.saveAll(additionalTermAuditTrails);
        auditTrailRepository.saveAll(paymentAuditTrails);

        // 11. Ghi log audit trail cho hành động tạo phiên bản mới
        AuditTrail versionAuditTrail = AuditTrail.builder()
                .contract(savedNewContract)
                .entityName("Contract")
                .entityId(savedNewContract.getId())
                .action("CREATE_VERSION")
                .fieldName("contract")
                .oldValue(serializeContract(currentContract))
                .newValue(serializeContract(savedNewContract))
                .changedAt(now)
                .changedBy(changedBy)
                .changeSummary("Đã tạo phiên bản " + newVersion + " của hợp đồng " + currentContract.getContractNumber())
                .build();
        auditTrailRepository.save(versionAuditTrail);

        return savedNewContract;
    }


    private String serializeAdditionalTermDetail(ContractAdditionalTermDetail detail) {
        String commonTermsStr = detail.getCommonTerms().stream()
                .map(t -> "Term ID: " + t.getTermId() + ", Label: " + t.getTermLabel())
                .collect(Collectors.joining("; "));
        String aTermsStr = detail.getATerms().stream()
                .map(t -> "Term ID: " + t.getTermId() + ", Label: " + t.getTermLabel())
                .collect(Collectors.joining("; "));
        String bTermsStr = detail.getBTerms().stream()
                .map(t -> "Term ID: " + t.getTermId() + ", Label: " + t.getTermLabel())
                .collect(Collectors.joining("; "));
        return String.format("ContractAdditionalTermDetail{typeTermId=%d, commonTerms=[%s], aTerms=[%s], bTerms=[%s]}",
                detail.getTypeTermId(), commonTermsStr, aTermsStr, bTermsStr);
    }

    private AuditTrail createAuditTrail(Contract contract, String fieldName, String oldValue, String newValue,
                                        LocalDateTime changedAt, String changedBy, String action, String changeSummary) {
        return AuditTrail.builder()
                .entityName("Contract")
                .entityId(contract.getId())  // Đảm bảo gán entityId bằng ID của hợp đồng
                .contract(contract)
                .action(action)
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedAt(changedAt)
                .changedBy(changedBy)
                .changeSummary(changeSummary)
                .build();
    }


    // Hàm hỗ trợ để lấy termId từ newValue (nếu cần)
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

    private Long getTypeTermIdFromNewValue(String newValue) {
        String[] parts = newValue.split(",");
        for (String part : parts) {
            if (part.contains("typeTermId=")) {
                return Long.parseLong(part.split("=")[1].trim());
            }
        }
        throw new RuntimeException("Không thể trích xuất typeTermId từ newValue: " + newValue);
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

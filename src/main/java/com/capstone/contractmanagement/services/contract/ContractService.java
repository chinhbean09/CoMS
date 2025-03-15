package com.capstone.contractmanagement.services.contract;

import com.capstone.contractmanagement.components.SecurityUtils;
import com.capstone.contractmanagement.dtos.contract.ContractComparisonDTO;
import com.capstone.contractmanagement.dtos.contract.ContractDTO;
import com.capstone.contractmanagement.dtos.contract.ContractUpdateDTO;
import com.capstone.contractmanagement.dtos.contract.TermSnapshotDTO;
import com.capstone.contractmanagement.dtos.payment.PaymentDTO;
import com.capstone.contractmanagement.dtos.payment.PaymentScheduleDTO;
import com.capstone.contractmanagement.entities.*;
import com.capstone.contractmanagement.entities.approval_workflow.ApprovalWorkflow;
import com.capstone.contractmanagement.entities.contract.*;
import com.capstone.contractmanagement.entities.contract_template.ContractTemplate;
import com.capstone.contractmanagement.entities.term.Term;
import com.capstone.contractmanagement.enums.ApprovalStatus;
import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.enums.PaymentStatus;
import com.capstone.contractmanagement.enums.TypeTermIdentifier;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.exceptions.ResourceNotFoundException;
import com.capstone.contractmanagement.repositories.*;
import com.capstone.contractmanagement.responses.User.UserContractResponse;
import com.capstone.contractmanagement.responses.contract.*;
import com.capstone.contractmanagement.responses.payment_schedule.PaymentScheduleResponse;
import com.capstone.contractmanagement.responses.term.TermResponse;
import com.capstone.contractmanagement.responses.term.TypeTermResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractService implements IContractService{


    private final IContractRepository contractRepository;
    private final IContractTemplateRepository contractTemplateRepository;
    private final IUserRepository userRepository;
    private final IPartnerRepository partyRepository;
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
        // Kiểm tra và ném lỗi nếu không tìm thấy mẫu hợp đồng hoặc đối tác
        ContractTemplate template = contractTemplateRepository.findById(dto.getTemplateId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mẫu hợp đồng với ID: " + dto.getTemplateId()));
        Partner partner = partyRepository.findById(dto.getPartyId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đối tác với ID: " + dto.getPartyId()));
        User user = currentUser.getLoggedInUser();
        if (user == null) {
            throw new IllegalStateException("Không tìm thấy thông tin người dùng hiện tại.");
        }

        LocalDateTime createdAt = LocalDateTime.now();
        String contractNumber = generateContractNumber(createdAt, dto.getContractTitle());

        // 2. Tạo hợp đồng mới
        Contract contract = Contract.builder()
                .title(dto.getTemplateData().getContractTitle())
                .contractNumber(contractNumber)
                .partner(partner)
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

        // Căn cứ pháp lý
        if (dto.getTemplateData().getLegalBasisTerms() != null) {
            for (TermSnapshotDTO termDTO : dto.getTemplateData().getLegalBasisTerms()) {
                if (termDTO.getId() == null) {
                    throw new IllegalArgumentException("ID của điều khoản Căn cứ pháp lý không được để trống.");
                }
                Term term = termRepository.findById(termDTO.getId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy điều khoản với ID: " + termDTO.getId()));
                if (!term.getTypeTerm().getIdentifier().equals(TypeTermIdentifier.LEGAL_BASIS)) {
                    throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại Căn cứ pháp lý (LEGAL_BASIS).");
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

        // Các điều khoản chung
        if (dto.getTemplateData().getGeneralTerms() != null) {
            for (TermSnapshotDTO termDTO : dto.getTemplateData().getGeneralTerms()) {
                if (termDTO.getId() == null) {
                    throw new IllegalArgumentException("ID của điều khoản chung không được để trống.");
                }
                Term term = termRepository.findById(termDTO.getId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy điều khoản với ID: " + termDTO.getId()));
                if (!term.getTypeTerm().getIdentifier().equals(TypeTermIdentifier.GENERAL_TERMS)) {
                    throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại Điều khoản chung (GENERAL_TERMS).");
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

        // Các điều khoản khác
        if (dto.getTemplateData().getOtherTerms() != null) {
            for (TermSnapshotDTO termDTO : dto.getTemplateData().getOtherTerms()) {
                if (termDTO.getId() == null) {
                    throw new IllegalArgumentException("ID của điều khoản khác không được để trống.");
                }
                Term term = termRepository.findById(termDTO.getId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy điều khoản với ID: " + termDTO.getId()));
                if (!term.getTypeTerm().getIdentifier().equals(TypeTermIdentifier.OTHER_TERMS)) {
                    throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại Điều khoản khác (OTHER_TERMS).");
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
        List<ContractAdditionalTermDetail> additionalDetails = new ArrayList<>();
        if (dto.getTemplateData().getAdditionalConfig() != null) {
            Map<String, Map<String, List<TermSnapshotDTO>>> configMap = dto.getTemplateData().getAdditionalConfig();
            for (Map.Entry<String, Map<String, List<TermSnapshotDTO>>> entry : configMap.entrySet()) {
                String key = entry.getKey();
                Long configTypeTermId;
                try {
                    configTypeTermId = Long.parseLong(key);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Key trong additionalConfig phải là số đại diện cho Id của loại điều khoản. Key không hợp lệ: " + key);
                }
                Map<String, List<TermSnapshotDTO>> groupConfig = entry.getValue();

                // Map nhóm Common
                List<AdditionalTermSnapshot> commonSnapshots = new ArrayList<>();
                if (groupConfig.containsKey("Common")) {
                    for (TermSnapshotDTO termDTO : groupConfig.get("Common")) {
                        if (termDTO.getId() == null) {
                            throw new IllegalArgumentException("ID của điều khoản trong nhóm điều khoản chung không được để trống.");
                        }
                        Term term = termRepository.findById(termDTO.getId())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy điều khoản với ID: " + termDTO.getId()));
                        commonSnapshots.add(AdditionalTermSnapshot.builder()
                                .termId(term.getId())
                                .termLabel(term.getLabel())
                                .termValue(term.getValue())
                                .build());
                    }
                }

                // Map nhóm A
                List<AdditionalTermSnapshot> aSnapshots = new ArrayList<>();
                if (groupConfig.containsKey("A")) {
                    for (TermSnapshotDTO termDTO : groupConfig.get("A")) {
                        if (termDTO.getId() == null) {
                            throw new IllegalArgumentException("ID của điều khoản trong nhóm bên A không được để trống.");
                        }
                        Term term = termRepository.findById(termDTO.getId())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy điều khoản với ID: " + termDTO.getId()));
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
                        if (termDTO.getId() == null) {
                            throw new IllegalArgumentException("ID của điều khoản trong nhóm bên B không được để trống.");
                        }
                        Term term = termRepository.findById(termDTO.getId())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy điều khoản với ID: " + termDTO.getId()));
                        bSnapshots.add(AdditionalTermSnapshot.builder()
                                .termId(term.getId())
                                .termLabel(term.getLabel())
                                .termValue(term.getValue())
                                .build());
                    }
                }

                // Kiểm tra trùng lặp
                Set<Long> unionCommonA = new HashSet<>(commonSnapshots.stream().map(AdditionalTermSnapshot::getTermId).toList());
                unionCommonA.retainAll(aSnapshots.stream().map(AdditionalTermSnapshot::getTermId).toList());
                if (!unionCommonA.isEmpty()) {
                    throw new IllegalArgumentException("Các điều khoản không được chọn đồng thời ở nhóm 'Chung' và 'A'.");
                }
                Set<Long> unionCommonB = new HashSet<>(commonSnapshots.stream().map(AdditionalTermSnapshot::getTermId).toList());
                unionCommonB.retainAll(bSnapshots.stream().map(AdditionalTermSnapshot::getTermId).toList());
                if (!unionCommonB.isEmpty()) {
                    throw new IllegalArgumentException("Các điều khoản không được chọn đồng thời ở nhóm 'Chung' và 'B'.");
                }
                Set<Long> unionAB = new HashSet<>(aSnapshots.stream().map(AdditionalTermSnapshot::getTermId).toList());
                unionAB.retainAll(bSnapshots.stream().map(AdditionalTermSnapshot::getTermId).toList());
                if (!unionAB.isEmpty()) {
                    throw new IllegalArgumentException("Các điều khoản không được chọn đồng thời ở nhóm 'A' và 'B'.");
                }

                // Kiểm tra type term
                for (AdditionalTermSnapshot snap : commonSnapshots) {
                    Term term = termRepository.findById(snap.getTermId())
                            .orElseThrow(() -> new IllegalArgumentException("Không tồn tại điều khoản với ID: " + snap.getTermId()));
                    if (!term.getTypeTerm().getId().equals(configTypeTermId)) {
                        throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại điều khoản: \"" + term.getTypeTerm().getName() + "\".");
                    }
                }
                for (AdditionalTermSnapshot snap : aSnapshots) {
                    Term term = termRepository.findById(snap.getTermId())
                            .orElseThrow(() -> new IllegalArgumentException("Không tồn tại điều khoản với ID: " + snap.getTermId()));
                    if (!term.getTypeTerm().getId().equals(configTypeTermId)) {
                        throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại điều khoản: \"" + term.getTypeTerm().getName() + "\".");
                    }
                }
                for (AdditionalTermSnapshot snap : bSnapshots) {
                    Term term = termRepository.findById(snap.getTermId())
                            .orElseThrow(() -> new IllegalArgumentException("Không tồn tại điều khoản với ID: " + snap.getTermId()));
                    if (!term.getTypeTerm().getId().equals(configTypeTermId)) {
                        throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại điều khoản: \"" + term.getTypeTerm().getName() + "\".");
                    }
                }

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

        // 5. Ánh xạ lịch thanh toán
        List<PaymentSchedule> paymentSchedules = new ArrayList<>();
        if (dto.getPayments() != null) {
            int order = 1;
            for (PaymentDTO paymentDTO : dto.getPayments()) {
                if (paymentDTO.getAmount() == null || paymentDTO.getAmount() <= 0.0) {
                    throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn 0.");
                }
                if (paymentDTO.getPaymentDate() == null) {
                    throw new IllegalArgumentException("Ngày thanh toán không được để trống.");
                }
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

        // 6. Lưu hợp đồng
        Contract savedContract = contractRepository.save(contract);
        savedContract.setOriginalContractId(savedContract.getId());
        contractRepository.save(savedContract);

        // 7. Ghi audit trail cho các trường quan trọng
        List<AuditTrail> auditTrails = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        String changedBy = user.getFullName();

        // Ghi audit cho từng trường với change summary bằng tiếng Việt
        auditTrails.add(createAuditTrail(savedContract, "title", null, savedContract.getTitle(), now, changedBy,
                "Tạo hợp đồng mới với tiêu đề: " + savedContract.getTitle()));
        auditTrails.add(createAuditTrail(savedContract, "contractNumber", null, savedContract.getContractNumber(), now, changedBy,
                "Gán số hợp đồng: " + savedContract.getContractNumber()));
        auditTrails.add(createAuditTrail(savedContract, "partner", null, savedContract.getPartner().getId().toString(), now, changedBy,
                "Liên kết với đối tác có ID: " + savedContract.getPartner().getId()));
        auditTrails.add(createAuditTrail(savedContract, "user", null, savedContract.getUser().getId().toString(), now, changedBy,
                "Gán cho người dùng có ID: " + savedContract.getUser().getId()));
        auditTrails.add(createAuditTrail(savedContract, "template", null, savedContract.getTemplate().getId().toString(), now, changedBy,
                "Sử dụng mẫu hợp đồng có ID: " + savedContract.getTemplate().getId()));
        auditTrails.add(createAuditTrail(savedContract, "signingDate", null,
                savedContract.getSigningDate() != null ? savedContract.getSigningDate().toString() : null, now, changedBy,
                "Đặt ngày ký hợp đồng: " + (savedContract.getSigningDate() != null ? savedContract.getSigningDate().toString() : "không có")));
        auditTrails.add(createAuditTrail(savedContract, "contractLocation", null, savedContract.getContractLocation(), now, changedBy,
                "Đặt địa điểm ký hợp đồng: " + savedContract.getContractLocation()));
        auditTrails.add(createAuditTrail(savedContract, "amount", null,
                savedContract.getAmount() != null ? savedContract.getAmount().toString() : null, now, changedBy,
                "Đặt giá trị hợp đồng: " + (savedContract.getAmount() != null ? savedContract.getAmount().toString() : "không có")));
        auditTrails.add(createAuditTrail(savedContract, "effectiveDate", null,
                savedContract.getEffectiveDate() != null ? savedContract.getEffectiveDate().toString() : null, now, changedBy,
                "Đặt ngày hiệu lực: " + (savedContract.getEffectiveDate() != null ? savedContract.getEffectiveDate().toString() : "không có")));
        auditTrails.add(createAuditTrail(savedContract, "expiryDate", null,
                savedContract.getExpiryDate() != null ? savedContract.getExpiryDate().toString() : null, now, changedBy,
                "Đặt ngày hết hạn: " + (savedContract.getExpiryDate() != null ? savedContract.getExpiryDate().toString() : "không có")));
        auditTrails.add(createAuditTrail(savedContract, "status", null, savedContract.getStatus().name(), now, changedBy,
                "Đặt trạng thái ban đầu: " + savedContract.getStatus().name()));
        auditTrails.add(createAuditTrail(savedContract, "createdAt", null, savedContract.getCreatedAt().toString(), now, changedBy,
                "Hợp đồng được tạo vào: " + savedContract.getCreatedAt().toString()));

        // Ghi audit cho ContractTerm
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
                    .changeSummary("Tạo điều khoản hợp đồng mới")
                    .build());
        }

        // Ghi audit cho ContractAdditionalTermDetail
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
                        .changeSummary("Tạo chi tiết điều khoản bổ sung cho hợp đồng")
                        .build());
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Không thể chuyển đổi ContractAdditionalTermDetail thành JSON: " + e.getMessage());
            }
        }

        // Lưu tất cả bản ghi audit trail
        auditTrailRepository.saveAll(auditTrails);

        return savedContract;
    }

    // Phương thức createAuditTrail
    private AuditTrail createAuditTrail(Contract contract, String fieldName, String oldValue, String newValue,
                                        LocalDateTime changedAt, String changedBy, String changeSummary) {
        AuditTrail auditTrail = new AuditTrail();
        auditTrail.setEntityName("Contract");
        auditTrail.setEntityId(contract.getId());
        auditTrail.setContract(contract);
        auditTrail.setAction("CREATE");
        auditTrail.setFieldName(fieldName);
        auditTrail.setOldValue(oldValue);
        auditTrail.setNewValue(newValue);
        auditTrail.setChangedAt(changedAt);
        auditTrail.setChangedBy(changedBy);
        auditTrail.setChangeSummary(changeSummary);

        if (auditTrail.getContract() == null || auditTrail.getContract().getId() == null) {
            throw new IllegalStateException("Hợp đồng trong AuditTrail không tồn tại hoặc không có ID.");
        }

        return auditTrail;
    }

    // Phương thức generateContractNumber
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

    // Phương thức generateTitleAbbreviation
    private String generateTitleAbbreviation(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "HD";
        }
        String[] words = title.trim().split("\\s+");
        return Arrays.stream(words)
                .map(word -> word.isEmpty() ? "" : word.substring(0, 1).toUpperCase())
                .collect(Collectors.joining());
    }
    @Override
    @Transactional(readOnly = true)
    public Page<GetAllContractReponse> getAllContracts(Pageable pageable,
                                                       String keyword,
                                                       ContractStatus status,
                                                       Long contractTypeId,
                                                       User currentUser) {
        boolean hasSearch = keyword != null && !keyword.trim().isEmpty();
        boolean hasStatusFilter = status != null;
        boolean hasContractTypeFilter = contractTypeId != null;
        Page<Contract> contracts;

        boolean isCeo = Boolean.TRUE.equals(currentUser.getIsCeo());
        boolean isStaff = currentUser.getRole() != null &&
                "STAFF".equalsIgnoreCase(currentUser.getRole().getRoleName());

        if (isStaff && !isCeo) {
            // Nếu là STAFF (và không phải CEO), chỉ lấy hợp đồng của user hiện tại
            if (hasStatusFilter) {
                if (hasContractTypeFilter) {
                    if (hasSearch) {
                        keyword = keyword.trim();
                        contracts = contractRepository.findLatestByTitleContainingIgnoreCaseAndStatusAndContractTypeIdAndUser(
                                keyword, status, contractTypeId, currentUser, pageable);
                    } else {
                        contracts = contractRepository.findLatestByStatusAndContractTypeIdAndUser(
                                status, contractTypeId, currentUser, pageable);
                    }
                } else {
                    if (hasSearch) {
                        keyword = keyword.trim();
                        contracts = contractRepository.findLatestByTitleContainingIgnoreCaseAndStatusAndUser(
                                keyword, status, currentUser, pageable);
                    } else {
                        contracts = contractRepository.findLatestByStatusAndUser(
                                status, currentUser, pageable);
                    }
                }
            } else {
                if (hasContractTypeFilter) {
                    if (hasSearch) {
                        keyword = keyword.trim();
                        contracts = contractRepository.findLatestByTitleContainingIgnoreCaseAndStatusNotAndContractTypeIdAndUser(
                                keyword, ContractStatus.DELETED, contractTypeId, currentUser, pageable);
                    } else {
                        contracts = contractRepository.findLatestByStatusNotAndContractTypeIdAndUser(
                                ContractStatus.DELETED, contractTypeId, currentUser, pageable);
                    }
                } else {
                    if (hasSearch) {
                        keyword = keyword.trim();
                        contracts = contractRepository.findLatestByTitleContainingIgnoreCaseAndStatusNotAndUser(
                                keyword, ContractStatus.DELETED, currentUser, pageable);
                    } else {
                        contracts = contractRepository.findLatestByStatusNotAndUser(
                                ContractStatus.DELETED, currentUser, pageable);
                    }
                }
            }
        } else if (isCeo) {
            // Nếu là CEO, thấy tất cả hợp đồng
            if (hasStatusFilter) {
                if (hasContractTypeFilter) {
                    if (hasSearch) {
                        keyword = keyword.trim();
                        contracts = contractRepository.findLatestByTitleContainingIgnoreCaseAndStatusAndContractTypeId(
                                keyword, status, contractTypeId, pageable);
                    } else {
                        contracts = contractRepository.findLatestByStatusAndContractTypeId(status, contractTypeId, pageable);
                    }
                } else {
                    if (hasSearch) {
                        keyword = keyword.trim();
                        contracts = contractRepository.findLatestByTitleContainingIgnoreCaseAndStatus(keyword, status, pageable);
                    } else {
                        contracts = contractRepository.findLatestByStatus(status, pageable);
                    }
                }
            } else {
                if (hasContractTypeFilter) {
                    if (hasSearch) {
                        keyword = keyword.trim();
                        contracts = contractRepository.findLatestByTitleContainingIgnoreCaseAndStatusNotAndContractTypeId(
                                keyword, ContractStatus.DELETED, contractTypeId, pageable);
                    } else {
                        contracts = contractRepository.findLatestByStatusNotAndContractTypeId(ContractStatus.DELETED, contractTypeId, pageable);
                    }
                } else {
                    if (hasSearch) {
                        keyword = keyword.trim();
                        contracts = contractRepository.findLatestByTitleContainingIgnoreCaseAndStatusNot(keyword, ContractStatus.DELETED, pageable);
                    } else {
                        contracts = contractRepository.findLatestByStatusNot(ContractStatus.DELETED, pageable);
                    }
                }
            }
        } else {
            // Các role khác (bao gồm ADMIN), trả về danh sách rỗng
            contracts = new PageImpl<>(Collections.emptyList(), pageable, 0);
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
                .partner(Partner.builder()
                        .id(contract.getPartner().getId())
                        .partnerName(contract.getPartner().getPartnerName())
                        .build())
                .version(contract.getVersion())
                .originalContractId(contract.getOriginalContractId())
                .approvalWorkflowId(contract.getApprovalWorkflow() == null ? null : contract.getApprovalWorkflow().getId())
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
                .partner(contract.getPartner())
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
                .originalContractId(contract.getOriginalContractId())
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
                .contractNumber(originalContract.getContractNumber() + "__" + copyNumber) //HD-001__2
                .originalContractId(originalContract.getId()) // Liên kết với hợp đồng gốc
                .partner(originalContract.getPartner())
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

    public String generateNewContractNumber(Contract currentContract, int newVersion) {
        String baseContractNumber = currentContract.getContractNumber().replaceAll("(-v\\d+)+$", ""); // Xóa toàn bộ phần version
        return baseContractNumber + "-v" + newVersion;
    }


    @Transactional
    @Override
    public Contract updateContract(Long contractId, ContractUpdateDTO dto) {
        // 1. Tìm hợp đồng hiện tại
        Contract currentContract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng với id: " + contractId));

        // 1.1 Kiểm tra xem hợp đồng có đang trong quy trình duyệt hay không
        ApprovalWorkflow workflow = currentContract.getApprovalWorkflow();
        if (workflow != null) {
            // Kiểm tra xem có stage nào có trạng thái REJECTED không
            boolean hasRejectedStage = workflow.getStages().stream()
                    .anyMatch(stage -> stage.getStatus() == ApprovalStatus.REJECTED);
            // Nếu không có stage nào bị từ chối -> không cho phép cập nhật
            if (!hasRejectedStage) {
                throw new RuntimeException("Hợp đồng đang trong quy trình duyệt, không được phép cập nhật.");
            }
        }

        // Không tạo phiên bản mới nếu không có thay đổi
        try {
            if (!hasChanges(currentContract, dto)) {
                return currentContract;
            }
        } catch ( Exception e) {
            e.printStackTrace();
        }


        List<AuditTrail> auditTrails = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        String changedBy = currentUser.getLoggedInUser().getFullName();

        // 2. Xác định hợp đồng gốc và tính toán phiên bản mới
        Long originalContractId = currentContract.getOriginalContractId() != null
                ? currentContract.getOriginalContractId()
                : currentContract.getId();
        int newVersion = calculateNewVersion(originalContractId, currentContract);
        String newContractNumber = generateNewContractNumber(currentContract, newVersion);

        ApprovalWorkflow tempWorkflow = currentContract.getApprovalWorkflow();
        currentContract.setApprovalWorkflow(null);
        contractRepository.save(currentContract);

        // 3. Tạo hợp đồng mới với các giá trị từ currentContract và cập nhật từ DTO
        Contract newContract = Contract.builder()
                .originalContractId(originalContractId)
                .version(newVersion)
                .signingDate(dto.getSigningDate() != null ? dto.getSigningDate() : currentContract.getSigningDate())
                .contractLocation(dto.getContractLocation() != null ? dto.getContractLocation() : currentContract.getContractLocation())
                .contractNumber(newContractNumber)
                .approvalWorkflow(tempWorkflow)
                .specialTermsA(dto.getSpecialTermsA() != null ? dto.getSpecialTermsA() : currentContract.getSpecialTermsA())
                .specialTermsB(dto.getSpecialTermsB() != null ? dto.getSpecialTermsB() : currentContract.getSpecialTermsB())
                .status(ContractStatus.UPDATED)
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
                .partner(currentContract.getPartner())
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
                .contractType(dto.getContractTypeId() != null
                        ? contractTypeRepository.findById(dto.getContractTypeId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy loại hợp đồng với id: " + dto.getContractTypeId()))
                        : currentContract.getContractType())
                .build();

// Contract savedNewContract = contractRepository.save(newContract); // Xóa dòng này
        // 3. ContractTerm

/*
//nếu term id có trong dto thì check db
[DTO Terms] --> (Kiểm tra tồn tại trong DB?)
                |
                |-- Có --> (Đúng loại term?) --> [So sánh với ContractTerm]
                |               |                    |
                |               |-- Giống --> [NO ACTION]
                |               |-- Khác --> [UPDATE]
                |
                |-- Không --> [ Trả Lỗi]

//nếu term id cũ không có trong dto thì xóa đi và ghi audit trail, có thì giữ lại không ghi audit trail
[ContractTerm cũ] --> (Có trong DTO?)
                        |
                        |-- Có --> [Giữ lại]
                        |-- Không --> [DELETE] --

*/


        /* danh sách term mới sau khi update của hợp đồng

         * Term mới (CREATE).

         * Term cập nhật (UPDATE).

         * Term không thay đổi (NO ACTION nhưng vẫn giữ lại).

         */
        List<ContractTerm> updatedTerms = new ArrayList<>();

        //theo dõi các term ID hiện có từ DTO, tập hợp tất cả OriginalTermId từ DTO, dùng để xác định term nào bị xóa.
        Set<Long> newTermIds = new HashSet<>();

        //lưu audit trail cho các thay đổi của term
        List<AuditTrail> termAuditTrails = new ArrayList<>();

        // Xử lý LegalBasisTerms
        if (dto.getLegalBasisTerms() != null) {
            for (TermSnapshotDTO termDTO : dto.getLegalBasisTerms()) {

                // check tồn tại term trong database
                Term term = termRepository.findById(termDTO.getId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy Term với id: " + termDTO.getId()));

                // validate loại term
                if (!term.getTypeTerm().getIdentifier().equals(TypeTermIdentifier.LEGAL_BASIS)) {
                    throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại LEGAL_BASIS");
                }

                // Đánh dấu term ID đang được xử lý để theo dõi
                newTermIds.add(term.getId());

                //tìm trong hợp đồng hiện tại xem đã có term này chưa
                ContractTerm existingTerm = currentContract.getContractTerms().stream()
                        .filter(t -> t.getOriginalTermId().equals(term.getId()) && t.getTermType().equals(TypeTermIdentifier.LEGAL_BASIS))
                        .findFirst()
                        .orElse(null);
                //CREATE: Nếu existingTerm == null → nghĩa là term này không có trong contract_term nên thêm vào ContractTerm. => tạo audit trail
                //UPDATE: Nếu existingTerm != null và có thay đổi giá trị → nghĩa là term này có trong contract_term và có thay đổi giá trị nên Cập nhật. => tạo audit trail
                //        Nếu existingTerm != null không có thay đổi giá trị → nghĩa là term này có trong contract_term nên ta không tạo audit trail.

                // chuẩn bị giá trị cho audit trail
                String oldValue = null;
                String newValue = String.format("Term ID: %d, Label: %s, Value: %s, Type: %s",
                        term.getId(), term.getLabel(), term.getValue(), TypeTermIdentifier.LEGAL_BASIS.name());


                if (existingTerm != null) {
                    // Xử lý term đã tồn tại trong contract term
                    oldValue = String.format("Term ID: %d, Label: %s, Value: %s, Type: %s",
                            existingTerm.getOriginalTermId(), existingTerm.getTermLabel(), existingTerm.getTermValue(), existingTerm.getTermType().name());

                    // Xử lý term đã tồn tại trong contract term và có thay đổi giá trị
                    if (!oldValue.equals(newValue)) {
                        //UPDATE: Nếu existingTerm != null và có thay đổi giá trị → Cập nhật và ghi audit trail
                        // Cập nhật thông tin term
                        ContractTerm newTerm = ContractTerm.builder()
                                .originalTermId(term.getId())
                                .termLabel(term.getLabel())
                                .termValue(term.getValue())
                                .termType(TypeTermIdentifier.LEGAL_BASIS)
                                .contract(newContract)
                                .build();
                        updatedTerms.add(newTerm);

                        // Tạo audit trail UPDATE
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
                                .changeSummary("Đã cập nhật điều khoản cơ sở pháp lý với Term ID: " +
                                        term.getId() + " của hợp đồng " + currentContract.getContractNumber())
                                .build());
                    } else {

                        //existingTerm != null không có thay đổi giá trị → nghĩa là term này có trong contract_term nên ta không tạo audit trail
                        //ghi vào updatedTerms để theo dõi term update
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

                    //CREATE: existingTerm == null → nghĩa là term này không có trong contract_term nên thêm vào ContractTerm. => tạo audit trail
                    // Xử lý CREATE term
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
                            .entityId(null) // ID sẽ được cập nhật sau khi lưu
                            .action("CREATE")
                            .fieldName("legalBasisTerms")
                            .oldValue(null)
                            .newValue(newValue)
                            .changedAt(now)
                            .changedBy(changedBy)
                            .changeSummary("Đã tạo điều khoản cơ sở pháp lý với Term ID: " + term.getId()
                                    + " của hợp đồng " + currentContract.getContractNumber())
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
                                .changeSummary("Đã cập nhật điều khoản chung với Term ID: " + term.getId()
                                        + " của hợp đồng " + currentContract.getContractNumber())
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
                            .changeSummary("Đã tạo điều khoản chung với Term ID: " + term.getId()
                                    + " của hợp đồng " + currentContract.getContractNumber())
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
                                .changeSummary("Đã cập nhật điều khoản khác với Term ID: " + term.getId()
                                        + "trong hợp đồng ID: " + newContract.getId())
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
                            .changeSummary("Đã tạo điều khoản khác với Term ID: " + term.getId()
                                    + " của hợp đồng " + currentContract.getContractNumber())
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
                        .changeSummary("Đã xóa điều khoản với Term ID: " + oldTerm.getOriginalTermId()
                                + " của hợp đồng " + currentContract.getContractNumber())
                        .build());
            }
        }
        newContract.setContractTerms(updatedTerms);

        // 6. Xử lý ContractAdditionalTermDetail

        //danh sách chi tiết điều khoản bổ sung sau khi cập nhật
        List<ContractAdditionalTermDetail> updatedDetails = new ArrayList<>();

        //Danh sách tạm để lưu Audit Trail cho phần điều khoản bổ sung
        List<AuditTrail> additionalTermAuditTrails = new ArrayList<>();

        if (dto.getAdditionalConfig() != null) {

            // lấy config từ DTO (Map<typeTermId, Map<nhóm, List<Term>> => {1: {A: (A1, A2, A3), B: (B1, B2, B3), Common: (C1, C2, C3)}}
            Map<String, Map<String, List<TermSnapshotDTO>>> configMap = dto.getAdditionalConfig();

            //tập hợp tất cả typeTermId từ DTO để kiểm tra xóa.
            Set<Long> newTypeTermIds = configMap.keySet().stream().map(Long::parseLong).collect(Collectors.toSet());

            // Duyệt qua từng entry trong configMap (theo từng Map typeTermId)
            for (Map.Entry<String, Map<String, List<TermSnapshotDTO>>> entry : configMap.entrySet()) {

                // Lấy typeTermId từ key của Map
                Long configTypeTermId = Long.parseLong(entry.getKey());

                // Lấy cấu hình nhóm (Common, A, B) cho typeTermId này
                Map<String, List<TermSnapshotDTO>> groupConfig = entry.getValue();

                // Xử lý các nhóm Common, A, B
                List<AdditionalTermSnapshot> commonSnapshots = new ArrayList<>();
                List<AdditionalTermSnapshot> aSnapshots = new ArrayList<>();
                List<AdditionalTermSnapshot> bSnapshots = new ArrayList<>();

                // Xử lý nhóm Common trong dto
                if (groupConfig.containsKey("Common")) {
                    commonSnapshots = groupConfig.get("Common").stream()
                            .map(termDTO -> {

                                // Kiểm tra term có tồn tại trong database không
                                Term term = termRepository.findById(termDTO.getId())
                                        .orElseThrow(() -> new RuntimeException("Không tìm thấy Term với id: " + termDTO.getId()));

                                // Tạo snapshot từ term
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
                // Tạo newDetail tạm thời để so sánh
                ContractAdditionalTermDetail newDetail = ContractAdditionalTermDetail.builder()
                        .contract(newContract)
                        .typeTermId(configTypeTermId)
                        .commonTerms(commonSnapshots)
                        .aTerms(aSnapshots)
                        .bTerms(bSnapshots)
                        .build();
                updatedDetails.add(newDetail);

                //Tìm hoặc tạo ContractAdditionalTermDetail trong contract (Term của hợp đồng hiện tại)
                ContractAdditionalTermDetail oldDetail = currentContract.getAdditionalTermDetails().stream()
                        //tìm ra điều khoản bổ sung trong contract có giống với điều khoản bổ sung trong dto không (Theo typeTermId)
                        .filter(d -> d.getTypeTermId().equals(configTypeTermId))
                        .findFirst()
                        .orElse(null); // Không tìm thấy -> nghĩa là điều khoản bổ sung mới chưa có trong hợp đồng

                // CREATE: detail = null, Nếu không tìm thấy -> tạo mới => tạo audit trail CREATE
                // UPDATE: detail != null, Nếu tìm thấy và giá trị cũ của hợp đồng khác với giá trị mới thì ta sẽ cập nhật => tạo audit trail UPDATE
                //         detail != null, Nếu tìm thấy và giá trị cũ của hợp đồng giống với giá trị mới thì ta không tạo audit trail./

                // Chuẩn bị giá trị cũ cho audit trail
                String oldValue = oldDetail != null ? serializeAdditionalTermDetail(oldDetail) : null;

                String newValue = serializeAdditionalTermDetail(newDetail);

                if (oldDetail == null) {

                    //Term additional detail nay chua co trong hop dong
                    //=> CREATE
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
                            .changeSummary("Đã tạo chi tiết điều khoản bổ sung với TypeTerm ID: " + configTypeTermId
                                    + " của hợp đồng " + currentContract.getContractNumber())
                            .build());

                }
                else if (!oldValue.equals(newValue)) {
                    //hợp đồng đã có term này rồi nhưng khác giá trị
                    //=> UPDATE
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
                            .changeSummary("Đã cập nhật chi tiết điều khoản bổ sung với TypeTerm ID: " + configTypeTermId
                                    + " của hợp đồng " + currentContract.getContractNumber())
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
                            .changeSummary("Đã xóa chi tiết điều khoản bổ sung với TypeTerm ID: " + oldDetail.getTypeTermId()
                                    + " của hợp đồng " + currentContract.getContractNumber())
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
        Map<AuditTrail, PaymentSchedule> auditTrailToPaymentMap = new HashMap<>(); // Lưu ánh xạ tạm thời

        // Debug dữ liệu đầu vào
        System.out.println("DTO Payments:");
        if (dto.getPayments() != null) {
            dto.getPayments().forEach(paymentDTO -> System.out.println("PaymentDTO: ID=" + paymentDTO.getId() + ", Order=" + paymentDTO.getPaymentOrder()));
        } else {
            System.out.println("dto.getPayments() is null");
        }

        if (dto.getPayments() != null) {
            Set<Long> newPaymentIds = dto.getPayments().stream()
                    .filter(p -> p.getId() != null)
                    .map(PaymentScheduleDTO::getId)
                    .collect(Collectors.toSet());

            for (PaymentScheduleDTO paymentDTO : dto.getPayments()) {
                PaymentSchedule oldPayment = paymentDTO.getId() != null ? currentContract.getPaymentSchedules().stream()
                        .filter(p -> p.getId().equals(paymentDTO.getId()))
                        .findFirst()
                        .orElse(null) : null;

                PaymentSchedule newPayment = new PaymentSchedule();
                newPayment.setContract(newContract);

                if (oldPayment != null) {
                    // Sao chép dữ liệu từ oldPayment hoặc DTO
                    newPayment.setPaymentOrder(paymentDTO.getPaymentOrder() != null ? paymentDTO.getPaymentOrder() : oldPayment.getPaymentOrder());
                    newPayment.setAmount(paymentDTO.getAmount() != null ? paymentDTO.getAmount() : oldPayment.getAmount());
                    newPayment.setNotifyPaymentDate(paymentDTO.getNotifyPaymentDate() != null ? paymentDTO.getNotifyPaymentDate() : oldPayment.getNotifyPaymentDate());
                    newPayment.setPaymentDate(paymentDTO.getPaymentDate() != null ? paymentDTO.getPaymentDate() : oldPayment.getPaymentDate());
                    newPayment.setStatus(paymentDTO.getStatus() != null ? paymentDTO.getStatus() : oldPayment.getStatus());
                    newPayment.setPaymentMethod(paymentDTO.getPaymentMethod() != null ? paymentDTO.getPaymentMethod() : oldPayment.getPaymentMethod());
                    newPayment.setNotifyPaymentContent(paymentDTO.getNotifyPaymentContent() != null ? paymentDTO.getNotifyPaymentContent() : oldPayment.getNotifyPaymentContent());
                    newPayment.setReminderEmailSent(paymentDTO.isReminderEmailSent());
                    newPayment.setOverdueEmailSent(paymentDTO.isOverdueEmailSent());

                    // Kiểm tra thay đổi
                    boolean hasChanges = !Objects.equals(oldPayment.getPaymentOrder(), newPayment.getPaymentOrder()) ||
                            !Objects.equals(oldPayment.getAmount(), newPayment.getAmount()) ||
                            !Objects.equals(oldPayment.getNotifyPaymentDate(), newPayment.getNotifyPaymentDate()) ||
                            !Objects.equals(oldPayment.getPaymentDate(), newPayment.getPaymentDate()) ||
                            !Objects.equals(oldPayment.getStatus(), newPayment.getStatus()) ||
                            !Objects.equals(oldPayment.getPaymentMethod(), newPayment.getPaymentMethod()) ||
                            !Objects.equals(oldPayment.getNotifyPaymentContent(), newPayment.getNotifyPaymentContent()) ||
                            oldPayment.isReminderEmailSent() != newPayment.isReminderEmailSent() ||
                            oldPayment.isOverdueEmailSent() != newPayment.isOverdueEmailSent();

                    updatedPayments.add(newPayment);

                    if (hasChanges) {
                        String oldValue = serializePaymentSchedule(oldPayment);
                        String newValue = serializePaymentSchedule(newPayment);
                        AuditTrail auditTrail = AuditTrail.builder()
                                .contract(newContract)
                                .entityName("PaymentSchedule")
                                .entityId(oldPayment.getId()) // ID gốc để tham chiếu
                                .action("UPDATE")
                                .fieldName("paymentSchedules")
                                .oldValue(oldValue)
                                .newValue(newValue)
                                .changedAt(now)
                                .changedBy(changedBy)
                                .changeSummary("Đã cập nhật PaymentSchedule với ID gốc: " + oldPayment.getId() + " trong phiên bản mới")
                                .build();
                        paymentAuditTrails.add(auditTrail);
                        auditTrailToPaymentMap.put(auditTrail, newPayment); // Lưu ánh xạ
                        System.out.println("Added to map (UPDATE): " + newValue);
                    }
                } else {
                    // Tạo mới PaymentSchedule
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

                    String newValue = serializePaymentSchedule(newPayment);
                    AuditTrail auditTrail = AuditTrail.builder()
                            .contract(newContract)
                            .entityName("PaymentSchedule")
                            .entityId(null)
                            .action("CREATE")
                            .fieldName("paymentSchedules")
                            .oldValue(null)
                            .newValue(newValue)
                            .changedAt(now)
                            .changedBy(changedBy)
                            .changeSummary("Đã tạo PaymentSchedule mới")
                            .build();
                    paymentAuditTrails.add(auditTrail);
                    auditTrailToPaymentMap.put(auditTrail, newPayment); // Lưu ánh xạ
                    System.out.println("Added to map (CREATE): " + newValue);
                }
            }

            for (PaymentSchedule oldPayment : currentContract.getPaymentSchedules()) {
                if (!newPaymentIds.contains(oldPayment.getId())) {
                    String oldValue = serializePaymentSchedule(oldPayment);
                    AuditTrail auditTrail = AuditTrail.builder()
                            .contract(newContract)
                            .entityName("PaymentSchedule")
                            .entityId(oldPayment.getId())
                            .action("DELETE")
                            .fieldName("paymentSchedules")
                            .oldValue(oldValue)
                            .newValue(null)
                            .changedAt(now)
                            .changedBy(changedBy)
                            .changeSummary("Đã xóa PaymentSchedule với ID: " + oldPayment.getId() + " trong phiên bản mới")
                            .build();
                    paymentAuditTrails.add(auditTrail);
                    // Không cần thêm vào map vì DELETE không cần cập nhật newValue
                }
            }
        } else {
            for (PaymentSchedule oldPayment : currentContract.getPaymentSchedules()) {
                PaymentSchedule newPayment = new PaymentSchedule();
                newPayment.setContract(newContract);
                newPayment.setPaymentOrder(oldPayment.getPaymentOrder());
                newPayment.setAmount(oldPayment.getAmount());
                newPayment.setNotifyPaymentDate(oldPayment.getNotifyPaymentDate());
                newPayment.setPaymentDate(oldPayment.getPaymentDate());
                newPayment.setStatus(oldPayment.getStatus());
                newPayment.setPaymentMethod(oldPayment.getPaymentMethod());
                newPayment.setNotifyPaymentContent(oldPayment.getNotifyPaymentContent());
                newPayment.setReminderEmailSent(oldPayment.isReminderEmailSent());
                newPayment.setOverdueEmailSent(oldPayment.isOverdueEmailSent());
                updatedPayments.add(newPayment);
            }
        }
        newContract.setPaymentSchedules(updatedPayments);
        Contract savedNewContract = contractRepository.save(newContract);

        System.out.println("PaymentSchedules in savedNewContract:");
        savedNewContract.getPaymentSchedules().forEach(p -> System.out.println(serializePaymentSchedule(p)));

//        for (AuditTrail auditTrail : paymentAuditTrails) {
//            if ("CREATE".equals(auditTrail.getAction()) && auditTrail.getEntityId() == null) {
//                String newValue = auditTrail.getNewValue();
//                PaymentSchedule savedPayment = savedNewContract.getPaymentSchedules().stream()
//                        .filter(p -> serializePaymentSchedule(p).equals(newValue))
//                        .findFirst()
//                        .orElse(null);
//                if (savedPayment != null) {
//                    auditTrail.setEntityId(savedPayment.getId());
//                } else {
//                    System.err.println("Không tìm thấy PaymentSchedule tương ứng cho audit trail: " + newValue);
//                }
//            }
//            auditTrail.setContract(savedNewContract); // Đảm bảo contract được gán đúng
//        }
        // 8. Lưu hợp đồng mới

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
//        if (dto.getStatus() != null && !dto.getStatus().equals(currentContract.getStatus())) {
//            auditTrails.add(createAuditTrail(savedNewContract, "status", currentContract.getStatus().name(), dto.getStatus().name(), now, changedBy, "UPDATE", "Cập nhật trạng thái hợp đồng"));
//        }

        if (dto.getContractTypeId() != null && !dto.getContractTypeId().equals(currentContract.getContractType().getId())) {
            auditTrails.add(createAuditTrail(savedNewContract, "contractTypeId",
                    currentContract.getContractType().getId().toString(),
                    dto.getContractTypeId().toString(),
                    now, changedBy, "UPDATE", "Cập nhật loại hợp đồng"));
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
                PaymentSchedule newPayment = auditTrailToPaymentMap.get(auditTrail);
                if (newPayment != null) {
                    PaymentSchedule savedPayment = savedNewContract.getPaymentSchedules().stream()
                            .filter(p -> p.getPaymentOrder().equals(newPayment.getPaymentOrder())
                                    && p.getAmount().equals(newPayment.getAmount())
                                    && p.getNotifyPaymentDate().equals(newPayment.getNotifyPaymentDate()))
                            .findFirst()
                            .orElse(null);
                    if (savedPayment != null) {
                        auditTrail.setEntityId(savedPayment.getId());
                        auditTrail.setNewValue(serializePaymentSchedule(savedPayment));
                        System.out.println("Updated audit trail: " + auditTrail.getNewValue());
                    } else {
                        System.err.println("Không tìm thấy PaymentSchedule tương ứng trong savedNewContract cho audit trail: " + auditTrail.getNewValue());
                    }
                } else {
                    System.err.println("Không tìm thấy PaymentSchedule trong map cho audit trail: " + auditTrail.getNewValue());
                }
            }
            auditTrail.setContract(savedNewContract);
        }

        // 10. Lưu tất cả audit trails
        auditTrailRepository.saveAll(auditTrails);
        auditTrailRepository.saveAll(termAuditTrails);
        auditTrailRepository.saveAll(additionalTermAuditTrails);
        auditTrailRepository.saveAll(paymentAuditTrails);

        if(auditTrails.isEmpty() && termAuditTrails.isEmpty() && additionalTermAuditTrails.isEmpty() && paymentAuditTrails.isEmpty()) {
            return savedNewContract;
        }
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

    private boolean hasChanges(Contract currentContract, ContractUpdateDTO dto) {
        // Kiểm tra từng trường trong DTO so với currentContract
        if (dto.getTitle() != null && !dto.getTitle().equals(currentContract.getTitle())) return true;
        if (dto.getSigningDate() != null && !Objects.equals(dto.getSigningDate(), currentContract.getSigningDate())) return true;
        if (dto.getContractLocation() != null && !dto.getContractLocation().equals(currentContract.getContractLocation())) return true;
        if (dto.getAmount() != null && !Objects.equals(dto.getAmount(), currentContract.getAmount())) return true;
        if (dto.getEffectiveDate() != null && !Objects.equals(dto.getEffectiveDate(), currentContract.getEffectiveDate())) return true;
        if (dto.getExpiryDate() != null && !Objects.equals(dto.getExpiryDate(), currentContract.getExpiryDate())) return true;
        if (dto.getNotifyEffectiveDate() != null && !Objects.equals(dto.getNotifyEffectiveDate(), currentContract.getNotifyEffectiveDate())) return true;
        if (dto.getNotifyExpiryDate() != null && !Objects.equals(dto.getNotifyExpiryDate(), currentContract.getNotifyExpiryDate())) return true;
        if (dto.getNotifyEffectiveContent() != null && !dto.getNotifyEffectiveContent().equals(currentContract.getNotifyEffectiveContent())) return true;
        if (dto.getNotifyExpiryContent() != null && !dto.getNotifyExpiryContent().equals(currentContract.getNotifyExpiryContent())) return true;
        if (dto.getSpecialTermsA() != null && !dto.getSpecialTermsA().equals(currentContract.getSpecialTermsA())) return true;
        if (dto.getSpecialTermsB() != null && !dto.getSpecialTermsB().equals(currentContract.getSpecialTermsB())) return true;
        if (dto.getContractContent() != null && !dto.getContractContent().equals(currentContract.getContractContent())) return true;
        if (dto.getAppendixEnabled() != null && !dto.getAppendixEnabled().equals(currentContract.getAppendixEnabled())) return true;
        if (dto.getTransferEnabled() != null && !dto.getTransferEnabled().equals(currentContract.getTransferEnabled())) return true;
        if (dto.getAutoAddVAT() != null && !dto.getAutoAddVAT().equals(currentContract.getAutoAddVAT())) return true;
        if (dto.getVatPercentage() != null && !Objects.equals(dto.getVatPercentage(), currentContract.getVatPercentage())) return true;
        if (dto.getIsDateLateChecked() != null && !dto.getIsDateLateChecked().equals(currentContract.getIsDateLateChecked())) return true;
        if (dto.getAutoRenew() != null && !dto.getAutoRenew().equals(currentContract.getAutoRenew())) return true;
        if (dto.getViolate() != null && !dto.getViolate().equals(currentContract.getViolate())) return true;
        if (dto.getSuspend() != null && !dto.getSuspend().equals(currentContract.getSuspend())) return true;
        if (dto.getSuspendContent() != null && !dto.getSuspendContent().equals(currentContract.getSuspendContent())) return true;
        if (dto.getMaxDateLate() != null && !Objects.equals(dto.getMaxDateLate(), currentContract.getMaxDateLate())) return true;
        if (dto.getStatus() != null && !dto.getStatus().equals(currentContract.getStatus())) return true;
        if (dto.getContractTypeId() != null && !dto.getContractTypeId().equals(currentContract.getContractType().getId())) {
            return true;
        }
        // Kiểm tra thay đổi trong terms
        if (dto.getLegalBasisTerms() != null && hasTermChanges(currentContract.getContractTerms(), dto.getLegalBasisTerms(), TypeTermIdentifier.LEGAL_BASIS)) return true;
        if (dto.getGeneralTerms() != null && hasTermChanges(currentContract.getContractTerms(), dto.getGeneralTerms(), TypeTermIdentifier.GENERAL_TERMS)) return true;
        if (dto.getOtherTerms() != null && hasTermChanges(currentContract.getContractTerms(), dto.getOtherTerms(), TypeTermIdentifier.OTHER_TERMS)) return true;

        // Kiểm tra thay đổi trong additional terms
        if (dto.getAdditionalConfig() != null && hasAdditionalTermChanges(currentContract.getAdditionalTermDetails(), dto.getAdditionalConfig())) return true;

        // Kiểm tra thay đổi trong payment schedules
        if (dto.getPayments() != null && hasPaymentChanges(currentContract.getPaymentSchedules(), dto.getPayments())) return true;

        return false; // Không có thay đổi
    }

    // Phương thức phụ để kiểm tra thay đổi trong terms
    private boolean hasTermChanges(List<ContractTerm> existingTerms, List<TermSnapshotDTO> dtoTerms, TypeTermIdentifier type) {
        // Tập hợp các term ID từ DTO
        Set<Long> dtoTermIds = dtoTerms.stream()
                .map(TermSnapshotDTO::getId)
                .collect(Collectors.toSet());

        // Tập hợp các term hiện tại theo type
        Map<Long, ContractTerm> existingTermMap = existingTerms.stream()
                .filter(t -> t.getTermType().equals(type))
                .collect(Collectors.toMap(ContractTerm::getOriginalTermId, t -> t));

        // Kiểm tra term bị xóa (có trong existing nhưng không có trong DTO)
        for (Long existingTermId : existingTermMap.keySet()) {
            if (!dtoTermIds.contains(existingTermId)) {
                return true; // Có term bị xóa
            }
        }

        // Kiểm tra term mới hoặc cập nhật
        for (TermSnapshotDTO dtoTerm : dtoTerms) {
            ContractTerm existingTerm = existingTermMap.get(dtoTerm.getId());
            if (existingTerm == null) {
                return true; // Term mới được thêm
            }

            // So sánh giá trị term
            Term term = termRepository.findById(dtoTerm.getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Term với id: " + dtoTerm.getId()));
            String dtoValue = String.format("Term ID: %d, Label: %s, Value: %s, Type: %s",
                    term.getId(), term.getLabel(), term.getValue(), type.name());
            String existingValue = String.format("Term ID: %d, Label: %s, Value: %s, Type: %s",
                    existingTerm.getOriginalTermId(), existingTerm.getTermLabel(), existingTerm.getTermValue(), existingTerm.getTermType().name());
            if (!dtoValue.equals(existingValue)) {
                return true; // Term có thay đổi
            }
        }

        return false; // Không có thay đổi
    }

    // Phương thức phụ để kiểm tra thay đổi trong additional terms
    private boolean hasAdditionalTermChanges(List<ContractAdditionalTermDetail> existingDetails, Map<String, Map<String, List<TermSnapshotDTO>>> newConfig) {
        // Tập hợp typeTermId từ DTO
        Set<Long> newTypeTermIds = newConfig.keySet().stream()
                .map(Long::parseLong)
                .collect(Collectors.toSet());

        // Tập hợp existing details theo typeTermId
        Map<Long, ContractAdditionalTermDetail> existingDetailMap = existingDetails.stream()
                .collect(Collectors.toMap(ContractAdditionalTermDetail::getTypeTermId, d -> d));

        // Kiểm tra detail bị xóa (có trong existing nhưng không có trong DTO)
        for (Long existingTypeTermId : existingDetailMap.keySet()) {
            if (!newTypeTermIds.contains(existingTypeTermId)) {
                return true; // Có detail bị xóa
            }
        }

        // Kiểm tra detail mới hoặc cập nhật
        for (Map.Entry<String, Map<String, List<TermSnapshotDTO>>> entry : newConfig.entrySet()) {
            Long typeTermId = Long.parseLong(entry.getKey());
            Map<String, List<TermSnapshotDTO>> groupConfig = entry.getValue();

            ContractAdditionalTermDetail existingDetail = existingDetailMap.get(typeTermId);
            if (existingDetail == null) {
                return true; // Detail mới được thêm
            }

            // Tạo snapshot từ DTO để so sánh
            List<AdditionalTermSnapshot> commonSnapshots = groupConfig.getOrDefault("Common", Collections.emptyList()).stream()
                    .map(this::mapToSnapshot)
                    .collect(Collectors.toList());
            List<AdditionalTermSnapshot> aSnapshots = groupConfig.getOrDefault("A", Collections.emptyList()).stream()
                    .map(this::mapToSnapshot)
                    .collect(Collectors.toList());
            List<AdditionalTermSnapshot> bSnapshots = groupConfig.getOrDefault("B", Collections.emptyList()).stream()
                    .map(this::mapToSnapshot)
                    .collect(Collectors.toList());

            // So sánh với existing detail
            if (!areSnapshotsEqual(existingDetail.getCommonTerms(), commonSnapshots) ||
                    !areSnapshotsEqual(existingDetail.getATerms(), aSnapshots) ||
                    !areSnapshotsEqual(existingDetail.getBTerms(), bSnapshots)) {
                return true; // Có thay đổi trong detail
            }
        }

        return false; // Không có thay đổi
    }

    // Helper method để map TermSnapshotDTO thành AdditionalTermSnapshot
    private AdditionalTermSnapshot mapToSnapshot(TermSnapshotDTO dto) {
        Term term = termRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Term với id: " + dto.getId()));
        return AdditionalTermSnapshot.builder()
                .termId(term.getId())
                .termLabel(term.getLabel())
                .termValue(term.getValue())
                .build();
    }

    // Helper method để so sánh danh sách snapshots
    private boolean areSnapshotsEqual(List<AdditionalTermSnapshot> list1, List<AdditionalTermSnapshot> list2) {
        if (list1.size() != list2.size()) return false;
        Set<Long> ids1 = list1.stream().map(AdditionalTermSnapshot::getTermId).collect(Collectors.toSet());
        Set<Long> ids2 = list2.stream().map(AdditionalTermSnapshot::getTermId).collect(Collectors.toSet());
        if (!ids1.equals(ids2)) return false;

        for (AdditionalTermSnapshot s1 : list1) {
            AdditionalTermSnapshot s2 = list2.stream()
                    .filter(s -> s.getTermId().equals(s1.getTermId()))
                    .findFirst()
                    .orElse(null);
            if (s2 == null || !s1.getTermLabel().equals(s2.getTermLabel()) || !s1.getTermValue().equals(s2.getTermValue())) {
                return false;
            }
        }
        return true;
    }

    // Phương thức phụ để kiểm tra thay đổi trong payment schedules
    private boolean hasPaymentChanges(List<PaymentSchedule> existingPayments, List<PaymentScheduleDTO> dtoPayments) {
        // Tập hợp payment ID từ DTO
        Set<Long> dtoPaymentIds = dtoPayments.stream()
                .filter(p -> p.getId() != null)
                .map(PaymentScheduleDTO::getId)
                .collect(Collectors.toSet());

        // Tập hợp existing payments theo ID
        Map<Long, PaymentSchedule> existingPaymentMap = existingPayments.stream()
                .collect(Collectors.toMap(PaymentSchedule::getId, p -> p));

        // Kiểm tra payment bị xóa (có trong existing nhưng không có trong DTO)
        for (Long existingPaymentId : existingPaymentMap.keySet()) {
            if (!dtoPaymentIds.contains(existingPaymentId)) {
                return true; // Có payment bị xóa
            }
        }

        // Kiểm tra payment mới hoặc cập nhật
        for (PaymentScheduleDTO dtoPayment : dtoPayments) {
            if (dtoPayment.getId() == null) {
                return true; // Payment mới được thêm
            }

            PaymentSchedule existingPayment = existingPaymentMap.get(dtoPayment.getId());
            if (existingPayment == null) {
                return true; // Payment trong DTO không tồn tại trong existing
            }

            // So sánh các trường
            if (!Objects.equals(existingPayment.getPaymentOrder(), dtoPayment.getPaymentOrder()) ||
                    !Objects.equals(existingPayment.getAmount(), dtoPayment.getAmount()) ||
                    !Objects.equals(existingPayment.getNotifyPaymentDate(), dtoPayment.getNotifyPaymentDate()) ||
                    !Objects.equals(existingPayment.getPaymentDate(), dtoPayment.getPaymentDate()) ||
                    !Objects.equals(existingPayment.getStatus(), dtoPayment.getStatus()) ||
                    !Objects.equals(existingPayment.getPaymentMethod(), dtoPayment.getPaymentMethod()) ||
                    !Objects.equals(existingPayment.getNotifyPaymentContent(), dtoPayment.getNotifyPaymentContent()) ||
                    existingPayment.isReminderEmailSent() != dtoPayment.isReminderEmailSent() ||
                    existingPayment.isOverdueEmailSent() != dtoPayment.isOverdueEmailSent()) {
                return true; // Có thay đổi trong payment
            }
        }

        return false; // Không có thay đổi
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


    @Transactional
    @Override
    public Contract rollbackContract(Long originalContractId, int targetVersion) {
        // 1. Tìm hợp đồng cần rollback về
        Contract targetContract = contractRepository.findByOriginalContractIdAndVersion(originalContractId, targetVersion)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng với originalContractId: " + originalContractId + " và version: " + targetVersion));

        // 2. Tìm phiên bản hiện tại (mới nhất) để so sánh và ghi log
        Integer currentMaxVersion = contractRepository.findMaxVersionByOriginalContractId(originalContractId);
        if (currentMaxVersion == null || currentMaxVersion < targetVersion) {
            throw new RuntimeException("Phiên bản hiện tại không tồn tại hoặc nhỏ hơn phiên bản rollback");
        }

        Contract currentContract = contractRepository.findByOriginalContractIdAndVersion(originalContractId, currentMaxVersion)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên bản hiện tại"));

        // 3. Tính toán phiên bản mới
        int newVersion = currentMaxVersion + 1;

        LocalDateTime now = LocalDateTime.now();

        String changedBy = currentUser.getLoggedInUser().getFullName();

        // 4. Tạo hợp đồng mới dựa trên targetContract
        Contract rollbackContract = Contract.builder()
                .originalContractId(originalContractId)
                .version(newVersion)
                .signingDate(targetContract.getSigningDate())
                .contractLocation(targetContract.getContractLocation())
                .contractNumber(generateNewContractNumber(targetContract, newVersion)) // Tạo contractNumber mới
                .specialTermsA(targetContract.getSpecialTermsA())
                .specialTermsB(targetContract.getSpecialTermsB())
                .status(targetContract.getStatus())
                .createdAt(now)
                .updatedAt(now)
                .effectiveDate(targetContract.getEffectiveDate())
                .expiryDate(targetContract.getExpiryDate())
                .notifyEffectiveDate(targetContract.getNotifyEffectiveDate())
                .notifyExpiryDate(targetContract.getNotifyExpiryDate())
                .notifyEffectiveContent(targetContract.getNotifyEffectiveContent())
                .notifyExpiryContent(targetContract.getNotifyExpiryContent())
                .title(targetContract.getTitle())
                .amount(targetContract.getAmount())
                .user(targetContract.getUser())
                .isDateLateChecked(targetContract.getIsDateLateChecked())
                .template(targetContract.getTemplate())
                .partner(targetContract.getPartner())
                .appendixEnabled(targetContract.getAppendixEnabled())
                .transferEnabled(targetContract.getTransferEnabled())
                .autoAddVAT(targetContract.getAutoAddVAT())
                .vatPercentage(targetContract.getVatPercentage())
                .autoRenew(targetContract.getAutoRenew())
                .violate(targetContract.getViolate())
                .suspend(targetContract.getSuspend())
                .suspendContent(targetContract.getSuspendContent())
                .contractContent(targetContract.getContractContent())
                .approvalWorkflow(targetContract.getApprovalWorkflow())
                .maxDateLate(targetContract.getMaxDateLate())
                .contractType(targetContract.getContractType())
                .build();

        // 5. Sao chép ContractTerms
        List<ContractTerm> rollbackTerms = new ArrayList<>();
        for (ContractTerm oldTerm : targetContract.getContractTerms()) {
            ContractTerm newTerm = ContractTerm.builder()
                    .originalTermId(oldTerm.getOriginalTermId())
                    .termLabel(oldTerm.getTermLabel())
                    .termValue(oldTerm.getTermValue())
                    .termType(oldTerm.getTermType())
                    .contract(rollbackContract)
                    .build();
            rollbackTerms.add(newTerm);
        }
        rollbackContract.setContractTerms(rollbackTerms);

        // 6. Sao chép ContractAdditionalTermDetails
        List<ContractAdditionalTermDetail> rollbackDetails = new ArrayList<>();
        for (ContractAdditionalTermDetail oldDetail : targetContract.getAdditionalTermDetails()) {
            ContractAdditionalTermDetail newDetail = ContractAdditionalTermDetail.builder()
                    .contract(rollbackContract)
                    .typeTermId(oldDetail.getTypeTermId())
                    .commonTerms(new ArrayList<>(oldDetail.getCommonTerms()))
                    .aTerms(new ArrayList<>(oldDetail.getATerms()))
                    .bTerms(new ArrayList<>(oldDetail.getBTerms()))
                    .build();
            rollbackDetails.add(newDetail);
        }
        rollbackContract.setAdditionalTermDetails(rollbackDetails);

        // 7. Sao chép PaymentSchedules
        List<PaymentSchedule> rollbackPayments = new ArrayList<>();
        for (PaymentSchedule oldPayment : targetContract.getPaymentSchedules()) {
            PaymentSchedule newPayment = new PaymentSchedule();
            newPayment.setContract(rollbackContract);
            newPayment.setPaymentOrder(oldPayment.getPaymentOrder());
            newPayment.setAmount(oldPayment.getAmount());
            newPayment.setNotifyPaymentDate(oldPayment.getNotifyPaymentDate());
            newPayment.setPaymentDate(oldPayment.getPaymentDate());
            newPayment.setStatus(oldPayment.getStatus());
            newPayment.setPaymentMethod(oldPayment.getPaymentMethod());
            newPayment.setNotifyPaymentContent(oldPayment.getNotifyPaymentContent());
            newPayment.setReminderEmailSent(oldPayment.isReminderEmailSent());
            newPayment.setOverdueEmailSent(oldPayment.isOverdueEmailSent());
            rollbackPayments.add(newPayment);
        }
        rollbackContract.setPaymentSchedules(rollbackPayments);

        // 8. Lưu hợp đồng rollback
        Contract savedRollbackContract = contractRepository.save(rollbackContract);

        // 9. Ghi audit trail cho hành động rollback
        AuditTrail rollbackAuditTrail = AuditTrail.builder()
                .contract(savedRollbackContract)
                .entityName("Contract")
                .entityId(savedRollbackContract.getId())
                .action("ROLLBACK")
                .fieldName("contract")
                .oldValue(serializeContract(currentContract))
                .newValue(serializeContract(savedRollbackContract))
                .changedAt(now)
                .changedBy(changedBy)
                .changeSummary("Đã rollback hợp đồng từ phiên bản " + currentMaxVersion + " về phiên bản " + targetVersion +
                        " (tạo phiên bản mới " + newVersion + ")")
                .build();
        auditTrailRepository.save(rollbackAuditTrail);

        return savedRollbackContract;
    }

    @Override
    @Transactional
    // ContractServiceImpl.java
    public List<ContractComparisonDTO> compareVersions(Long originalContractId, Integer version1, Integer version2) throws DataNotFoundException {
        Contract v1 = contractRepository.findByOriginalContractIdAndVersion(originalContractId, version1)
                .orElseThrow(() -> new DataNotFoundException("Version " + version1 + " not found"));

        Contract v2 = contractRepository.findByOriginalContractIdAndVersion(originalContractId, version2)
                .orElseThrow(() -> new DataNotFoundException("Version " + version2 + " not found"));

        List<ContractComparisonDTO> changes = new ArrayList<>();

        // Compare basic info
        compareBasicInfo(v1, v2, changes);

        // Compare contract terms
        compareContractTerms(v1.getContractTerms(), v2.getContractTerms(), changes);

        // Compare additional terms
        compareAdditionalTerms(v1.getAdditionalTermDetails(), v2.getAdditionalTermDetails(), changes);

        return changes;
    }

    private void compareBasicInfo(Contract v1, Contract v2, List<ContractComparisonDTO> changes) {
        compareField("Title", "BASIC", v1.getTitle(), v2.getTitle(), changes);
        compareField("Amount", "BASIC", v1.getAmount(), v2.getAmount(), changes);
        compareField("Status", "BASIC", v1.getStatus(), v2.getStatus(), changes);
        compareField("Effective Date", "BASIC", v1.getEffectiveDate(), v2.getEffectiveDate(), changes);
        // Add more fields as needed
    }

    private void compareContractTerms(List<ContractTerm> v1Terms, List<ContractTerm> v2Terms,
                                      List<ContractComparisonDTO> changes) {

        Map<Long, ContractTerm> v1Map = v1Terms.stream()
                .collect(Collectors.toMap(ContractTerm::getOriginalTermId, Function.identity()));

        for (ContractTerm v2Term : v2Terms) {
            ContractTerm v1Term = v1Map.get(v2Term.getOriginalTermId());

            if (v1Term == null) {
                changes.add(createTermDTO(v2Term, "ADDED", null));
                continue;
            }

            if (!Objects.equals(v1Term.getTermValue(), v2Term.getTermValue())) {
                changes.add(createTermDTO(v2Term, "MODIFIED", v1Term.getTermValue()));
            }

            v1Map.remove(v2Term.getOriginalTermId());
        }

        // Remaining terms in v1Map are removed
        v1Map.values().forEach(term ->
                changes.add(createTermDTO(term, "REMOVED", null)));
    }

    private void compareAdditionalTerms(List<ContractAdditionalTermDetail> v1Details,
                                        List<ContractAdditionalTermDetail> v2Details,
                                        List<ContractComparisonDTO> changes) {

        // Compare Common terms
        compareTermGroup("COMMON",
                v1Details.stream().flatMap(d -> d.getCommonTerms().stream()).collect(Collectors.toList()),
                v2Details.stream().flatMap(d -> d.getCommonTerms().stream()).collect(Collectors.toList()),
                changes);

        // Compare A terms
        compareTermGroup("A",
                v1Details.stream().flatMap(d -> d.getATerms().stream()).collect(Collectors.toList()),
                v2Details.stream().flatMap(d -> d.getATerms().stream()).collect(Collectors.toList()),
                changes);

        // Compare B terms
        compareTermGroup("B",
                v1Details.stream().flatMap(d -> d.getBTerms().stream()).collect(Collectors.toList()),
                v2Details.stream().flatMap(d -> d.getBTerms().stream()).collect(Collectors.toList()),
                changes);
    }

    private void compareTermGroup(String groupName,
                                  List<AdditionalTermSnapshot> v1Terms,
                                  List<AdditionalTermSnapshot> v2Terms,
                                  List<ContractComparisonDTO> changes) {

        Map<Long, AdditionalTermSnapshot> v1Map = v1Terms.stream()
                .collect(Collectors.toMap(AdditionalTermSnapshot::getTermId, Function.identity()));

        for (AdditionalTermSnapshot v2Term : v2Terms) {
            AdditionalTermSnapshot v1Term = v1Map.get(v2Term.getTermId());

            if (v1Term == null) {
                changes.add(createAdditionalTermDTO(v2Term, groupName, "ADDED", null));
                continue;
            }

            if (!Objects.equals(v1Term.getTermValue(), v2Term.getTermValue())) {
                changes.add(createAdditionalTermDTO(v2Term, groupName, "MODIFIED", v1Term.getTermValue()));
            }

            v1Map.remove(v2Term.getTermId());
        }

        // Remaining terms are removed
        v1Map.values().forEach(term ->
                changes.add(createAdditionalTermDTO(term, groupName, "REMOVED", null)));
    }

    private ContractComparisonDTO createTermDTO(ContractTerm term, String changeType, Object oldValue) {
        return ContractComparisonDTO.builder()
                .fieldName(term.getTermLabel())
                .fieldType("TERM")
                .oldValue(oldValue)
                .newValue(term.getTermValue())
                .changeType(changeType)
                .build();
    }

    private ContractComparisonDTO createAdditionalTermDTO(AdditionalTermSnapshot term, String groupName,
                                                          String changeType, Object oldValue) {
        return ContractComparisonDTO.builder()
                .fieldName(term.getTermLabel())
                .fieldType("ADDITIONAL_TERM")
                .groupName(groupName)
                .oldValue(oldValue)
                .newValue(term.getTermValue())
                .changeType(changeType)
                .build();
    }

    private void compareField(String fieldName, String fieldType,
                              Object v1Value, Object v2Value,
                              List<ContractComparisonDTO> changes) {
        if (!Objects.equals(v1Value, v2Value)) {
            changes.add(ContractComparisonDTO.builder()
                    .fieldName(fieldName)
                    .fieldType(fieldType)
                    .oldValue(v1Value)
                    .newValue(v2Value)
                    .changeType("MODIFIED")
                    .build());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GetAllContractReponse> getAllVersionsByOriginalContractId(Long originalContractId, Pageable pageable, User currentUser) {
        boolean isCeo = Boolean.TRUE.equals(currentUser.getIsCeo());
        boolean isStaff = currentUser.getRole() != null && "STAFF".equalsIgnoreCase(currentUser.getRole().getRoleName());

        Page<Contract> versions;
        if (isStaff && !isCeo) {
            // Nếu là STAFF (không phải CEO), chỉ lấy các phiên bản của user hiện tại
            versions = contractRepository.findAllByOriginalContractIdAndUser(originalContractId, currentUser, pageable);
        } else if (isCeo) {
            // Nếu là CEO, lấy tất cả các phiên bản
            versions = contractRepository.findAllByOriginalContractId(originalContractId, pageable);
        } else {
            // Các role khác không được phép xem, trả về danh sách rỗng
            versions = new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        return versions.map(this::convertToGetAllContractResponse);
    }

    @Transactional(readOnly = true)
    public List<ContractResponse> getContractsByOriginalIdAndVersions(Long originalContractId, Integer version1, Integer version2) {
        List<Contract> contracts = contractRepository.findByOriginalContractIdAndVersionIn(
                originalContractId,
                Arrays.asList(version1, version2)
        );
        return contracts.stream()
                .map(this::convertContractToResponse)
                .collect(Collectors.toList());
    }

}

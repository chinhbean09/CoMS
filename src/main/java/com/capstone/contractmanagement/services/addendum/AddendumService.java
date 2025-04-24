package com.capstone.contractmanagement.services.addendum;

import com.capstone.contractmanagement.components.LocalizationUtils;
import com.capstone.contractmanagement.dtos.FileBase64DTO;
import com.capstone.contractmanagement.dtos.addendum.AddendumDTO;
import com.capstone.contractmanagement.dtos.addendum.AddendumTermSnapshotDTO;
import com.capstone.contractmanagement.dtos.approvalworkflow.AddendumApprovalWorkflowDTO;
import com.capstone.contractmanagement.dtos.approvalworkflow.WorkflowDTO;
import com.capstone.contractmanagement.dtos.contract.ContractItemDTO;
import com.capstone.contractmanagement.dtos.contract.ContractPartnerDTO;
import com.capstone.contractmanagement.dtos.contract.TermSnapshotDTO;
import com.capstone.contractmanagement.dtos.payment.PaymentDTO;
import com.capstone.contractmanagement.entities.*;
import com.capstone.contractmanagement.entities.addendum.*;
import com.capstone.contractmanagement.entities.approval_workflow.ApprovalStage;
import com.capstone.contractmanagement.entities.approval_workflow.ApprovalWorkflow;
import com.capstone.contractmanagement.entities.contract.*;
import com.capstone.contractmanagement.entities.term.Term;
import com.capstone.contractmanagement.enums.*;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.exceptions.InvalidParamException;
import com.capstone.contractmanagement.repositories.*;
import com.capstone.contractmanagement.responses.addendum.AddendumResponse;
import com.capstone.contractmanagement.responses.addendum.AddendumTypeResponse;
import com.capstone.contractmanagement.responses.addendum.UserAddendumResponse;
import com.capstone.contractmanagement.responses.approvalworkflow.ApprovalStageResponse;
import com.capstone.contractmanagement.responses.approvalworkflow.ApprovalWorkflowResponse;
import com.capstone.contractmanagement.responses.approvalworkflow.CommentResponse;
import com.capstone.contractmanagement.responses.contract.ContractResponse;
import com.capstone.contractmanagement.responses.payment_schedule.PaymentScheduleResponse;
import com.capstone.contractmanagement.responses.term.TermResponse;
import com.capstone.contractmanagement.responses.term.TypeTermResponse;
import com.capstone.contractmanagement.services.notification.INotificationService;
import com.capstone.contractmanagement.services.sendmails.IMailService;
import com.capstone.contractmanagement.utils.MessageKeys;
import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddendumService implements IAddendumService{
    private final IAddendumRepository addendumRepository;
    private final IContractRepository contractRepository;
    private final IApprovalWorkflowRepository approvalWorkflowRepository;
    private final IMailService mailService;
    private final SimpMessagingTemplate messagingTemplate;
    private final INotificationService notificationService;
    private final IApprovalStageRepository approvalStageRepository;
    private final IUserRepository userRepository;
    private final IPartnerRepository partnerRepository;
    private final IContractPartnerRepository contractPartnerRepository;
    private final IAuditTrailRepository auditTrailRepository;
    private final Cloudinary cloudinary;
    private final LocalizationUtils localizationUtils;
    private final ITermRepository termRepository;
    private final ITypeTermRepository typeTermRepository;
    private final IAddendumPaymentScheduleRepository addendumPaymentScheduleRepository;


    private static final Logger logger = LoggerFactory.getLogger(AddendumService.class);

    @Override
    @Transactional
    public AddendumResponse createAddendum(AddendumDTO addendumDTO) throws DataNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        // Lấy hợp đồng từ DTO
        Contract contract = contractRepository.findById(addendumDTO.getContractId())
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy hợp đồng"));

        // Kiểm tra xem có phụ lục nào với cùng title cho hợp đồng này chưa
        boolean isTitleExist = addendumRepository.existsByContractIdAndTitle(contract.getId(), addendumDTO.getTitle());
        if (isTitleExist) {
            throw new DataNotFoundException("Tên phụ lục bị trùng: " + addendumDTO.getTitle());
        }

        Partner partnerA = partnerRepository.findById(1L)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy bên A mặc định"));

        Optional<ContractPartner> contractPartners = contractPartnerRepository.findByContractIdAndPartnerType(addendumDTO.getContractId(), PartnerType.PARTNER_B);

        // Lấy loại phụ lục
//        AddendumType addendumType = addendumTypeRepository.findById(addendumDTO.getAddendumTypeId())
//                .orElseThrow(() -> new DataNotFoundException("Loại phụ lục không tìm thấy với id : " + addendumDTO.getAddendumTypeId()));

        Addendum savedAddendum = null;
        if (contract.getStatus() == ContractStatus.ACTIVE
                || contract.getStatus() == ContractStatus.EXPIRED) {
            // Tạo phụ lục mới

            Addendum addendum = Addendum.builder()
                    .title(addendumDTO.getTitle())
                    .content(addendumDTO.getContent())
                    .effectiveDate(addendumDTO.getEffectiveDate())
                    .extendContractDate(addendumDTO.getExtendContractDate()) // Thêm ngày gia hạn
                    .contractExpirationDate(addendumDTO.getContractExpirationDate()) // Thêm ngày hết hạn
                    .contractNumber(contract.getContractNumber())
                    .status(AddendumStatus.CREATED)
                    .user(currentUser)
                    .createdAt(LocalDateTime.now())
                    .isEffectiveNotified(false)
                    .isExpiryNotified(false)
                    .updatedAt(null)
                    .contractContent(addendumDTO.getContractContent())
//                    .addendumType(addendumType)
                    .contract(contract)
                    .build();

            // Lưu phụ lục
            List<AddendumTerm> addendumTerms = new ArrayList<>();

            if (addendumDTO.getContractItems() != null && !addendumDTO.getContractItems().isEmpty()) {
                List<AddendumItem> addendumItems = new ArrayList<>();
                int order = 1;
                for (ContractItemDTO itemDTO : addendumDTO.getContractItems()) {
                    if (itemDTO.getDescription() == null || itemDTO.getDescription().trim().isEmpty()) {
                        throw new IllegalArgumentException("Mô tả hạng mục không được để trống.");
                    }
                    if (itemDTO.getAmount() == null || itemDTO.getAmount() <= 0.0) {
                        throw new IllegalArgumentException("Số tiền hạng mục phải lớn hơn 0.");
                    }

                    AddendumItem item = AddendumItem.builder()
                            .addendum(addendum)
                            .description(itemDTO.getDescription())
                            .amount(itemDTO.getAmount())
                            .itemOrder(order++)
                            .build();
                    addendumItems.add(item);
                }
                addendum.setAddendumItems(addendumItems);
            }

            // Căn cứ pháp lý
            if (addendumDTO.getLegalBasisTerms() != null) {
                for (AddendumTermSnapshotDTO termDTO : addendumDTO.getLegalBasisTerms()) {
                    if (termDTO.getId() == null) {
                        throw new IllegalArgumentException("Điều khoản Căn cứ pháp lý không được để trống.");
                    }
                    Term term = termRepository.findById(termDTO.getId())
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy điều khoản với ID: " + termDTO.getId()));
                    if (!term.getTypeTerm().getIdentifier().equals(TypeTermIdentifier.LEGAL_BASIS)) {
                        throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại Căn cứ pháp lý.");
                    }
                    addendumTerms.add(AddendumTerm.builder()
                            .originalTermId(term.getId())
                            .termLabel(term.getLabel())
                            .termValue(term.getValue())
                            .termType(TypeTermIdentifier.LEGAL_BASIS)
                            .addendum(addendum)
                            .build());
                }
            }
            if (addendumDTO.getGeneralTerms() != null) {
                for (AddendumTermSnapshotDTO termDTO : addendumDTO.getGeneralTerms()) {
                    if (termDTO.getId() == null) {
                        throw new IllegalArgumentException("Điều khoản chung không được để trống.");
                    }
                    Term term = termRepository.findById(termDTO.getId())
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy điều khoản"));
                    if (!term.getTypeTerm().getIdentifier().equals(TypeTermIdentifier.GENERAL_TERMS)) {
                        throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại Điều khoản chung.");
                    }
                    addendumTerms.add(AddendumTerm.builder()
                            .originalTermId(term.getId())
                            .termLabel(term.getLabel())
                            .termValue(term.getValue())
                            .termType(TypeTermIdentifier.GENERAL_TERMS)
                            .addendum(addendum)
                            .build());
                }
            }

            if (addendumDTO.getOtherTerms() != null) {
                for (AddendumTermSnapshotDTO termDTO : addendumDTO.getOtherTerms()) {
                    if (termDTO.getId() == null) {
                        throw new IllegalArgumentException("Điều khoản khác không được để trống.");
                    }
                    Term term = termRepository.findById(termDTO.getId())
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy điều khoản"));
                    if (!term.getTypeTerm().getIdentifier().equals(TypeTermIdentifier.OTHER_TERMS)) {
                        throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại Điều khoản khác.");
                    }
                    addendumTerms.add(AddendumTerm.builder()
                            .originalTermId(term.getId())
                            .termLabel(term.getLabel())
                            .termValue(term.getValue())
                            .termType(TypeTermIdentifier.OTHER_TERMS)
                            .addendum(addendum)
                            .build());
                }
            }
            addendum.setAddendumTerms(addendumTerms);

            List<AddendumAdditionalTermDetail> additionalDetails = new ArrayList<>();
            if (addendumDTO.getAdditionalConfig() != null) {
                Map<String, Map<String, List<AddendumTermSnapshotDTO>>> configMap = addendumDTO.getAdditionalConfig();
                for (Map.Entry<String, Map<String, List<AddendumTermSnapshotDTO>>> entry : configMap.entrySet()) {
                    String key = entry.getKey();
                    Long configTypeTermId;
                    try {
                        configTypeTermId = Long.parseLong(key);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Key trong điều khoản bổ sung phải là số đại diện cho của loại điều khoản. Key không hợp lệ");
                    }
                    Map<String, List<AddendumTermSnapshotDTO>> groupConfig = entry.getValue();

                    // Map nhóm Common
                    List<AdditionalTermSnapshot> commonSnapshots = new ArrayList<>();
                    if (groupConfig.containsKey("Common")) {
                        for (AddendumTermSnapshotDTO termDTO : groupConfig.get("Common")) {
                            if (termDTO.getId() == null) {
                                throw new IllegalArgumentException("Điều khoản trong nhóm điều khoản chung không được để trống.");
                            }
                            Term term = termRepository.findById(termDTO.getId())
                                    .orElseThrow(() -> new RuntimeException("Không tìm thấy điều khoản"));
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
                        for (AddendumTermSnapshotDTO termDTO : groupConfig.get("A")) {
                            if (termDTO.getId() == null) {
                                throw new IllegalArgumentException("Điều khoản trong nhóm bên A không được để trống.");
                            }
                            Term term = termRepository.findById(termDTO.getId())
                                    .orElseThrow(() -> new RuntimeException("Không tìm thấy điều khoản"));
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
                        for (AddendumTermSnapshotDTO termDTO : groupConfig.get("B")) {
                            if (termDTO.getId() == null) {
                                throw new IllegalArgumentException("Điều khoản trong nhóm bên B không được để trống.");
                            }
                            Term term = termRepository.findById(termDTO.getId())
                                    .orElseThrow(() -> new RuntimeException("Không tìm thấy điều khoản"));
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
                        throw new IllegalArgumentException("Các điều khoản không được chọn đồng thời ở nhóm 'Bên Chung' và 'Bên A'.");
                    }
                    Set<Long> unionCommonB = new HashSet<>(commonSnapshots.stream().map(AdditionalTermSnapshot::getTermId).toList());
                    unionCommonB.retainAll(bSnapshots.stream().map(AdditionalTermSnapshot::getTermId).toList());
                    if (!unionCommonB.isEmpty()) {
                        throw new IllegalArgumentException("Các điều khoản không được chọn đồng thời ở nhóm 'Bên Chung' và 'Bên B'.");
                    }
                    Set<Long> unionAB = new HashSet<>(aSnapshots.stream().map(AdditionalTermSnapshot::getTermId).toList());
                    unionAB.retainAll(bSnapshots.stream().map(AdditionalTermSnapshot::getTermId).toList());
                    if (!unionAB.isEmpty()) {
                        throw new IllegalArgumentException("Các điều khoản không được chọn đồng thời ở nhóm 'Bên A' và 'Bên B'.");
                    }

                    // Kiểm tra type term
                    for (AdditionalTermSnapshot snap : commonSnapshots) {
                        Term term = termRepository.findById(snap.getTermId())
                                .orElseThrow(() -> new IllegalArgumentException("Không tồn tại điều khoản"));
                        if (!term.getTypeTerm().getId().equals(configTypeTermId)) {
                            throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại điều khoản: \"" + term.getTypeTerm().getName() + "\".");
                        }
                    }
                    for (AdditionalTermSnapshot snap : aSnapshots) {
                        Term term = termRepository.findById(snap.getTermId())
                                .orElseThrow(() -> new IllegalArgumentException("Không tồn tại điều khoản"));
                        if (!term.getTypeTerm().getId().equals(configTypeTermId)) {
                            throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại điều khoản: \"" + term.getTypeTerm().getName() + "\".");
                        }
                    }
                    for (AdditionalTermSnapshot snap : bSnapshots) {
                        Term term = termRepository.findById(snap.getTermId())
                                .orElseThrow(() -> new IllegalArgumentException("Không tồn tại điều khoản"));
                        if (!term.getTypeTerm().getId().equals(configTypeTermId)) {
                            throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại điều khoản: \"" + term.getTypeTerm().getName() + "\".");
                        }
                    }

                    AddendumAdditionalTermDetail configDetail = AddendumAdditionalTermDetail.builder()
                            .typeTermId(configTypeTermId)
                            .commonTerms(commonSnapshots)
                            .aTerms(aSnapshots)
                            .bTerms(bSnapshots)
                            .addendum(addendum)
                            .build();
                    additionalDetails.add(configDetail);
                }
            }
            addendum.setAdditionalTermDetails(additionalDetails);

            List<AddendumPaymentSchedule> paymentSchedules = new ArrayList<>();
            if (addendumDTO.getPayments() != null) {
                int order = 1;
                for (PaymentDTO paymentDTO : addendumDTO.getPayments()) {
                    if (paymentDTO.getAmount() == null || paymentDTO.getAmount() <= 0.0) {
                        throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn 0.");
                    }
                    if (paymentDTO.getPaymentDate() == null) {
                        throw new IllegalArgumentException("Ngày thanh toán không được để trống.");
                    }
                    AddendumPaymentSchedule paymentSchedule = AddendumPaymentSchedule.builder()
                            .amount(paymentDTO.getAmount())
                            .paymentDate(paymentDTO.getPaymentDate())
                            .notifyPaymentDate(paymentDTO.getNotifyPaymentDate())
                            .paymentOrder(order++)
                            .status(PaymentStatus.UNPAID)
                            .paymentMethod(paymentDTO.getPaymentMethod())
//                            .notifyPaymentContent(paymentDTO.getNotifyPaymentContent())
                            .reminderEmailSent(false)
                            .overdueEmailSent(false)
                            .addendum(addendum)
                            .build();
                    paymentSchedules.add(paymentSchedule);
                }
            }
            addendum.setPaymentSchedules(paymentSchedules);

            savedAddendum = addendumRepository.save(addendum);

            logAuditTrailForAddendum(addendum, "CREATE", "status", null, AddendumStatus.CREATED.name(), currentUser.getUsername());
            // Trả về thông tin phụ lục đã tạo
            return AddendumResponse.builder()
                    .addendumId(savedAddendum.getId())
                    .title(savedAddendum.getTitle())
                    .content(savedAddendum.getContent())
                    .contractNumber(savedAddendum.getContractNumber())
                    .status(savedAddendum.getStatus())
                    .createdBy(UserAddendumResponse.builder()
                            .userId(currentUser.getId())
                            .userName(currentUser.getUsername())
                            .build())
                    .contractId(savedAddendum.getContract().getId())
//                    .addendumType(AddendumTypeResponse.builder()
//                            .addendumTypeId(addendum.getAddendumType().getId())
//                            .name(addendum.getAddendumType().getName())
//                            .build())
                    .partnerA(partnerA)
                    .partnerB(contractPartners)
                    .extendContractDate(savedAddendum.getExtendContractDate())
                    .contractExpirationDate(savedAddendum.getContractExpirationDate())
                    .effectiveDate(savedAddendum.getEffectiveDate())
                    .createdAt(savedAddendum.getCreatedAt())
                    .updatedAt(savedAddendum.getUpdatedAt())
                    .build();


        }




        throw new DataNotFoundException("Không thể tạo phụ lục: Hợp đồng không HOẠT ĐỘNG");
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddendumResponse> getAllByContractId(Long contractId) throws DataNotFoundException {
        // Kiểm tra hợp đồng có tồn tại không
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy hợp đồng"));

        Partner partnerA = partnerRepository.findById(1L)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy bên A"));

        Optional<ContractPartner> contractPartners = contractPartnerRepository.findByContractIdAndPartnerType(contract.getId(), PartnerType.PARTNER_B);
        // Lấy danh sách phụ lục theo contract id (giả sử repository có method: findByContract_Id)
        List<Addendum> addenda = addendumRepository.findByContract(contract);

        // Nếu không có phụ lục, có thể trả về danh sách rỗng hoặc ném ngoại lệ
        if (addenda.isEmpty()) {
            throw new DataNotFoundException("Không tìm thấy phụ lục cho hợp đồng");
        }

        // Map entity thành DTO
        return addenda.stream()
                .map(addendum -> AddendumResponse.builder()
                        .addendumId(addendum.getId())
                        .title(addendum.getTitle())
                        .content(addendum.getContent())
                        .effectiveDate(addendum.getEffectiveDate())
                        .contractNumber(addendum.getContractNumber())
                        .status(addendum.getStatus())
                        .createdBy(UserAddendumResponse.builder()
                                .userId(addendum.getUser().getId())
                                .userName(addendum.getUser().getFullName())
                                .build())
                        .partnerA(partnerA)
                        .partnerB(contractPartners)
                        .contractId(addendum.getContract().getId())
                        .createdAt(addendum.getCreatedAt())
                        .updatedAt(addendum.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public String updateAddendum(Long addendumId, AddendumDTO addendumDTO) throws DataNotFoundException {
        // Lấy thông tin người dùng hiện tại
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        // Tìm hợp đồng
        Contract contract = contractRepository.findById(addendumDTO.getContractId())
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy hợp đồng"));

        // Tìm phụ lục
        Addendum addendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phụ lục cho hợp đồng"));

        // Kiểm tra trạng thái phụ lục
        if (addendum.getStatus().equals(AddendumStatus.APPROVAL_PENDING)) {
            throw new RuntimeException("Phụ lục đang trong quy trình duyệt, không được phép cập nhật.");
        }

        // Lưu trạng thái cũ để ghi log
        String oldStatus = addendum.getStatus().name();

        // Biến để theo dõi thay đổi
        boolean isChanged = false;

        // Kiểm tra trùng lặp tiêu đề chỉ khi tiêu đề thay đổi
        if (addendumDTO.getTitle() != null && !addendum.getTitle().equals(addendumDTO.getTitle())) {
            boolean isTitleExist = addendumRepository.existsByContractIdAndTitleAndIdNot(contract.getId(), addendumDTO.getTitle(), addendumId);
            if (isTitleExist) {
                throw new DataNotFoundException("Tên phụ lục bị trùng: " + addendumDTO.getTitle());
            }
            addendum.setTitle(addendumDTO.getTitle());
            isChanged = true;
        }

        // Cập nhật các trường cơ bản nếu có giá trị
        if (addendumDTO.getContent() != null && !addendumDTO.getContent().equals(addendum.getContent())) {
            addendum.setContent(addendumDTO.getContent());
            isChanged = true;
        }
        if (addendumDTO.getContractContent() != null && !addendumDTO.getContractContent().equals(addendum.getContractContent())) {
            addendum.setContractContent(addendumDTO.getContractContent());
            isChanged = true;
        }
        if (addendumDTO.getEffectiveDate() != null && !addendumDTO.getEffectiveDate().equals(addendum.getEffectiveDate())) {
            addendum.setEffectiveDate(addendumDTO.getEffectiveDate());
            isChanged = true;
        }

        if (addendumDTO.getExtendContractDate() != null && !Objects.equals(addendumDTO.getExtendContractDate(), addendum.getExtendContractDate())) {
            addendum.setExtendContractDate(addendumDTO.getExtendContractDate());
            isChanged = true;
        }
        if (addendumDTO.getContractExpirationDate() != null && !Objects.equals(addendumDTO.getContractExpirationDate(), addendum.getContractExpirationDate())) {
            addendum.setContractExpirationDate(addendumDTO.getContractExpirationDate());
            isChanged = true;
        }

        // Cập nhật AddendumItems
        if (addendumDTO.getContractItems() != null && !addendumDTO.getContractItems().isEmpty()) {
            // Xóa toàn bộ phần tử hiện có trong collection
            addendum.getAddendumItems().clear();
            int order = 1;
            for (ContractItemDTO itemDTO : addendumDTO.getContractItems()) {
                if (itemDTO.getDescription() == null || itemDTO.getDescription().trim().isEmpty()) {
                    throw new IllegalArgumentException("Mô tả hạng mục không được để trống.");
                }
                if (itemDTO.getAmount() == null || itemDTO.getAmount() <= 0.0) {
                    throw new IllegalArgumentException("Số tiền hạng mục phải lớn hơn 0.");
                }
                AddendumItem item = AddendumItem.builder()
                        .addendum(addendum)
                        .description(itemDTO.getDescription())
                        .amount(itemDTO.getAmount())
                        .itemOrder(order++)
                        .build();
                addendum.getAddendumItems().add(item);
            }
            isChanged = true;
        } else {
            // Nếu contractItems là null hoặc rỗng, xóa toàn bộ addendumItems
            if (!addendum.getAddendumItems().isEmpty()) {
                addendum.getAddendumItems().clear();
                isChanged = true;
            }
        }

        // Cập nhật AddendumTerms
        boolean hasTerms = (addendumDTO.getLegalBasisTerms() != null && !addendumDTO.getLegalBasisTerms().isEmpty()) ||
                (addendumDTO.getGeneralTerms() != null && !addendumDTO.getGeneralTerms().isEmpty()) ||
                (addendumDTO.getOtherTerms() != null && !addendumDTO.getOtherTerms().isEmpty());

        if (hasTerms) {
            // Xóa toàn bộ phần tử hiện có trong collection
            addendum.getAddendumTerms().clear();

            // Căn cứ pháp lý
            if (addendumDTO.getLegalBasisTerms() != null && !addendumDTO.getLegalBasisTerms().isEmpty()) {
                for (AddendumTermSnapshotDTO termDTO : addendumDTO.getLegalBasisTerms()) {
                    if (termDTO.getId() == null) {
                        throw new IllegalArgumentException("Điều khoản Căn cứ pháp lý không được để trống.");
                    }
                    Term term = termRepository.findById(termDTO.getId())
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy điều khoản"));
                    if (!term.getTypeTerm().getIdentifier().equals(TypeTermIdentifier.LEGAL_BASIS)) {
                        throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại Căn cứ pháp lý.");
                    }
                    AddendumTerm addendumTerm = AddendumTerm.builder()
                            .originalTermId(term.getId())
                            .termLabel(term.getLabel())
                            .termValue(term.getValue())
                            .termType(TypeTermIdentifier.LEGAL_BASIS)
                            .addendum(addendum)
                            .build();
                    addendum.getAddendumTerms().add(addendumTerm);
                }
            }

            // Điều khoản chung
            if (addendumDTO.getGeneralTerms() != null && !addendumDTO.getGeneralTerms().isEmpty()) {
                for (AddendumTermSnapshotDTO termDTO : addendumDTO.getGeneralTerms()) {
                    if (termDTO.getId() == null) {
                        throw new IllegalArgumentException("Điều khoản chung không được để trống.");
                    }
                    Term term = termRepository.findById(termDTO.getId())
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy điều khoản"));
                    if (!term.getTypeTerm().getIdentifier().equals(TypeTermIdentifier.GENERAL_TERMS)) {
                        throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại Điều khoản chung.");
                    }
                    AddendumTerm addendumTerm = AddendumTerm.builder()
                            .originalTermId(term.getId())
                            .termLabel(term.getLabel())
                            .termValue(term.getValue())
                            .termType(TypeTermIdentifier.GENERAL_TERMS)
                            .addendum(addendum)
                            .build();
                    addendum.getAddendumTerms().add(addendumTerm);
                }
            }

            // Điều khoản khác
            if (addendumDTO.getOtherTerms() != null && !addendumDTO.getOtherTerms().isEmpty()) {
                for (AddendumTermSnapshotDTO termDTO : addendumDTO.getOtherTerms()) {
                    if (termDTO.getId() == null) {
                        throw new IllegalArgumentException("Điều khoản khác không được để trống.");
                    }
                    Term term = termRepository.findById(termDTO.getId())
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy điều khoản"));
                    if (!term.getTypeTerm().getIdentifier().equals(TypeTermIdentifier.OTHER_TERMS)) {
                        throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại Điều khoản khác.");
                    }
                    AddendumTerm addendumTerm = AddendumTerm.builder()
                            .originalTermId(term.getId())
                            .termLabel(term.getLabel())
                            .termValue(term.getValue())
                            .termType(TypeTermIdentifier.OTHER_TERMS)
                            .addendum(addendum)
                            .build();
                    addendum.getAddendumTerms().add(addendumTerm);
                }
            }
            isChanged = true;
        } else {
            // Nếu tất cả terms đều null hoặc rỗng, xóa toàn bộ addendumTerms
            if (!addendum.getAddendumTerms().isEmpty()) {
                addendum.getAddendumTerms().clear();
                isChanged = true;
            }
        }

        // Cập nhật AdditionalTermDetails
        if (addendumDTO.getAdditionalConfig() != null && !addendumDTO.getAdditionalConfig().isEmpty()) {
            // Xóa toàn bộ phần tử hiện có
            addendum.getAdditionalTermDetails().clear();
            Map<String, Map<String, List<AddendumTermSnapshotDTO>>> configMap = addendumDTO.getAdditionalConfig();
            for (Map.Entry<String, Map<String, List<AddendumTermSnapshotDTO>>> entry : configMap.entrySet()) {
                String key = entry.getKey();
                Long configTypeTermId;
                try {
                    configTypeTermId = Long.parseLong(key);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Key trong điều khoản bổ sung phải là số đại diện cho của loại điều khoản. Key không hợp lệ" );
                }
                Map<String, List<AddendumTermSnapshotDTO>> groupConfig = entry.getValue();

                // Map nhóm Common
                List<AdditionalTermSnapshot> commonSnapshots = new ArrayList<>();
                if (groupConfig.containsKey("Common") && !groupConfig.get("Common").isEmpty()) {
                    for (AddendumTermSnapshotDTO termDTO : groupConfig.get("Common")) {
                        if (termDTO.getId() == null) {
                            throw new IllegalArgumentException("Điều khoản trong nhóm điều khoản chung không được để trống.");
                        }
                        Term term = termRepository.findById(termDTO.getId())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy điều khoản"));
                        commonSnapshots.add(AdditionalTermSnapshot.builder()
                                .termId(term.getId())
                                .termLabel(term.getLabel())
                                .termValue(term.getValue())
                                .build());
                    }
                }

                // Map nhóm A
                List<AdditionalTermSnapshot> aSnapshots = new ArrayList<>();
                if (groupConfig.containsKey("A") && !groupConfig.get("A").isEmpty()) {
                    for (AddendumTermSnapshotDTO termDTO : groupConfig.get("A")) {
                        if (termDTO.getId() == null) {
                            throw new IllegalArgumentException("Điều khoản trong nhóm bên A không được để trống.");
                        }
                        Term term = termRepository.findById(termDTO.getId())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy điều khoản"));
                        aSnapshots.add(AdditionalTermSnapshot.builder()
                                .termId(term.getId())
                                .termLabel(term.getLabel())
                                .termValue(term.getValue())
                                .build());
                    }
                }

                // Map nhóm B
                List<AdditionalTermSnapshot> bSnapshots = new ArrayList<>();
                if (groupConfig.containsKey("B") && !groupConfig.get("B").isEmpty()) {
                    for (AddendumTermSnapshotDTO termDTO : groupConfig.get("B")) {
                        if (termDTO.getId() == null) {
                            throw new IllegalArgumentException("Điều khoản trong nhóm bên B không được để trống.");
                        }
                        Term term = termRepository.findById(termDTO.getId())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy điều khoản"));
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
                    throw new IllegalArgumentException("Các điều khoản không được chọn đồng thời ở nhóm 'Bên Chung' và 'Bên A'.");
                }
                Set<Long> unionCommonB = new HashSet<>(commonSnapshots.stream().map(AdditionalTermSnapshot::getTermId).toList());
                unionCommonB.retainAll(bSnapshots.stream().map(AdditionalTermSnapshot::getTermId).toList());
                if (!unionCommonB.isEmpty()) {
                    throw new IllegalArgumentException("Các điều khoản không được chọn đồng thời ở nhóm 'Bên Chung' và 'Bên B'.");
                }
                Set<Long> unionAB = new HashSet<>(aSnapshots.stream().map(AdditionalTermSnapshot::getTermId).toList());
                unionAB.retainAll(bSnapshots.stream().map(AdditionalTermSnapshot::getTermId).toList());
                if (!unionAB.isEmpty()) {
                    throw new IllegalArgumentException("Các điều khoản không được chọn đồng thời ở nhóm 'Bên A' và 'Bên B'.");
                }

                // Kiểm tra type term
                for (AdditionalTermSnapshot snap : commonSnapshots) {
                    Term term = termRepository.findById(snap.getTermId())
                            .orElseThrow(() -> new IllegalArgumentException("Không tồn tại điều khoản"));
                    if (!term.getTypeTerm().getId().equals(configTypeTermId)) {
                        throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại điều khoản: \"" + term.getTypeTerm().getName() + "\".");
                    }
                }
                for (AdditionalTermSnapshot snap : aSnapshots) {
                    Term term = termRepository.findById(snap.getTermId())
                            .orElseThrow(() -> new IllegalArgumentException("Không tồn tại điều khoản"));
                    if (!term.getTypeTerm().getId().equals(configTypeTermId)) {
                        throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại điều khoản: \"" + term.getTypeTerm().getName() + "\".");
                    }
                }
                for (AdditionalTermSnapshot snap : bSnapshots) {
                    Term term = termRepository.findById(snap.getTermId())
                            .orElseThrow(() -> new IllegalArgumentException("Không tồn tại điều khoản"));
                    if (!term.getTypeTerm().getId().equals(configTypeTermId)) {
                        throw new IllegalArgumentException("Điều khoản \"" + term.getLabel() + "\" không thuộc loại điều khoản: \"" + term.getTypeTerm().getName() + "\".");
                    }
                }

                AddendumAdditionalTermDetail configDetail = AddendumAdditionalTermDetail.builder()
                        .typeTermId(configTypeTermId)
                        .commonTerms(commonSnapshots)
                        .aTerms(aSnapshots)
                        .bTerms(bSnapshots)
                        .addendum(addendum)
                        .build();
                addendum.getAdditionalTermDetails().add(configDetail);
            }
            isChanged = true;
        } else {
            // Nếu additionalConfig là null hoặc rỗng, xóa toàn bộ additionalTermDetails
            if (!addendum.getAdditionalTermDetails().isEmpty()) {
                addendum.getAdditionalTermDetails().clear();
                isChanged = true;
            }
        }

        // Cập nhật PaymentSchedules
        if (addendumDTO.getPayments() != null && !addendumDTO.getPayments().isEmpty()) {
            // Xóa toàn bộ phần tử hiện có
            addendum.getPaymentSchedules().clear();
            int order = 1;
            for (PaymentDTO paymentDTO : addendumDTO.getPayments()) {
                if (paymentDTO.getAmount() == null || paymentDTO.getAmount() <= 0.0) {
                    throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn 0.");
                }
                if (paymentDTO.getPaymentDate() == null) {
                    throw new IllegalArgumentException("Ngày thanh toán không được để trống.");
                }
                AddendumPaymentSchedule paymentSchedule = AddendumPaymentSchedule.builder()
                        .amount(paymentDTO.getAmount())
                        .paymentDate(paymentDTO.getPaymentDate())
                        .notifyPaymentDate(paymentDTO.getNotifyPaymentDate())
                        .paymentOrder(order++)
                        .status(PaymentStatus.UNPAID)
                        .paymentMethod(paymentDTO.getPaymentMethod())
//                        .notifyPaymentContent(paymentDTO.getNotifyPaymentContent())
                        .reminderEmailSent(false)
                        .overdueEmailSent(false)
                        .addendum(addendum)
                        .build();
                addendum.getPaymentSchedules().add(paymentSchedule);
            }
            isChanged = true;
        } else {
            // Nếu payments là null hoặc rỗng, xóa toàn bộ paymentSchedules
            if (!addendum.getPaymentSchedules().isEmpty()) {
                addendum.getPaymentSchedules().clear();
                isChanged = true;
            }
        }

        // Chỉ lưu nếu có thay đổi
        if (isChanged) {
            addendum.setStatus(AddendumStatus.UPDATED);
            addendum.setUpdatedAt(LocalDateTime.now());
            Addendum updatedAddendum = addendumRepository.save(addendum);

            // Ghi log audit
            logAuditTrailForAddendum(updatedAddendum, "UPDATE", "status", oldStatus, AddendumStatus.UPDATED.name(), currentUser.getUsername());
            return "Addendum updated successfully.";
        } else {
            return "No changes detected.";
        }
    }

    @Override
    @Transactional
    public void deleteAddendum(Long addendumId) throws DataNotFoundException {
        // Tìm phụ lục theo id
        Addendum addendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phụ lục"));
        // Xóa phụ lục
        addendumRepository.delete(addendum);
    }

    @Override
    @Transactional
    public Optional<AddendumResponse> getAddendumById(Long addendumId) throws DataNotFoundException {
        return addendumRepository.findById(addendumId)
                .map(addendum -> {
                    // Force lazy loading của các collection khi session còn mở.
                    addendum.getAddendumTerms().size();
                    addendum.getAddendumItems().size();
                    addendum.getAdditionalTermDetails().forEach(detail -> {
                        detail.getCommonTerms().size();
                        detail.getATerms().size();
                        detail.getBTerms().size();
                    });
                    try {
                        return convertAddendumToResponse(addendum);
                    } catch (DataNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private AddendumResponse convertAddendumToResponse(Addendum addendum) throws DataNotFoundException {
        // Map các ContractTerm thành ContractTermResponse
        List<TermResponse> legalBasisTerms = addendum.getAddendumTerms().stream()
                .filter(term -> term.getTermType() == TypeTermIdentifier.LEGAL_BASIS)
                .map(term -> TermResponse.builder()
                        .id(term.getOriginalTermId())
                        .label(term.getTermLabel())
                        .value(term.getTermValue())
                        .build())
                .collect(Collectors.toList());

        List<TermResponse> generalTerms = addendum.getAddendumTerms().stream()
                .filter(term -> term.getTermType() == TypeTermIdentifier.GENERAL_TERMS)
                .map(term -> TermResponse.builder()
                        .id(term.getOriginalTermId())
                        .label(term.getTermLabel())
                        .value(term.getTermValue())
                        .build())
                .collect(Collectors.toList());

        List<TermResponse> otherTerms = addendum.getAddendumTerms().stream()
                .filter(term -> term.getTermType() == TypeTermIdentifier.OTHER_TERMS)
                .map(term -> TermResponse.builder()
                        .id(term.getOriginalTermId())
                        .label(term.getTermLabel())
                        .value(term.getTermValue())
                        .build())
                .collect(Collectors.toList());


        List<TypeTermResponse> additionalTerms = addendum.getAdditionalTermDetails().stream()
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
        Map<String, Map<String, List<TermResponse>>> additionalConfig = addendum.getAdditionalTermDetails()
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
        Partner partnerA = partnerRepository.findById(1L)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy bên A"));
        Optional<ContractPartner> contractPartners = contractPartnerRepository.findByContractIdAndPartnerType(addendum.getContract().getId(), PartnerType.PARTNER_B);


        return AddendumResponse.builder()
                .addendumId(addendum.getId())
                .title(addendum.getTitle())
                .content(addendum.getContent())
                .contractNumber(addendum.getContractNumber())
                .status(addendum.getStatus())
                .contractId(addendum.getContract().getId())
                .createdBy(UserAddendumResponse.builder()
                        .userId(addendum.getUser().getId())
                        .userName(addendum.getUser().getFullName())
                        .build())
                .partnerA(partnerA)
                .partnerB(contractPartners)
                .legalBasisTerms(legalBasisTerms)
                .generalTerms(generalTerms)
                .otherTerms(otherTerms)
                .paymentSchedules(convertPaymentSchedules(addendum.getPaymentSchedules()))
                .additionalTerms(additionalTerms)
                .additionalConfig(additionalConfig)
                .contractContent(addendum.getContractContent())
                .contractItems(convertContractItems(addendum.getAddendumItems()))
                .extendContractDate(addendum.getExtendContractDate())
                .contractExpirationDate(addendum.getContractExpirationDate())
                .build();
    }

    private List<ContractItemDTO> convertContractItems(List<AddendumItem> addendumItems) {
        if (addendumItems == null || addendumItems.isEmpty()) {
            return Collections.emptyList();
        }
        return addendumItems.stream()
                .map(item -> ContractItemDTO.builder()
                        .id(item.getId())
                        .description(item.getDescription())
                        .itemOrder(item.getItemOrder())
                        .amount(item.getAmount())
                        .build())
                .collect(Collectors.toList());
    }


    private List<TermResponse> convertAdditionalTermSnapshotsToTermResponseList(List<AdditionalTermSnapshot> snapshots) {
        return snapshots.stream()
                .map(snap -> TermResponse.builder()
                        .id(snap.getTermId())
                        .label(snap.getTermLabel())
                        .value(snap.getTermValue())
                        .build())
                .collect(Collectors.toList());
    }
    private List<PaymentScheduleResponse> convertPaymentSchedules(List<AddendumPaymentSchedule> paymentSchedules) {
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
//                        .notifyPaymentContent(schedule.getNotifyPaymentContent())
                        .reminderEmailSent(schedule.isReminderEmailSent())
                        .overdueEmailSent(schedule.isOverdueEmailSent())
                        .build())
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public void assignApprovalWorkflowOfContractToAddendum(Long addendumId) throws DataNotFoundException {
        // Lấy phụ lục hợp đồng và hợp đồng gốc
        Addendum addendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Phụ lục hợp đồng không tìm thấy"));

        Contract contract = contractRepository.findById(addendum.getContract().getId())
                .orElseThrow(() -> new DataNotFoundException("Hợp đồng không tìm thấy"));

        ApprovalWorkflow contractApprovalWorkflow = contract.getApprovalWorkflow();
        if (contractApprovalWorkflow == null) {
            throw new DataNotFoundException("Không tìm thấy quy trình phê duyệt hợp đồng");
        }

        // Tạo một quy trình duyệt cho phụ lục hợp đồng bằng cách sao chép thông tin từ hợp đồng
        ApprovalWorkflow addendumApprovalWorkflow = new ApprovalWorkflow();
        addendumApprovalWorkflow.setName(contractApprovalWorkflow.getName());
        addendumApprovalWorkflow.setCreatedAt(LocalDateTime.now());
        addendumApprovalWorkflow.setContractType(contractApprovalWorkflow.getContractType());

        // Sao chép các bước duyệt từ quy trình duyệt của hợp đồng vào quy trình của phụ lục hợp đồng
        for (ApprovalStage stage : contractApprovalWorkflow.getStages()) {
            ApprovalStage addendumStage = new ApprovalStage();
            addendumStage.setStageOrder(stage.getStageOrder());
            addendumStage.setApprover(stage.getApprover());
            addendumStage.setStatus(ApprovalStatus.NOT_STARTED);
            addendumStage.setApprovalWorkflow(addendumApprovalWorkflow);
            addendumApprovalWorkflow.getStages().add(addendumStage);
        }

        approvalWorkflowRepository.save(addendumApprovalWorkflow);
        addendum.setApprovalWorkflow(addendumApprovalWorkflow);
        addendum.setStatus(AddendumStatus.APPROVAL_PENDING);
        addendumRepository.save(addendum);

        addendumApprovalWorkflow.getStages().stream()
                .min(Comparator.comparingInt(ApprovalStage::getStageOrder))
                .ifPresent(firstStage -> {
                    Map<String, Object> payload = new HashMap<>();
                    String notificationMessage = "Bạn có phụ lục hợp đồng cần phê duyệt đợt "
                            + firstStage.getStageOrder() + ": Phụ lục " + addendum.getTitle() + " của hợp đồng số " + addendum.getContract().getContractNumber();
                    payload.put("message", notificationMessage);
                    payload.put("addendumId", addendumId);
                    User firstApprover = firstStage.getApprover();
                    mailService.sendEmailAddendumReminder(addendum, firstApprover, firstStage);
                    notificationService.saveNotification(firstApprover, notificationMessage, addendum.getContract());
                    messagingTemplate.convertAndSendToUser(firstApprover.getFullName(), "/queue/notifications", payload);

                    // Đặt trạng thái duyệt cho bước này (nếu cần)
                    firstStage.setStatus(ApprovalStatus.APPROVING);
                    approvalStageRepository.save(firstStage);
                });
    }

    @Override
    @Transactional
    public void assignWorkflowToAddendum(Long addendumId, Long workflowId) throws DataNotFoundException {
        // Tìm phụ lục cần gán workflow
        Addendum addendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Phụ lục hợp đồng không tìm thấy"));

        // Tìm workflow gốc theo workflowId
        ApprovalWorkflow originalWorkflow = approvalWorkflowRepository.findById(workflowId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.WORKFLOW_NOT_FOUND));

        ApprovalWorkflow workflowToAssign = originalWorkflow;

        // Nếu workflow đã được gán cho một phụ lục khác, thực hiện clone
        if (originalWorkflow.getContract() != null || originalWorkflow.getAddendum() != null) {
            // Clone thông tin cơ bản của workflow
            ApprovalWorkflow clonedWorkflow = ApprovalWorkflow.builder()
                    .name(originalWorkflow.getName())
                    .customStagesCount(originalWorkflow.getCustomStagesCount())
                    .createdAt(LocalDateTime.now())
                    .contractType(originalWorkflow.getContractType())  // Optional, nếu muốn
                    .build();

            // Clone các bước duyệt (stages) và đặt trạng thái về PENDING
            originalWorkflow.getStages().forEach(stage -> {
                ApprovalStage clonedStage = ApprovalStage.builder()
                        .stageOrder(stage.getStageOrder())
                        .approver(stage.getApprover())
                        .status(ApprovalStatus.NOT_STARTED)
                        .approvalWorkflow(clonedWorkflow)
                        .build();
                clonedWorkflow.getStages().add(clonedStage);
            });

            // Lưu workflow mới (với các stage tương ứng)
            approvalWorkflowRepository.save(clonedWorkflow);
            workflowToAssign = clonedWorkflow;
        }
        String oldStatus = addendum.getStatus().name();
        // Gán workflow (mới hoặc gốc nếu chưa gán) cho phụ lục
        addendum.setApprovalWorkflow(workflowToAssign);
        addendum.setStatus(AddendumStatus.APPROVAL_PENDING);
        addendumRepository.save(addendum);

        // Lưu lại lịch sử thay đổi (AuditTrail)
        String changedBy = SecurityContextHolder.getContext().getAuthentication().getName();
        logAuditTrailForAddendum(addendum, "UPDATE", "status", oldStatus, AddendumStatus.APPROVAL_PENDING.name(), changedBy);
        // Lấy stage có stageOrder nhỏ nhất để gửi thông báo
        workflowToAssign.getStages().stream()
                .min(Comparator.comparingInt(ApprovalStage::getStageOrder))
                .ifPresent(firstStage -> {
                    Map<String, Object> payload = new HashMap<>();
                    String notificationMessage = "Bạn có phụ lục hợp đồng cần phê duyệt đợt "
                            + firstStage.getStageOrder() + ": Phụ lục " + addendum.getTitle() + " của hợp đồng số " + addendum.getContract().getContractNumber();
                    payload.put("message", notificationMessage);
                    payload.put("addendumId", addendumId);
                    User firstApprover = firstStage.getApprover();
                    mailService.sendEmailAddendumReminder(addendum, firstApprover, firstStage);
                    notificationService.saveNotification(firstApprover, notificationMessage, addendum.getContract());
                    messagingTemplate.convertAndSendToUser(firstApprover.getFullName(), "/queue/notifications", payload);

                    // Đặt trạng thái duyệt cho bước này (nếu cần)
                    firstStage.setStatus(ApprovalStatus.APPROVING);
                    approvalStageRepository.save(firstStage);
                });
    }

    @Override
    @Transactional
    public void approvedStageForAddendum(Long addendumId, Long stageId) throws DataNotFoundException {
        // Lấy phụ lục theo addendumId
        Addendum addendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phụ lục"));

        // Tìm ApprovalStage theo stageId
        ApprovalStage stage = approvalStageRepository.findById(stageId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy giai đoạn phê duyệt"));

        // Lấy người dùng hiện tại từ SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        // Kiểm tra: chỉ cho phép người được giao duyệt thao tác
        if (!stage.getApprover().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền duyệt bước này.");
        }

        // Kiểm tra: nếu người duyệt đã xử lý (approve hoặc reject) ở bất kỳ bước nào của phụ lục, từ chối thao tác tiếp
        boolean alreadyProcessed = addendum.getApprovalWorkflow().getStages().stream()
                .filter(s -> s.getApprover().getId().equals(currentUser.getId()))
                .anyMatch(s -> s.getStatus() == ApprovalStatus.APPROVED || s.getStatus() == ApprovalStatus.REJECTED);
        if (alreadyProcessed) {
            throw new RuntimeException("Bạn đã xử lý phụ lục này rồi.");
        }

        // Kiểm tra nếu bước đã được xử lý rồi thì không cho duyệt lại
        if (stage.getStatus() == ApprovalStatus.APPROVED || stage.getStatus() == ApprovalStatus.REJECTED) {
            throw new RuntimeException("Bước này đã được xử lý.");
        }

        // Cập nhật trạng thái duyệt cho bước hiện tại
        stage.setStatus(ApprovalStatus.APPROVED);
        stage.setApprovedAt(LocalDateTime.now());
        approvalStageRepository.save(stage);

        // Nếu duyệt thành công, chuyển sang bước tiếp theo (nếu có)
        if (stage.getStatus() == ApprovalStatus.APPROVED) {
            ApprovalWorkflow workflow = stage.getApprovalWorkflow();
            Optional<ApprovalStage> nextStageOptional = workflow.getStages().stream()
                    .filter(s -> s.getStageOrder() > stage.getStageOrder())
                    .min(Comparator.comparingInt(ApprovalStage::getStageOrder));

            if (nextStageOptional.isPresent()) {
                ApprovalStage nextStage = nextStageOptional.get();
                nextStage.setStatus(ApprovalStatus.APPROVING);
                approvalStageRepository.save(nextStage);
                User nextApprover = nextStage.getApprover();

                // Tạo payload thông báo
                Map<String, Object> payload = new HashMap<>();
                String notificationMessage = "Bạn có phụ lục hợp đồng cần phê duyệt đợt " + nextStage.getStageOrder() +
                        ": Phụ lục " + addendum.getTitle();
                payload.put("message", notificationMessage);
                payload.put("addendumId", addendumId);

                // Gửi thông báo cho người duyệt tiếp theo
                mailService.sendEmailAddendumReminder(addendum, nextApprover, nextStage);
                notificationService.saveNotification(nextApprover, notificationMessage, addendum.getContract());
                messagingTemplate.convertAndSendToUser(nextApprover.getFullName(), "/queue/notifications", payload);
            } else {
                String oldStatus = addendum.getStatus().name();
                // Nếu không còn bước tiếp theo, cập nhật trạng thái phụ lục thành APPROVED
                addendum.setStatus(AddendumStatus.APPROVED);
                addendumRepository.save(addendum);
                User finalApprover = stage.getApprover();

                String changedBy = currentUser.getUsername();
                logAuditTrailForAddendum(addendum, "UPDATE", "status", oldStatus, AddendumStatus.APPROVED.name(), changedBy);
                Map<String, Object> payload = new HashMap<>();
                String notificationMessage = "Phụ lục: " + addendum.getTitle() + " của hợp đồng số " + addendum.getContractNumber() + " đã duyệt xong.";
                payload.put("message", notificationMessage);
                payload.put("addendumId", addendumId);

                // Gửi thông báo cho người duyệt tiếp theo
                mailService.sendEmailApprovalSuccessForAddendum(addendum, finalApprover);
                notificationService.saveNotification(finalApprover, notificationMessage, addendum.getContract());
                messagingTemplate.convertAndSendToUser(finalApprover.getFullName(), "/queue/notifications", payload);
            }
        }
    }

    @Override
    @Transactional
    public void rejectStageForAddendum(Long addendumId, Long stageId, WorkflowDTO workflowDTO) throws DataNotFoundException {
        // Lấy phụ lục theo addendumId
        Addendum addendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phụ lục"));

        // Tìm ApprovalStage theo stageId
        ApprovalStage stage = approvalStageRepository.findById(stageId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy giai đoạn phê duyệt"));

        // Lấy người dùng hiện tại từ SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        // Kiểm tra: chỉ cho phép người được giao duyệt thao tác
        if (!stage.getApprover().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền từ chối bước này.");
        }

        // Kiểm tra: nếu người duyệt đã xử lý (approve hoặc reject) ở bất kỳ bước nào của phụ lục, từ chối thao tác tiếp
        boolean alreadyProcessed = addendum.getApprovalWorkflow().getStages().stream()
                .filter(s -> s.getApprover().getId().equals(currentUser.getId()))
                .anyMatch(s -> s.getStatus() == ApprovalStatus.APPROVED || s.getStatus() == ApprovalStatus.REJECTED);
        if (alreadyProcessed) {
            throw new RuntimeException("Bạn đã xử lý phụ lục này rồi.");
        }

        // Kiểm tra nếu bước đã được xử lý rồi thì không cho thao tác lại
        if (stage.getStatus() == ApprovalStatus.APPROVED || stage.getStatus() == ApprovalStatus.REJECTED) {
            throw new RuntimeException("Bước này đã được xử lý.");
        }

        // Cập nhật trạng thái bước là REJECTED, lưu comment và thời gian xử lý
        stage.setStatus(ApprovalStatus.REJECTED);
        stage.setApprovedAt(LocalDateTime.now());
        stage.setComment(workflowDTO.getComment());
        approvalStageRepository.save(stage);

        String oldStatus = addendum.getStatus().name();
        // Cập nhật trạng thái phụ lục thành REJECTED
        addendum.setStatus(AddendumStatus.REJECTED);
        addendumRepository.save(addendum);

        // Ghi audit trail

        String changedBy = currentUser.getUsername();
        logAuditTrailForAddendum(addendum, "REJECT", "status", oldStatus, AddendumStatus.REJECTED.name(), changedBy);
        // Gửi thông báo cho người tạo phụ lục để yêu cầu chỉnh sửa
        Map<String, Object> payload = new HashMap<>();
        String notificationMessage = "Bạn có phụ lục " + addendum.getTitle() + " của hợp đồng số " + addendum.getContractNumber() + " đã bị từ chối phê duyệt. Vui lòng kiểm tra lại.";
        payload.put("message", notificationMessage);
        payload.put("addendumId", addendumId);
        mailService.sendUpdateAddendumReminder(addendum, addendum.getContract().getUser());
        notificationService.saveNotification(addendum.getContract().getUser(), notificationMessage, addendum.getContract());
        messagingTemplate.convertAndSendToUser(addendum.getContract().getUser().getFullName(), "/queue/notifications", payload);
    }

    @Override
    @Transactional
    public void resubmitAddendumForApproval(Long addendumId) throws DataNotFoundException {
        // Tìm phụ lục theo addendumId
        Addendum addendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phụ lục với id : " + addendumId));

        // Lấy workflow của phụ lục
        ApprovalWorkflow workflow = addendum.getApprovalWorkflow();
        if (workflow == null || workflow.getStages().isEmpty()) {
            throw new DataNotFoundException("Không tìm thấy quy trình phê duyệt cho phụ lục với id : " + addendumId);
        }

        // Reset lại tất cả các bước duyệt: đặt trạng thái về PENDING, xóa approvedAt và comment
        workflow.getStages().forEach(stage -> {
            stage.setStatus(ApprovalStatus.NOT_STARTED);
            stage.setApprovedAt(null);
            stage.setComment(null);
            approvalStageRepository.save(stage);
        });
        String oldStatus = addendum.getStatus().name();

        // Cập nhật lại trạng thái của phụ lục về APPROVAL_PENDING (để báo hiệu đang chờ duyệt lại)
        addendum.setStatus(AddendumStatus.APPROVAL_PENDING);
        addendumRepository.save(addendum);

        String changedBy = SecurityContextHolder.getContext().getAuthentication().getName();
        logAuditTrailForAddendum(addendum, "RESUBMIT", "status", oldStatus, AddendumStatus.APPROVAL_PENDING.name(), changedBy);
        // Tìm bước duyệt đầu tiên (stage có stageOrder nhỏ nhất)
        Optional<ApprovalStage> firstStageOpt = workflow.getStages().stream()
                .min(Comparator.comparingInt(ApprovalStage::getStageOrder));

        if (firstStageOpt.isPresent()) {
            ApprovalStage firstStage = firstStageOpt.get();
            // Đặt trạng thái duyệt cho bước đầu tiên
            firstStage.setStatus(ApprovalStatus.APPROVING);
            approvalStageRepository.save(firstStage);
            User firstApprover = firstStage.getApprover();

            // Tạo payload thông báo cho người duyệt ở bước đầu tiên
            Map<String, Object> payload = new HashMap<>();
            String notificationMessage = "Phụ lục hợp đồng '" + addendum.getTitle() + "' đã được chỉnh sửa và nộp lại để phê duyệt. Bạn có phụ lục cần phê duyệt đợt "
                    + firstStage.getStageOrder();
            payload.put("message", notificationMessage);
            payload.put("addendumId", addendumId);

            // Gửi email nhắc nhở nếu cần
            mailService.sendEmailAddendumReminder(addendum, firstApprover, firstStage);
            // Lưu thông báo vào hệ thống thông báo
            notificationService.saveNotification(firstApprover, notificationMessage, addendum.getContract());
            // Gửi thông báo qua WebSocket
            messagingTemplate.convertAndSendToUser(firstApprover.getFullName(), "/queue/notifications", payload);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AddendumResponse> getAddendaByUserWithFilters(
            Long userId,
            String keyword,
            List<AddendumStatus> statuses,
            int page,
            int size,
            User currentUser) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        boolean hasSearch = keyword != null && !keyword.trim().isEmpty();
        boolean hasStatusFilter = statuses != null && !statuses.isEmpty();

        Page<Addendum> addenda;

        String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "";
        boolean isDirector = "DIRECTOR".equalsIgnoreCase(roleName);
        boolean isStaff = "STAFF".equalsIgnoreCase(roleName);

        if (isStaff) {
            if (hasStatusFilter) {
                if (hasSearch) {
                    addenda = addendumRepository.findByContractUserIdAndKeywordAndStatusIn(
                            userId, keyword.trim(), statuses, pageable);
                } else {
                    addenda = addendumRepository.findByContractUserIdAndStatusIn(
                            userId, statuses, pageable);
                }
            } else {
                if (hasSearch) {
                    addenda = addendumRepository.findByContractUserIdAndKeyword(
                            userId, keyword.trim(), pageable);
                } else {
                    addenda = addendumRepository.findByContractUserId(userId, pageable);
                }
            }
        } else if (isDirector) {
            if (hasStatusFilter) {
                if (hasSearch) {
                    addenda = addendumRepository.findByKeywordAndStatusIn(
                            keyword.trim(), statuses, pageable);
                } else {
                    addenda = addendumRepository.findByStatusIn(statuses, pageable);
                }
            } else {
                if (hasSearch) {
                    addenda = addendumRepository.findByKeyword(keyword.trim(), pageable);
                } else {
                    addenda = addendumRepository.findAll(pageable);
                }
            }
        } else {
            // Không có quyền
            addenda = Page.empty(pageable);
        }

        return addenda.map(this::mapToAddendumResponse);
    }

    @Override
    @Transactional
    public Page<AddendumResponse> getAddendaForManager(Long approverId, String keyword, int page, int size) {
        // Cấu hình phân trang
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Lấy tất cả các phụ lục
        List<Addendum> pendingAddenda = addendumRepository.findAll();

        // Lọc các phụ lục theo approverId, keyword và trạng thái duyệt
        List<Addendum> filteredAddenda = pendingAddenda.stream()
                .filter(addendum -> {
                    ApprovalWorkflow workflow = addendum.getApprovalWorkflow();
                    if (workflow == null || workflow.getStages().isEmpty()) return false;

                    // Kiểm tra nếu phụ lục có bất kỳ bước nào đã được duyệt qua
                    boolean approverHasCompletedStep = workflow.getStages().stream()
                            .anyMatch(stage -> stage.getApprover().getId().equals(approverId) &&
                                    stage.getStatus() == ApprovalStatus.APPROVED);

                    // Nếu người duyệt đã duyệt xong ít nhất 1 bước, không cho hiển thị phụ lục đó nữa
                    if (approverHasCompletedStep) {
                        return false;
                    }

                    // Nếu phụ lục đang trong quá trình duyệt
                    if (addendum.getStatus() == AddendumStatus.APPROVAL_PENDING) {
                        // Tìm bước duyệt hiện tại (có trạng thái NOT_STARTED, REJECTED, hoặc APPROVING)
                        OptionalInt currentStageOrderOpt = workflow.getStages().stream()
                                .filter(stage -> stage.getStatus() == ApprovalStatus.NOT_STARTED
                                        || stage.getStatus() == ApprovalStatus.REJECTED
                                        || stage.getStatus() == ApprovalStatus.APPROVING)
                                .mapToInt(ApprovalStage::getStageOrder)
                                .min();

                        if (currentStageOrderOpt.isEmpty()) return false;

                        int currentStageOrder = currentStageOrderOpt.getAsInt();

                        // Kiểm tra xem approver có quyền duyệt bước này không
                        return workflow.getStages().stream()
                                .anyMatch(stage -> stage.getStageOrder() <= currentStageOrder
                                        && stage.getApprover().getId().equals(approverId));
                    }

                    // Nếu không, không cho hiển thị phụ lục (đã duyệt hoặc đã bị từ chối)
                    return false;
                })
                .filter(addendum -> {
                    // Lọc theo từ khóa (nếu có)
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String lowerKeyword = keyword.toLowerCase();
                        return addendum.getTitle().toLowerCase().contains(lowerKeyword)
                                || addendum.getContent().toLowerCase().contains(lowerKeyword);
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // Áp dụng phân trang
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredAddenda.size());
        List<AddendumResponse> content = filteredAddenda.subList(start, end).stream()
                .map(this::mapToAddendumResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, filteredAddenda.size());
    }

    @Override
    @Transactional
    public Page<AddendumResponse> getAddendaForApprover(Long approverId, String keyword, int page, int size) {
        // Cấu hình phân trang
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Lấy toàn bộ phụ lục (không giới hạn status)
        List<Addendum> allAddenda = addendumRepository.findAll();

        List<Addendum> filteredAddenda = allAddenda.stream()
                .filter(addendum -> {
                    ApprovalWorkflow workflow = addendum.getApprovalWorkflow();
                    if (workflow == null || workflow.getStages().isEmpty()) return false;

                    // 1. Nếu người này đã từng duyệt qua bất kỳ bước nào → luôn thấy
                    boolean hasApprovedBefore = workflow.getStages().stream()
                            .anyMatch(stage -> stage.getApprover().getId().equals(approverId)
                                    && stage.getStatus() == ApprovalStatus.APPROVED);

                    if (hasApprovedBefore) return true;

                    // 2. Nếu chưa duyệt, kiểm tra xem có đang ở bước hiện tại không
                    OptionalInt currentStageOrderOpt = workflow.getStages().stream()
                            .filter(stage -> stage.getStatus() == ApprovalStatus.NOT_STARTED
                                    || stage.getStatus() == ApprovalStatus.REJECTED
                                    || stage.getStatus() == ApprovalStatus.APPROVING)
                            .mapToInt(ApprovalStage::getStageOrder)
                            .min();

                    if (currentStageOrderOpt.isEmpty()) return false;

                    int currentStageOrder = currentStageOrderOpt.getAsInt();

                    return workflow.getStages().stream()
                            .anyMatch(stage -> stage.getStageOrder() <= currentStageOrder
                                    && stage.getApprover().getId().equals(approverId));
                })
                .filter(addendum -> {
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String lowerKeyword = keyword.toLowerCase();
                        return addendum.getTitle().toLowerCase().contains(lowerKeyword)
                                || addendum.getContent().toLowerCase().contains(lowerKeyword);
                    }
                    return true;
                })

                .collect(Collectors.toList());

        // Phân trang
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredAddenda.size());
        List<AddendumResponse> content = filteredAddenda.subList(start, end).stream()
                .map(this::mapToAddendumResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, filteredAddenda.size());
    }

    @Override
    @Transactional
    public ApprovalWorkflowResponse getWorkflowByAddendumId(Long addendumId) throws DataNotFoundException {
        Addendum addendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Phụ lục không tìm thấy"));

        ApprovalWorkflow workflow = addendum.getApprovalWorkflow();
        if (workflow == null) {
            // Trả về một workflow rỗng
            return ApprovalWorkflowResponse.builder()
                    .id(null)
                    .name("")
                    .customStagesCount(0)
                    .createdAt(null)
                    .stages(List.of()) // danh sách rỗng
                    .build();
        }

        return ApprovalWorkflowResponse.builder()
                .id(workflow.getId())
                .name(workflow.getName())
                .customStagesCount(workflow.getCustomStagesCount())
                .createdAt(workflow.getCreatedAt())
                .stages(
                        workflow.getStages() != null ? workflow.getStages().stream()
                                .map(stage -> ApprovalStageResponse.builder()
                                        .stageId(stage.getId())
                                        .stageOrder(stage.getStageOrder())
                                        .approver(stage.getApprover().getId())
                                        .approverName(stage.getApprover().getFullName())
                                        .department(stage.getApprover().getDepartment())
                                        .startDate(stage.getStartDate())
                                        .endDate(stage.getDueDate())
                                        .approvedAt(stage.getApprovedAt())
                                        .status(stage.getStatus())
                                        .comment(stage.getComment())
                                        .build())
                                .toList() : List.of()
                )
                .build();
    }

    @Override
    @Transactional
    public ApprovalWorkflowResponse createWorkflowForAddendum(AddendumApprovalWorkflowDTO approvalWorkflowDTO) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        ApprovalWorkflow workflow = ApprovalWorkflow.builder()
                .name(approvalWorkflowDTO.getName())
                .user(currentUser)
                .createdAt(LocalDateTime.now())
                .build();

        // Nếu có stages, kiểm tra xem approver của mỗi stage có bị trùng không
        if (approvalWorkflowDTO.getStages() != null) {
            Set<Long> approverIds = new HashSet<>();
            for (var stageDTO : approvalWorkflowDTO.getStages()) {
                if (!approverIds.add(stageDTO.getApproverId())) {
                    throw new RuntimeException("Trùng người duyệt tại stage: " + stageDTO.getApproverId());
                }
            }

            // Tạo và thêm các stage sau khi xác nhận không có duplicate
            approvalWorkflowDTO.getStages().forEach(stageDTO -> {
                User approver = userRepository.findById(stageDTO.getApproverId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy người duyệt  " + stageDTO.getApproverId()));
                ApprovalStage stage = ApprovalStage.builder()
                        .stageOrder(stageDTO.getStageOrder())
                        .approver(approver)
                        .status(ApprovalStatus.NOT_STARTED)
                        .approvalWorkflow(workflow)
                        .build();
                workflow.getStages().add(stage);
            });

            // ✅ Kiểm tra người duyệt cuối cùng có phải là DIRECTOR chưa
            User lastApprover = workflow.getStages().get(workflow.getStages().size() - 1).getApprover();
            boolean isDirector = lastApprover.getRole() != null &&
                    Role.DIRECTOR.equalsIgnoreCase(lastApprover.getRole().getRoleName());

            if (!isDirector) {
                User director = userRepository.findAll().stream()
                        .filter(user -> user.getRole() != null && Role.DIRECTOR.equalsIgnoreCase(user.getRole().getRoleName()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy người duyệt có vai trò giám đốc"));

                ApprovalStage directorStage = ApprovalStage.builder()
                        .stageOrder(workflow.getStages().size() + 1)
                        .approver(director)
                        .status(ApprovalStatus.NOT_STARTED)
                        .approvalWorkflow(workflow)
                        .build();

                workflow.getStages().add(directorStage);
            }
        }

        // Cập nhật số lượng stage tùy chỉnh dựa trên số stage đã thêm
        workflow.setCustomStagesCount(workflow.getStages().size());
        // Lưu workflow
        approvalWorkflowRepository.save(workflow);


//        if (approvalWorkflowDTO.getAddendumTypeId() != null) {
//            AddendumType addendumType = addendumTypeRepository.findById(approvalWorkflowDTO.getAddendumTypeId())
//                    .orElseThrow(() -> new RuntimeException("Không tìm thấy loại phụ lục với id " + approvalWorkflowDTO.getAddendumTypeId()));
//            workflow.setAddendumType(addendumType);
//            approvalWorkflowRepository.save(workflow);
//        }

        // Trả về response với các thông tin cần thiết
        return ApprovalWorkflowResponse.builder()
                .id(workflow.getId())
                .name(workflow.getName())
                .customStagesCount(workflow.getCustomStagesCount())
                .createdAt(workflow.getCreatedAt())
                .stages(workflow.getStages().stream()
                        .map(stage -> ApprovalStageResponse.builder()
                                .stageId(stage.getId())
                                .stageOrder(stage.getStageOrder())
                                .approver(stage.getApprover().getId())
                                .approverName(stage.getApprover().getFullName())
                                .department(stage.getApprover().getDepartment())
                                .status(stage.getStatus())
                                .startDate(stage.getStartDate())
                                .endDate(stage.getDueDate())
                                .build())
                        .toList())
                .build();
    }

    @Override
    @Transactional
    public List<ApprovalWorkflowResponse> getWorkflowByAddendumTypeId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        List<ApprovalWorkflow> workflow = approvalWorkflowRepository.findTop3ByUser_IdAndAddendumNotNullOrderByCreatedAtDesc(currentUser.getId());

        // Chuyển đổi ApprovalWorkflow thành ApprovalWorkflowResponse
        return workflow.stream()
                .map(workflows -> ApprovalWorkflowResponse.builder()
                        .id(workflows.getId())
                        .name(workflows.getName())
                        .customStagesCount(workflows.getCustomStagesCount())
                        .createdAt(workflows.getCreatedAt())
                        .stages(workflows.getStages().stream()
                                .map(stage -> ApprovalStageResponse.builder()
                                        .stageId(stage.getId())
                                        .stageOrder(stage.getStageOrder())
                                        .approver(stage.getApprover().getId())
                                        .approverName(stage.getApprover().getFullName())
                                        .department(stage.getApprover().getDepartment())
                                        .build())
                                .toList())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public List<CommentResponse> getApprovalStageCommentDetailsByAddendumId(Long addendumId) throws DataNotFoundException {
        // Tìm hợp đồng theo contractId
        Addendum addendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phụ lục"));

        // Lấy quy trình phê duyệt của hợp đồng
        ApprovalWorkflow workflow = addendum.getApprovalWorkflow();
        if (workflow == null) {
            throw new DataNotFoundException("Không tìm thấy quy trình phê duyệt cho phụ lục");
        }

        // Lấy danh sách thông tin comment từ các bước duyệt
        return workflow.getStages().stream()
                // Lọc chỉ những stage có comment (nếu cần)
                .filter(stage -> stage.getComment() != null && !stage.getComment().trim().isEmpty())
                .map(stage -> CommentResponse.builder()
                        .comment(stage.getComment())
                        .commenter(stage.getApprover().getFullName()) // Giả sử method getFullName() trả về tên đầy đủ của người dùng
                        .commentedAt(stage.getApprovedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddendumResponse duplicateAddendum(Long addendumId, Long contractId) throws DataNotFoundException {
        // Lấy thông tin người dùng hiện tại
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        // Tìm hợp đồng
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy hợp đồng"));

        // Kiểm tra trạng thái hợp đồng
        if (contract.getStatus() != ContractStatus.ACTIVE && contract.getStatus() != ContractStatus.EXPIRED) {
            throw new DataNotFoundException("Không thể tạo phụ lục: Hợp đồng đang không ở trạng thái hoạt động hoặc đã hết hạn");
        }

        // Tìm phụ lục gốc
        Addendum originAddendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phụ lục"));

        // Kiểm tra trùng lặp tiêu đề
        String newTitle = originAddendum.getTitle() + " (Copy)";
        boolean isTitleExist = addendumRepository.existsByContractIdAndTitleAndIdNot(contractId, newTitle, addendumId);
        if (isTitleExist) {
            // Nếu tiêu đề đã tồn tại, thêm số thứ tự để tránh trùng lặp
            int copyNumber = 1;
            String baseTitle = newTitle;
            do {
                newTitle = baseTitle + " (" + copyNumber + ")";
                copyNumber++;
            } while (addendumRepository.existsByContractIdAndTitleAndIdNot(contractId, newTitle, addendumId));
        }

        // Tạo bản sao của phụ lục
        Addendum newAddendum = Addendum.builder()
                .title(newTitle)
                .content(originAddendum.getContent())
                .effectiveDate(originAddendum.getEffectiveDate())
                .extendContractDate(originAddendum.getExtendContractDate()) // Sao chép ngày gia hạn
                .contractExpirationDate(originAddendum.getContractExpirationDate()) // Sao chép ngày hết hạn
                .contractNumber(contract.getContractNumber())
                .status(AddendumStatus.CREATED)
                .user(currentUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(null)
                .contract(contract)
                .signedFilePath(null)
                .signedBy(null)
                .signedAt(null)
                .contractContent(originAddendum.getContractContent())
                .signedAddendumUrls(new ArrayList<>())
                .build();

        // Sao chép AddendumItems
        List<AddendumItem> duplicateItems = new ArrayList<>();
        for (AddendumItem originalItem : originAddendum.getAddendumItems()) {
            AddendumItem duplicateItem = AddendumItem.builder()
                    .description(originalItem.getDescription())
                    .amount(originalItem.getAmount())
                    .itemOrder(originalItem.getItemOrder())
                    .addendum(newAddendum)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            duplicateItems.add(duplicateItem);
        }
        newAddendum.setAddendumItems(duplicateItems);

        // Sao chép AddendumTerms
        List<AddendumTerm> duplicateTerms = new ArrayList<>();
        for (AddendumTerm originalTerm : originAddendum.getAddendumTerms()) {
            AddendumTerm duplicateTerm = AddendumTerm.builder()
                    .termLabel(originalTerm.getTermLabel())
                    .termValue(originalTerm.getTermValue())
                    .termType(originalTerm.getTermType())
                    .originalTermId(originalTerm.getOriginalTermId())
                    .addendum(newAddendum)
                    .build();
            duplicateTerms.add(duplicateTerm);
        }
        newAddendum.setAddendumTerms(duplicateTerms);

        // Sao chép AdditionalTermDetails
        List<AddendumAdditionalTermDetail> duplicateDetails = new ArrayList<>();
        for (AddendumAdditionalTermDetail originalDetail : originAddendum.getAdditionalTermDetails()) {
            // Tạo bản sao của các danh sách con
            List<AdditionalTermSnapshot> commonTermsCopy = new ArrayList<>();
            for (AdditionalTermSnapshot snapshot : originalDetail.getCommonTerms()) {
                commonTermsCopy.add(AdditionalTermSnapshot.builder()
                        .termId(snapshot.getTermId())
                        .termLabel(snapshot.getTermLabel())
                        .termValue(snapshot.getTermValue())
                        .build());
            }

            List<AdditionalTermSnapshot> aTermsCopy = new ArrayList<>();
            for (AdditionalTermSnapshot snapshot : originalDetail.getATerms()) {
                aTermsCopy.add(AdditionalTermSnapshot.builder()
                        .termId(snapshot.getTermId())
                        .termLabel(snapshot.getTermLabel())
                        .termValue(snapshot.getTermValue())
                        .build());
            }

            List<AdditionalTermSnapshot> bTermsCopy = new ArrayList<>();
            for (AdditionalTermSnapshot snapshot : originalDetail.getBTerms()) {
                bTermsCopy.add(AdditionalTermSnapshot.builder()
                        .termId(snapshot.getTermId())
                        .termLabel(snapshot.getTermLabel())
                        .termValue(snapshot.getTermValue())
                        .build());
            }

            AddendumAdditionalTermDetail duplicateDetail = AddendumAdditionalTermDetail.builder()
                    .typeTermId(originalDetail.getTypeTermId())
                    .commonTerms(commonTermsCopy)
                    .aTerms(aTermsCopy)
                    .bTerms(bTermsCopy)
                    .addendum(newAddendum)
                    .build();
            duplicateDetails.add(duplicateDetail);
        }
        newAddendum.setAdditionalTermDetails(duplicateDetails);

        // Sao chép PaymentSchedules
        List<AddendumPaymentSchedule> duplicatePayments = new ArrayList<>();
        for (AddendumPaymentSchedule originalPayment : originAddendum.getPaymentSchedules()) {
            AddendumPaymentSchedule duplicatePayment = AddendumPaymentSchedule.builder()
                    .paymentOrder(originalPayment.getPaymentOrder())
                    .amount(originalPayment.getAmount())
                    .paymentDate(originalPayment.getPaymentDate())
                    .status(PaymentStatus.UNPAID) // Đặt trạng thái mới
                    .paymentMethod(originalPayment.getPaymentMethod())
                    .notifyPaymentDate(originalPayment.getNotifyPaymentDate())
//                    .notifyPaymentContent(originalPayment.getNotifyPaymentContent())
                    .reminderEmailSent(false)
                    .overdueEmailSent(false)
                    .addendum(newAddendum)
                    .build();
            duplicatePayments.add(duplicatePayment);
        }
        newAddendum.setPaymentSchedules(duplicatePayments);

        // Lưu bản sao vào cơ sở dữ liệu
        Addendum savedAddendum = addendumRepository.save(newAddendum);

        // Ghi log audit
        logAuditTrailForAddendum(savedAddendum, "CREATE", "status", null, AddendumStatus.CREATED.name(), currentUser.getUsername());

        // Trả về thông tin bản sao
        return AddendumResponse.builder()
                .addendumId(savedAddendum.getId())
                .title(savedAddendum.getTitle())
                .content(savedAddendum.getContent())
                .contractNumber(savedAddendum.getContractNumber())
                .status(savedAddendum.getStatus())
                .createdBy(UserAddendumResponse.builder()
                        .userId(savedAddendum.getUser().getId())
                        .userName(savedAddendum.getUser().getUsername())
                        .build())
                .contractId(savedAddendum.getContract().getId())
                .effectiveDate(savedAddendum.getEffectiveDate())
                .createdAt(savedAddendum.getCreatedAt())
                .updatedAt(savedAddendum.getUpdatedAt())
                .build();
    }
    @Override
    @Transactional
    public void uploadSignedAddendum(Long addendumId, List<MultipartFile> files) throws DataNotFoundException {
        Addendum addendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phụ lục"));

        try {
            //  Xoá file cũ khỏi Cloudinary (nếu có)
//            for (String oldUrl : contract.getSignedContractUrls()) {
//                String publicId = extractPublicIdFromUrl(oldUrl);
//                if (publicId != null) {
//                    // Xác định resource_type là raw (pdf) hay image
//                    String resourceType = publicId.endsWith(".pdf") ? "raw" : "image";
//                    cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType));
//                }
//            }
            // Xóa tất cả các hình ảnh cũ (nếu cần) nếu bạn muốn thay thế hoàn toàn
            addendum.getSignedAddendumUrls().clear();

            List<String> uploadedUrls = new ArrayList<>();

            for (MultipartFile file : files) {
                // Kiểm tra định dạng hợp lệ
                MediaType mediaType = MediaType.parseMediaType(Objects.requireNonNull(file.getContentType()));
                if (!mediaType.isCompatibleWith(MediaType.IMAGE_JPEG)
                        && !mediaType.isCompatibleWith(MediaType.IMAGE_PNG)
                        && !mediaType.isCompatibleWith(MediaType.APPLICATION_PDF)) {
                    throw new InvalidParamException(localizationUtils.getLocalizedMessage("File tải lên phải là file hình ảnh hoặc PDF"));
                }

                // Xác định resource_type
                String resourceType = mediaType.isCompatibleWith(MediaType.APPLICATION_PDF) ? "raw" : "image";

                // Upload file lên Cloudinary với tên file gốc, có thêm chuỗi tránh trùng
                Map uploadResult = cloudinary.uploader().upload(
                        file.getBytes(),
                        ObjectUtils.asMap(
                                "folder", "signed_addendum_done/" + addendumId,
                                "use_filename", true,
                                "unique_filename", true,
                                "resource_type", resourceType,
                                "format", mediaType.getSubtype()
                        )
                );

                // Lấy URL an toàn của file
                String signedUrl = uploadResult.get("secure_url").toString();

                // Nếu là file PDF, tạo URL tải xuống với tên file gốc và định dạng PDF
                if (mediaType.isCompatibleWith(MediaType.APPLICATION_PDF)) {
                    String originalFilename = file.getOriginalFilename();
                    String customFilename = normalizeFilename(originalFilename);

                    // Encode the filename for URL safety
                    String encodedFilename = URLEncoder.encode(customFilename, "UTF-8");

                    // Generate a secure download URL for PDF with the correct filename
                    signedUrl = cloudinary.url()
                            .resourceType("raw")
                            .publicId(uploadResult.get("public_id").toString())
                            .secure(true)
                            .transformation(new Transformation().flags("attachment:" + customFilename)) // Ensure it's downloaded as an attachment
                            .generate();
                }
                uploadedUrls.add(signedUrl);
            }

            // Ghi lại danh sách URL mới
            addendum.getSignedAddendumUrls().addAll(uploadedUrls);

            // Cập nhật trạng thái hợp đồng (có thể tuỳ chỉnh logic)
            //addendum.setStatus(ContractStatus.ACTIVE);
            addendumRepository.save(addendum);
        } catch (IOException e) {
            logger.error("Failed to upload urls for addendum with ID {}", addendumId, e);
        }
    }

    @Override
    public List<String> getSignedAddendumUrl(Long addendumId) throws DataNotFoundException {
        List<String> billUrls = addendumRepository.findSignedAddendumUrls(addendumId);

        if (billUrls == null || billUrls.isEmpty()) {
            throw new DataNotFoundException("Không tìm thấy URLs");
        }

        return billUrls;
    }

    @Override
    public void uploadFileBase64(Long addendumId, FileBase64DTO fileBase64DTO, String fileName) throws DataNotFoundException, IOException {
        Addendum addendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phụ lục"));

        if (addendum.getStatus() == AddendumStatus.SIGNED) {
            throw new RuntimeException("Phụ lục này đã được kí trước đó");
        }
        byte[] fileBytes = Base64.getDecoder().decode(fileBase64DTO.getFileBase64());

        // Upload as a raw file to Cloudinary
        Map<String, Object> uploadResult = cloudinary.uploader().upload(fileBytes, ObjectUtils.asMap(
                "resource_type", "raw",      // Cho phép upload file dạng raw
                "folder", "signed_addenda",
                "use_filename", true,        // Sử dụng tên file gốc làm public_id
                "unique_filename", true,
                "format", "pdf"
        ));

        // Lấy public ID của file đã upload
        String publicId = (String) uploadResult.get("public_id");

        // Lấy tên file gốc và chuẩn hóa (loại bỏ dấu, ký tự không hợp lệ)
        String customFilename = normalizeFilename(fileName);

        // URL-encode tên file (một lần encoding là đủ khi tên đã là ASCII)
        String encodedFilename = URLEncoder.encode(customFilename, "UTF-8");

        // Tạo URL bảo mật với transformation flag attachment:<custom_filename>
        // Khi tải file về, trình duyệt sẽ đặt tên file theo customFilename
        String secureUrl = cloudinary.url()
                .resourceType("raw")
                .publicId(publicId)
                .secure(true)
                .transformation(new Transformation().flags("attachment:" + encodedFilename))
                .generate();

        addendum.setSignedFilePath(secureUrl);
        addendum.setStatus(AddendumStatus.SIGNED);
        addendumRepository.save(addendum);
    }

    @Override
    public void uploadPaymentBillUrls(Long paymentScheduleId, List<MultipartFile> files) throws DataNotFoundException {
        AddendumPaymentSchedule addendumPaymentSchedule = addendumPaymentScheduleRepository.findById(paymentScheduleId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy lịch thanh toán"));

//        // Nếu thuộc Contract, kiểm tra điều kiện
//        if (paymentSchedule.getContract() != null) {
//            Contract contract = paymentSchedule.getContract();
//
//            // Kiểm tra status SIGNED + ACTIVE (dựa vào ngày)
//            boolean isActive = contract.getEffectiveDate() != null &&
//                    contract.getExpiryDate() != null &&
//                    !contract.getEffectiveDate().isAfter(LocalDateTime.now()) &&
//                    !contract.getExpiryDate().isBefore(LocalDateTime.now());
//
//            if (!ContractStatus.SIGNED.equals(contract.getStatus()) || !isActive) {
//                throw new InvalidParamException("Chỉ cho upload bằng chứng thanh toán khi hợp đồng đã ký hoặc đang hoạt động");
//            }
//        }

        try {
            // 🔥 Xoá các file cũ trên Cloudinary nếu có
            for (String oldUrl : addendumPaymentSchedule.getBillUrls()) {
                String publicId = extractPublicIdFromUrl(oldUrl);
                if (publicId != null) {
                    cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
                }
            }

            // Xoá danh sách URL cũ trong DB
            addendumPaymentSchedule.getBillUrls().clear();

            List<String> uploadedUrls = new ArrayList<>();

            for (MultipartFile file : files) {
                // Kiểm tra định dạng hình ảnh
                MediaType mediaType = MediaType.parseMediaType(Objects.requireNonNull(file.getContentType()));
                if (!mediaType.isCompatibleWith(MediaType.IMAGE_JPEG) &&
                        !mediaType.isCompatibleWith(MediaType.IMAGE_PNG)) {
                    throw new InvalidParamException(localizationUtils.getLocalizedMessage(MessageKeys.UPLOAD_IMAGES_FILE_MUST_BE_IMAGE));
                }

                // Upload lên Cloudinary
                Map uploadResult = cloudinary.uploader().upload(
                        file.getBytes(),
                        ObjectUtils.asMap(
                                "folder", "payment_bill/" + paymentScheduleId,
                                "use_filename", true,
                                "unique_filename", true,
                                "resource_type", "image"
                        )
                );

                // Lấy URL ảnh đã upload
                String billUrl = uploadResult.get("secure_url").toString();
                uploadedUrls.add(billUrl);
            }

            // Lưu danh sách URL mới
            addendumPaymentSchedule.getBillUrls().addAll(uploadedUrls);
            addendumPaymentSchedule.setStatus(PaymentStatus.PAID);
            addendumPaymentScheduleRepository.save(addendumPaymentSchedule);

        } catch (IOException e) {
            logger.error("Không tải được url hóa đơn cho lịch thanh toán. Lỗi:", e);
        }
    }

    @Override
    public List<String> getBillUrlsByAddendumPaymentId(Long paymentId) throws DataNotFoundException {
        List<String> billUrls = addendumPaymentScheduleRepository.findBillUrlsByPaymentId(paymentId);

        if (billUrls == null || billUrls.isEmpty()) {
            throw new DataNotFoundException("Không tìm thấy bill url với đợt thanh toán");
        }

        return billUrls;
    }

    private String extractPublicIdFromUrl(String url) {
        try {
            // Ví dụ URL:
            // https://res.cloudinary.com/your_cloud_name/image/upload/v1234567890/payment_bill/12/filename_xyz.png
            // Cần tách phần sau: payment_bill/12/filename_xyz

            URI uri = new URI(url);
            String path = uri.getPath(); // /your_cloud_name/image/upload/v1234567890/payment_bill/12/file.png
            int versionIndex = path.indexOf("/v"); // tìm vị trí bắt đầu version

            if (versionIndex != -1) {
                String publicPath = path.substring(versionIndex + 2); // bỏ "/v" và version
                int slashIndex = publicPath.indexOf('/');
                if (slashIndex != -1) {
                    return publicPath.substring(slashIndex + 1, publicPath.lastIndexOf('.')); // bỏ phần mở rộng .jpg/.png
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to extract publicId from URL: {}", url);
        }
        return null;
    }

    private String normalizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "file";
        }
        // Loại bỏ extension nếu có
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex != -1) {
            filename = filename.substring(0, dotIndex);
        }
        // Chuẩn hóa Unicode: tách dấu
        String normalized = Normalizer.normalize(filename, Normalizer.Form.NFD);
        // Loại bỏ dấu (diacritics)
        normalized = normalized.replaceAll("\\p{M}", "");
        // Giữ lại chữ, số, dấu gạch dưới, dấu gạch ngang, khoảng trắng và dấu chấm than
        normalized = normalized.replaceAll("[^\\w\\-\\s!]", "");
        // Chuyển khoảng trắng thành dấu gạch dưới và trim
        normalized = normalized.trim().replaceAll("\\s+", "_");
        return normalized;
    }

    private AddendumResponse mapToAddendumResponse(Addendum addendum) {
        return AddendumResponse.builder()
                .addendumId(addendum.getId())
                .title(addendum.getTitle())
                .content(addendum.getContent())
                .contractNumber(addendum.getContractNumber())
                .effectiveDate(addendum.getEffectiveDate())
                .status(addendum.getStatus())
                .createdBy(UserAddendumResponse.builder()
                        .userId(addendum.getUser().getId())
                        .userName(addendum.getUser().getFullName())
                        .build())
                .createdAt(addendum.getCreatedAt())
                .signedFilePath(addendum.getSignedFilePath())
                .updatedAt(addendum.getUpdatedAt())
                .contractId(addendum.getContract() != null ? addendum.getContract().getId() : null)
                .build();

    }

    private String translateAddendumStatusToVietnamese(String status) {
        switch (status) {
            case "DRAFT":
                return "Bản nháp";
            case "CREATED":
                return "Đã tạo";
            case "UPDATED":
                return "Đã cập nhật";
            case "APPROVAL_PENDING":
                return "Chờ phê duyệt";
            case "APPROVED":
                return "Đã phê duyệt";
            case "REJECTED":
                return "Bị từ chối";
            case "SIGNED":
                return "Đã ký";
            case "ACTIVE":
                return "Đang có hiệu lực";
            case "COMPLETED":
                return "Hoàn thành";
            case "EXPIRED":
                return "Hết hạn";
            case "CANCELLED":
                return "Đã hủy";
            default:
                return status;
        }
    }

    private void logAuditTrailForAddendum(Addendum addendum, String action, String fieldName, String oldValue, String newValue, String changedBy) {
        String oldStatusVi = oldValue != null ? translateAddendumStatusToVietnamese(oldValue) : null;
        String newStatusVi = newValue != null ? translateAddendumStatusToVietnamese(newValue) : null;

        String changeSummary;
        if ("CREATE".equalsIgnoreCase(newValue)) {
            changeSummary = "Đã tạo mới phụ lục với trạng thái '" + (newStatusVi != null ? newStatusVi : "Không có") + "'";
        } else {
            changeSummary = String.format("Đã cập nhật trạng thái phụ lục từ '%s' sang '%s'",
                    oldStatusVi != null ? oldStatusVi : "Không có",
                    newStatusVi != null ? newStatusVi : "Không có");
        }

        AuditTrail auditTrail = AuditTrail.builder()
                .contract(addendum.getContract())
                .entityName("Addendum")
                .entityId(addendum.getId())
                .action(action)
                .fieldName(fieldName)
                .oldValue(oldStatusVi)
                .newValue(newStatusVi)
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .changeSummary(changeSummary)
                .build();
        auditTrailRepository.save(auditTrail);
    }

}

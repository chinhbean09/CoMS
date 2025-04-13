package com.capstone.contractmanagement.services.addendum;

import com.capstone.contractmanagement.components.LocalizationUtils;
import com.capstone.contractmanagement.dtos.addendum.AddendumDTO;
import com.capstone.contractmanagement.dtos.approvalworkflow.AddendumApprovalWorkflowDTO;
import com.capstone.contractmanagement.dtos.approvalworkflow.WorkflowDTO;
import com.capstone.contractmanagement.entities.*;
import com.capstone.contractmanagement.entities.approval_workflow.ApprovalStage;
import com.capstone.contractmanagement.entities.approval_workflow.ApprovalWorkflow;
import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.entities.contract.ContractPartner;
import com.capstone.contractmanagement.enums.AddendumStatus;
import com.capstone.contractmanagement.enums.ApprovalStatus;
import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.enums.PartnerType;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.exceptions.InvalidParamException;
import com.capstone.contractmanagement.repositories.*;
import com.capstone.contractmanagement.responses.addendum.AddendumResponse;
import com.capstone.contractmanagement.responses.addendum.AddendumTypeResponse;
import com.capstone.contractmanagement.responses.addendum.UserAddendumResponse;
import com.capstone.contractmanagement.responses.approvalworkflow.ApprovalStageResponse;
import com.capstone.contractmanagement.responses.approvalworkflow.ApprovalWorkflowResponse;
import com.capstone.contractmanagement.responses.approvalworkflow.CommentResponse;
import com.capstone.contractmanagement.services.contract.ContractService;
import com.capstone.contractmanagement.services.notification.INotificationService;
import com.capstone.contractmanagement.services.sendmails.IMailService;
import com.capstone.contractmanagement.utils.MessageKeys;
import com.cloudinary.Cloudinary;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddendumService implements IAddendumService{
    private final IAddendumRepository addendumRepository;
    private final IContractRepository contractRepository;
    private final IAddendumTypeRepository addendumTypeRepository;
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
    private static final Logger logger = LoggerFactory.getLogger(AddendumService.class);

    @Override
    @Transactional
    public AddendumResponse createAddendum(AddendumDTO addendumDTO) throws DataNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        // Lấy hợp đồng từ DTO
        Contract contract = contractRepository.findById(addendumDTO.getContractId())
                .orElseThrow(() -> new DataNotFoundException("Contract not found"));

        // Kiểm tra xem có phụ lục nào với cùng title cho hợp đồng này chưa
        boolean isTitleExist = addendumRepository.existsByContractIdAndTitle(contract.getId(), addendumDTO.getTitle());
        if (isTitleExist) {
            throw new DataNotFoundException("Tên phụ lục bị trùng: " + addendumDTO.getTitle());
        }

        Partner partnerA = partnerRepository.findById(1L)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy bên A mặc định"));

        Optional<ContractPartner> contractPartners = contractPartnerRepository.findByContractIdAndPartnerType(contract.getId(), PartnerType.PARTNER_B);

        // Lấy loại phụ lục
        AddendumType addendumType = addendumTypeRepository.findById(addendumDTO.getAddendumTypeId())
                .orElseThrow(() -> new DataNotFoundException("Loại phụ lục không tìm thấy với id : " + addendumDTO.getAddendumTypeId()));

        if (contract.getStatus() == ContractStatus.ACTIVE
                || contract.getStatus() == ContractStatus.EXPIRED) {
            // Tạo phụ lục mới
            Addendum addendum = Addendum.builder()
                    .title(addendumDTO.getTitle())
                    .content(addendumDTO.getContent())
                    .effectiveDate(addendumDTO.getEffectiveDate())
                    .contractNumber(contract.getContractNumber())
                    .status(AddendumStatus.CREATED)
                    .user(currentUser)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(null)
                    .addendumType(addendumType)
                    .contract(contract)
                    .build();

            // Lưu phụ lục
            addendumRepository.save(addendum);

            logAuditTrailForAddendum(addendum, "CREATE", "status", null, AddendumStatus.CREATED.name(), currentUser.getUsername());
            // Trả về thông tin phụ lục đã tạo
            return AddendumResponse.builder()
                    .addendumId(addendum.getId())
                    .title(addendum.getTitle())
                    .content(addendum.getContent())
                    .contractNumber(addendum.getContractNumber())
                    .status(addendum.getStatus())
                    .createdBy(UserAddendumResponse.builder()
                            .userId(currentUser.getId())
                            .userName(currentUser.getUsername())
                            .build())
                    .contractId(addendum.getContract().getId())
                    .addendumType(AddendumTypeResponse.builder()
                            .addendumTypeId(addendum.getAddendumType().getId())
                            .name(addendum.getAddendumType().getName())
                            .build())
                    .partnerA(partnerA)
                    .partnerB(contractPartners)
                    .effectiveDate(addendum.getEffectiveDate())
                    .createdAt(addendum.getCreatedAt())
                    .updatedAt(addendum.getUpdatedAt())
                    .build();
        }

        throw new DataNotFoundException("Cannot create addendum: Contract is not ACTIVE");
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddendumResponse> getAllByContractId(Long contractId) throws DataNotFoundException {
        // Kiểm tra hợp đồng có tồn tại không
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new DataNotFoundException("Contract not found with id: " + contractId));

        Partner partnerA = partnerRepository.findById(1L)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy bên A mặc định"));

        Optional<ContractPartner> contractPartners = contractPartnerRepository.findByContractIdAndPartnerType(contract.getId(), PartnerType.PARTNER_B);
        // Lấy danh sách phụ lục theo contract id (giả sử repository có method: findByContract_Id)
        List<Addendum> addenda = addendumRepository.findByContract(contract);

        // Nếu không có phụ lục, có thể trả về danh sách rỗng hoặc ném ngoại lệ
        if (addenda.isEmpty()) {
            throw new DataNotFoundException("No addendum found for contract id: " + contractId);
        }

        // Map entity thành DTO
        return addenda.stream()
                .map(addendum -> AddendumResponse.builder()
                        .addendumId(addendum.getId())
                        .title(addendum.getTitle())
                        .content(addendum.getContent())
                        .effectiveDate(addendum.getEffectiveDate())
                        .contractNumber(addendum.getContractNumber())
                        .addendumType(AddendumTypeResponse.builder()
                                .addendumTypeId(addendum.getAddendumType().getId())
                                .name(addendum.getAddendumType().getName())
                                .build())
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
    public List<AddendumResponse> getAllByAddendumType(Long addendumTypeId) throws DataNotFoundException {
        AddendumType addendumType = addendumTypeRepository.findById(addendumTypeId)
                .orElseThrow(() -> new DataNotFoundException("Addendum type not found with id: " + addendumTypeId));
        List<Addendum> addenda = addendumRepository.findByAddendumType(addendumType);
        Partner partnerA = partnerRepository.findById(1L)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy bên A mặc định"));
        return addenda.stream()
                .map(addendum -> {
                    // Lấy partner B theo contract
                    Optional<ContractPartner> partnerB = contractPartnerRepository
                            .findByContractIdAndPartnerType(addendum.getContract().getId(), PartnerType.PARTNER_B);

                    return AddendumResponse.builder()
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
                            .partnerB(partnerB)
                            .contractId(addendum.getContract().getId())
                            .addendumType(AddendumTypeResponse.builder()
                                    .addendumTypeId(addendum.getAddendumType().getId())
                                    .name(addendum.getAddendumType().getName())
                                    .build())
                            .createdAt(addendum.getCreatedAt())
                            .updatedAt(addendum.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public String updateAddendum(Long addendumId, AddendumDTO addendumDTO) throws DataNotFoundException {
        // Tìm phụ lục theo id

        Addendum addendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Addendum not found with id: " + addendumId));

        String oldStatus = addendum.getStatus().name();

        if (addendum.getStatus().equals(AddendumStatus.APPROVAL_PENDING)) {
            throw new RuntimeException("Phụ lục đang trong quy trình duyệt, không được phép cập nhật.");
        }
        if (addendumDTO.getAddendumTypeId() != null) {
            AddendumType addendumType = addendumTypeRepository.findById(addendumDTO.getAddendumTypeId())
                    .orElseThrow(() -> new DataNotFoundException("Loại phụ lục không tìm thấy với id : " + addendumDTO.getAddendumTypeId()));
            addendum.setAddendumType(addendumType);
        }

        if (addendumDTO.getTitle() != null) {
            addendum.setTitle(addendumDTO.getTitle());
        }
        if (addendumDTO.getContent() != null) {
            addendum.setContent(addendumDTO.getContent());
        }
        if (addendumDTO.getEffectiveDate() != null) {
            addendum.setEffectiveDate(addendumDTO.getEffectiveDate());
        }
        addendum.setStatus(AddendumStatus.UPDATED);
        addendum.setUpdatedAt(LocalDateTime.now());

        addendumRepository.save(addendum);
        String changedBy = SecurityContextHolder.getContext().getAuthentication().getName();
        logAuditTrailForAddendum(addendum, "UPDATE", "status", oldStatus, AddendumStatus.UPDATED.name(), changedBy);
        return "Addendum updated successfully.";
    }

    @Override
    @Transactional
    public void deleteAddendum(Long addendumId) throws DataNotFoundException {
        // Tìm phụ lục theo id
        Addendum addendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Addendum not found with id: " + addendumId));
        // Xóa phụ lục
        addendumRepository.delete(addendum);
    }

    @Override
    public AddendumResponse getAddendumById(Long addendumId) throws DataNotFoundException {
        Addendum addendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Addendum not found with id: " + addendumId));
        Partner partnerA = partnerRepository.findById(1L)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy bên A mặc định"));
        Optional<ContractPartner> contractPartners = contractPartnerRepository.findByContractIdAndPartnerType(addendum.getContract().getId(), PartnerType.PARTNER_B);
        return AddendumResponse.builder()
                .addendumId(addendum.getId())
                .title(addendum.getTitle())
                .content(addendum.getContent())
                .contractNumber(addendum.getContractNumber())
                .status(addendum.getStatus())
                .createdBy(UserAddendumResponse.builder()
                        .userId(addendum.getUser().getId())
                        .userName(addendum.getUser().getFullName())
                        .build())
                .partnerA(partnerA)
                .partnerB(contractPartners)
                .contractId(addendum.getContract().getId())
                .addendumType(AddendumTypeResponse.builder()
                        .addendumTypeId(addendum.getAddendumType().getId())
                        .name(addendum.getAddendumType().getName())
                        .build())
                .effectiveDate(addendum.getEffectiveDate())
                .createdAt(addendum.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void assignApprovalWorkflowOfContractToAddendum(Long addendumId) throws DataNotFoundException {
        // Lấy phụ lục hợp đồng và hợp đồng gốc
        Addendum addendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Phụ lục hợp đồng không tìm thấy với id: " + addendumId));

        Contract contract = contractRepository.findById(addendum.getContract().getId())
                .orElseThrow(() -> new DataNotFoundException("Hợp đồng không tìm thấy"));

        ApprovalWorkflow contractApprovalWorkflow = contract.getApprovalWorkflow();
        if (contractApprovalWorkflow == null) {
            throw new DataNotFoundException("Contract approval workflow not found");
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
                .orElseThrow(() -> new DataNotFoundException("Phụ lục hợp đồng không tìm thấy với id " + addendumId));

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
                .orElseThrow(() -> new DataNotFoundException("Addendum not found"));

        // Tìm ApprovalStage theo stageId
        ApprovalStage stage = approvalStageRepository.findById(stageId)
                .orElseThrow(() -> new DataNotFoundException("Approval stage not found"));

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

                String changedBy = currentUser.getUsername();
                logAuditTrailForAddendum(addendum, "UPDATE", "status", oldStatus, AddendumStatus.APPROVED.name(), changedBy);
                Map<String, Object> payload = new HashMap<>();
                String notificationMessage = "Phụ lục: " + addendum.getTitle() + " của hợp đồng số " + addendum.getContractNumber() + " đã duyệt xong.";
                payload.put("message", notificationMessage);
                payload.put("addendumId", addendumId);

                // Gửi thông báo cho người duyệt tiếp theo
                mailService.sendEmailApprovalSuccessForAddendum(addendum, addendum.getUser());
                notificationService.saveNotification(addendum.getUser(), notificationMessage, addendum.getContract());
                messagingTemplate.convertAndSendToUser(addendum.getUser().getFullName(), "/queue/notifications", payload);
            }
        }
    }

    @Override
    @Transactional
    public void rejectStageForAddendum(Long addendumId, Long stageId, WorkflowDTO workflowDTO) throws DataNotFoundException {
        // Lấy phụ lục theo addendumId
        Addendum addendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Addendum not found"));

        // Tìm ApprovalStage theo stageId
        ApprovalStage stage = approvalStageRepository.findById(stageId)
                .orElseThrow(() -> new DataNotFoundException("Approval stage not found"));

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
            List<Long> addendumTypeIds,
            int page,
            int size,
            User currentUser) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        boolean hasSearch = keyword != null && !keyword.trim().isEmpty();
        boolean hasStatusFilter = statuses != null && !statuses.isEmpty();
        boolean hasTypeFilter = addendumTypeIds != null && !addendumTypeIds.isEmpty();

        Page<Addendum> addenda;

        boolean isCeo = currentUser.getRole() != null && "DIRECTOR".equalsIgnoreCase(currentUser.getRole().getRoleName());
        boolean isStaff = currentUser.getRole() != null && "STAFF".equalsIgnoreCase(currentUser.getRole().getRoleName());

        if (isStaff) {
            if (hasStatusFilter) {
                if (hasTypeFilter) {
                    if (hasSearch) {
                        addenda = addendumRepository.findByContractUserIdAndKeywordAndStatusInAndAddendumTypeIdIn(
                                userId, keyword.trim(), statuses, addendumTypeIds, pageable);
                    } else {
                        addenda = addendumRepository.findByContractUserIdAndStatusInAndAddendumTypeIdIn(
                                userId, statuses, addendumTypeIds, pageable);
                    }
                } else {
                    if (hasSearch) {
                        addenda = addendumRepository.findByContractUserIdAndKeywordAndStatusIn(
                                userId, keyword.trim(), statuses, pageable);
                    } else {
                        addenda = addendumRepository.findByContractUserIdAndStatusIn(
                                userId, statuses, pageable);
                    }
                }
            } else {
                if (hasTypeFilter) {
                    if (hasSearch) {
                        addenda = addendumRepository.findByContractUserIdAndKeywordAndAddendumTypeIdIn(
                                userId, keyword.trim(), addendumTypeIds, pageable);
                    } else {
                        addenda = addendumRepository.findByContractUserIdAndAddendumTypeIdIn(
                                userId, addendumTypeIds, pageable);
                    }
                } else {
                    if (hasSearch) {
                        addenda = addendumRepository.findByContractUserIdAndKeyword(
                                userId, keyword.trim(), pageable);
                    } else {
                        addenda = addendumRepository.findByContractUserId(userId, pageable);
                    }
                }
            }
        } else if (isCeo) {
            if (hasStatusFilter) {
                if (hasTypeFilter) {
                    if (hasSearch) {
                        addenda = addendumRepository.findByKeywordAndStatusInAndAddendumTypeIdIn(
                                keyword.trim(), statuses, addendumTypeIds, pageable);
                    } else {
                        addenda = addendumRepository.findByStatusInAndAddendumTypeIdIn(
                                statuses, addendumTypeIds, pageable);
                    }
                } else {
                    if (hasSearch) {
                        addenda = addendumRepository.findByKeywordAndStatusIn(
                                keyword.trim(), statuses, pageable);
                    } else {
                        addenda = addendumRepository.findByStatusIn(statuses, pageable);
                    }
                }
            } else {
                if (hasTypeFilter) {
                    if (hasSearch) {
                        addenda = addendumRepository.findByKeywordAndAddendumTypeIdIn(
                                keyword.trim(), addendumTypeIds, pageable);
                    } else {
                        addenda = addendumRepository.findByAddendumTypeIdIn(addendumTypeIds, pageable);
                    }
                } else {
                    if (hasSearch) {
                        addenda = addendumRepository.findByKeyword(keyword.trim(), pageable);
                    } else {
                        addenda = addendumRepository.findAll(pageable);
                    }
                }
            }
        } else {
            addenda = Page.empty(pageable);
        }

        return addenda.map(this::mapToAddendumResponse);
    }

    @Override
    @Transactional
    public Page<AddendumResponse> getAddendaForManager(Long approverId, String keyword, Long addendumTypeId, int page, int size) {
        // Cấu hình phân trang
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Lấy tất cả các phụ lục đang ở trạng thái APPROVAL_PENDING
        //List<Addendum> pendingAddenda = addendumRepository.findByStatus(AddendumStatus.APPROVAL_PENDING);
        List<Addendum> pendingAddenda = addendumRepository.findAll();

        // Lọc các phụ lục theo approverId, keyword và addendumTypeId
        // Lọc phụ lục dựa trên quyền duyệt của approver
        List<Addendum> filteredAddenda = pendingAddenda.stream()
                .filter(addendum -> {
                    ApprovalWorkflow workflow = addendum.getApprovalWorkflow();
                    if (workflow == null || workflow.getStages().isEmpty()) return false;

                    if (addendum.getStatus() == AddendumStatus.APPROVAL_PENDING) {
                        // Tìm bước duyệt hiện tại (sớm nhất có trạng thái NOT_STARTED / REJECTED / APPROVING)
                        OptionalInt currentStageOrderOpt = workflow.getStages().stream()
                                .filter(stage -> stage.getStatus() == ApprovalStatus.NOT_STARTED
                                        || stage.getStatus() == ApprovalStatus.REJECTED
                                        || stage.getStatus() == ApprovalStatus.APPROVING)
                                .mapToInt(ApprovalStage::getStageOrder)
                                .min();

                        if (currentStageOrderOpt.isEmpty()) return false;

                        int currentStageOrder = currentStageOrderOpt.getAsInt();

                        // Kiểm tra nếu approver có quyền duyệt bước này
                        return workflow.getStages().stream()
                                .anyMatch(stage -> stage.getStageOrder() <= currentStageOrder
                                        && stage.getApprover().getId().equals(approverId));
                    } else {
                        // Đã duyệt xong: kiểm tra xem approver có từng duyệt qua không
                        return workflow.getStages().stream()
                                .anyMatch(stage -> stage.getApprover().getId().equals(approverId)
                                        && stage.getStatus() == ApprovalStatus.APPROVED);
                    }
                })
                .filter(addendum -> {
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String lowerKeyword = keyword.toLowerCase();
                        return addendum.getTitle().toLowerCase().contains(lowerKeyword)
                                || addendum.getContent().toLowerCase().contains(lowerKeyword);
                    }
                    return true;
                })
                .filter(addendum -> {
                    if (addendumTypeId != null) {
                        return addendum.getAddendumType() != null
                                && addendum.getAddendumType().getId().equals(addendumTypeId);
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
    public Page<AddendumResponse> getAddendaForApprover(Long approverId, String keyword, Long addendumTypeId, int page, int size) {
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
                .filter(addendum -> {
                    if (addendumTypeId != null) {
                        return addendum.getAddendumType() != null
                                && addendum.getAddendumType().getId().equals(addendumTypeId);
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

        ApprovalWorkflow workflow = ApprovalWorkflow.builder()
                .name(approvalWorkflowDTO.getName())
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

            // ✅ Thêm bước duyệt cuối cùng là Director
//            User director = userRepository.findAll().stream()
//                    .filter(user -> user.getRole() != null && Role.DIRECTOR.equalsIgnoreCase(user.getRole().getRoleName()))
//                    .findFirst()
//                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người duyệt có vai trò DIRECTOR"));
//
//            ApprovalStage directorStage = ApprovalStage.builder()
//                    .stageOrder(workflow.getStages().size() + 1)
//                    .approver(director)
//                    .status(ApprovalStatus.NOT_STARTED)
//                    .approvalWorkflow(workflow)
//                    .build();
//
//            workflow.getStages().add(directorStage);
        }

        // Cập nhật số lượng stage tùy chỉnh dựa trên số stage đã thêm
        workflow.setCustomStagesCount(workflow.getStages().size());
        // Lưu workflow
        approvalWorkflowRepository.save(workflow);


        if (approvalWorkflowDTO.getAddendumTypeId() != null) {
            AddendumType addendumType = addendumTypeRepository.findById(approvalWorkflowDTO.getAddendumTypeId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy loại phụ lục với id " + approvalWorkflowDTO.getAddendumTypeId()));
            workflow.setAddendumType(addendumType);
            approvalWorkflowRepository.save(workflow);
        }

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
    public List<ApprovalWorkflowResponse> getWorkflowByAddendumTypeId(Long addendumTypeId) {
        List<ApprovalWorkflow> workflow = approvalWorkflowRepository.findTop3ByAddendumType_IdOrderByCreatedAtDesc(addendumTypeId);

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
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phụ lục với id : " + addendumId));

        // Lấy quy trình phê duyệt của hợp đồng
        ApprovalWorkflow workflow = addendum.getApprovalWorkflow();
        if (workflow == null) {
            throw new DataNotFoundException("Không tìm thấy quy trình phê duyệt cho phụ lục với id : " + addendumId);
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new DataNotFoundException("Contract not found"));

        Addendum originAddendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phụ lục với id : " + addendumId));

        if (contract.getStatus() == ContractStatus.ACTIVE || contract.getStatus() == ContractStatus.EXPIRED) {
            Addendum newAddendum = new Addendum();
            newAddendum.setAddendumType(originAddendum.getAddendumType());
            newAddendum.setTitle(originAddendum.getTitle() + " (Copy)");
            newAddendum.setStatus(AddendumStatus.CREATED);
            newAddendum.setUser(currentUser);
            newAddendum.setContent(originAddendum.getContent());
            newAddendum.setCreatedAt(LocalDateTime.now());
            newAddendum.setEffectiveDate(originAddendum.getEffectiveDate());
            newAddendum.setUpdatedAt(null);
            newAddendum.setContract(contract);
            newAddendum.setContractNumber(originAddendum.getContractNumber());
            addendumRepository.save(newAddendum);

            return AddendumResponse.builder()
                    .addendumId(newAddendum.getId())
                    .title(newAddendum.getTitle())
                    .content(newAddendum.getContent())
                    .contractNumber(newAddendum.getContractNumber())
                    .status(newAddendum.getStatus())
                    .createdBy(UserAddendumResponse.builder()
                            .userId(currentUser.getId())
                            .userName(currentUser.getUsername())
                            .build())
                    .contractId(newAddendum.getContract().getId())
                    .addendumType(AddendumTypeResponse.builder()
                            .addendumTypeId(newAddendum.getAddendumType().getId())
                            .name(newAddendum.getAddendumType().getName())
                            .build())
                    .effectiveDate(newAddendum.getEffectiveDate())
                    .createdAt(newAddendum.getCreatedAt())
                    .updatedAt(newAddendum.getUpdatedAt())
                    .build();
        } else {
            throw new DataNotFoundException("Không thể tạo phụ lục: Hợp đồng đang không ở trạng thái hoạt động");
        }
    }

    @Override
    @Transactional
    public void uploadSignedAddendum(Long addendumId, List<MultipartFile> files) throws DataNotFoundException {
        Addendum addendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phụ lục với id : " + addendumId));

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
                                "resource_type", resourceType
                        )
                );

                // Lấy URL an toàn của file
                String signedUrl = uploadResult.get("secure_url").toString();
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
            throw new DataNotFoundException("Không tìm thấy URLs với phụ lục ID: " + addendumId);
        }

        return billUrls;
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
                .updatedAt(addendum.getUpdatedAt())
                .contractId(addendum.getContract() != null ? addendum.getContract().getId() : null)
                .addendumType(
                        addendum.getAddendumType() != null ?
                                AddendumTypeResponse.builder()
                                        .addendumTypeId(addendum.getAddendumType().getId())
                                        .name(addendum.getAddendumType().getName())
                                        .build()
                                : null
                )
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

        AuditTrail auditTrail = AuditTrail.builder()
                .contract(addendum.getContract()) // Liên kết với hợp đồng của phụ lục
                .entityName("Addendum")
                .entityId(addendum.getId())
                .action(action)
                .fieldName(fieldName)
                .oldValue(oldStatusVi)
                .newValue(newStatusVi)
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .changeSummary(String.format("Đã cập nhật trạng thái phụ lục từ '%s' sang '%s'",
                        oldStatusVi != null ? oldStatusVi : "Không có",
                        newStatusVi != null ? newStatusVi : "Không có"))
                .build();
        auditTrailRepository.save(auditTrail);
    }


}

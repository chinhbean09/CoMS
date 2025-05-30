package com.capstone.contractmanagement.services.approvalworkflow;

import com.capstone.contractmanagement.dtos.DataMailDTO;
import com.capstone.contractmanagement.dtos.approvalworkflow.ApprovalStageDTO;
import com.capstone.contractmanagement.dtos.approvalworkflow.ApprovalWorkflowDTO;
import com.capstone.contractmanagement.dtos.approvalworkflow.WorkflowDTO;
import com.capstone.contractmanagement.entities.AuditTrail;
import com.capstone.contractmanagement.entities.Role;
import com.capstone.contractmanagement.entities.approval_workflow.ApprovalStage;
import com.capstone.contractmanagement.entities.approval_workflow.ApprovalWorkflow;
import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.entities.contract.ContractType;
import com.capstone.contractmanagement.enums.AddendumStatus;
import com.capstone.contractmanagement.enums.ApprovalStatus;
import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.*;
import com.capstone.contractmanagement.responses.User.UserContractResponse;
import com.capstone.contractmanagement.responses.approvalworkflow.ApprovalStageResponse;
import com.capstone.contractmanagement.responses.approvalworkflow.ApprovalWorkflowResponse;
import com.capstone.contractmanagement.responses.approvalworkflow.CommentResponse;
import com.capstone.contractmanagement.responses.contract.ContractResponse;
import com.capstone.contractmanagement.responses.contract.GetContractForApproverResponse;
import com.capstone.contractmanagement.services.app_config.IAppConfigService;
import com.capstone.contractmanagement.services.notification.INotificationService;
import com.capstone.contractmanagement.services.sendmails.IMailService;
import com.capstone.contractmanagement.utils.MailTemplate;
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApprovalWorkflowService implements IApprovalWorkflowService {
    private final IApprovalWorkflowRepository approvalWorkflowRepository;
    private final IApprovalStageRepository approvalStageRepository;
    private final IContractRepository contractRepository;
    private final IUserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final INotificationService notificationService;
    private final IMailService mailService;
    private final IContractTypeRepository contractTypeRepository;
    private final IAuditTrailRepository auditTrailRepository;
    private final IAppConfigService appConfigService;
    private final IAddendumRepository addendumRepository;


    @Override
    @Transactional
    public ApprovalWorkflowResponse createWorkflow(ApprovalWorkflowDTO approvalWorkflowDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        // Tạo đối tượng workflow mà không gán contract qua builder
        ApprovalWorkflow workflow = ApprovalWorkflow.builder()
                .name(approvalWorkflowDTO.getName())
                //.contractType(contractType)
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

        // Nếu DTO có chứa contractId, gán workflow vừa tạo cho Contract tương ứng
        if (approvalWorkflowDTO.getContractId() != null) {
            Contract contract = contractRepository.findById(approvalWorkflowDTO.getContractId())
                    .orElseThrow(() -> new RuntimeException("Hợp đồng không tìm thấy "));
            contract.setApprovalWorkflow(workflow);
            contractRepository.save(contract);
        }

        if (approvalWorkflowDTO.getContractTypeId() != null) {
            ContractType contractTypes = contractTypeRepository.findById(approvalWorkflowDTO.getContractTypeId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy loại hợp đồng"));
            workflow.setContractType(contractTypes);
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
    public List<ApprovalWorkflowResponse> getAllWorkflows() {
        // get all approval workflows from database
        List<ApprovalWorkflow> approvalWorkflows = approvalWorkflowRepository.findAll();
        return approvalWorkflows.stream()
                .map(workflow -> ApprovalWorkflowResponse.builder()
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
                                        .startDate(stage.getStartDate())
                                        .endDate(stage.getDueDate())
                                        .status(stage.getStatus())
                                        .build())
                                .toList())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void updateWorkflow(Long id, ApprovalWorkflowDTO approvalWorkflowDTO) throws DataNotFoundException {
        // 1. Lấy workflow và kiểm tra tồn tại
        ApprovalWorkflow workflow = approvalWorkflowRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.WORKFLOW_NOT_FOUND));

        // 2. Cập nhật tên workflow và xóa sạch các bước cũ
        workflow.setName(approvalWorkflowDTO.getName());
        workflow.getStages().clear();

        // 3. Lấy DTO và bắt buộc phải có ít nhất 2 bước duyệt
        List<ApprovalStageDTO> dtoStages = approvalWorkflowDTO.getStages();
        if (dtoStages == null || dtoStages.size() < 2) {
            throw new RuntimeException("Quy trình phải có ít nhất 2 người duyệt.");
        }

        // 4. Kiểm tra duplicate approver và build lại các bước từ DTO
        Set<Long> approverIds = new HashSet<>();
        for (ApprovalStageDTO stageDTO : dtoStages) {
            Long approverId = stageDTO.getApproverId();
            if (approverId == null) {
                throw new RuntimeException("Stage " + stageDTO.getStageOrder() + ": approverId không được để trống.");
            }
            if (!approverIds.add(approverId)) {
                throw new RuntimeException("Trùng người duyệt");
            }
            User approver = userRepository.findById(approverId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người duyệt"));
            ApprovalStage stage = ApprovalStage.builder()
                    .stageOrder(stageDTO.getStageOrder())
                    .approver(approver)
                    .status(ApprovalStatus.NOT_STARTED)
                    .approvalWorkflow(workflow)
                    .build();
            workflow.getStages().add(stage);
        }

        // 5. Kiểm tra xem bước cuối do DTO cung cấp đã là DIRECTOR chưa
        ApprovalStage lastStage = workflow.getStages().stream()
                .max(Comparator.comparingInt(ApprovalStage::getStageOrder))
                .get();
        String lastRoleName = lastStage.getApprover().getRole().getRoleName();
        // So sánh đúng String với enum name
        if (!lastRoleName.equalsIgnoreCase(Role.DIRECTOR)) {
            // 5a. Tự động tìm một user có role DIRECTOR
            User director = userRepository.findAll().stream()
                    .filter(u -> u.getRole() != null
                            && Role.DIRECTOR.equalsIgnoreCase(u.getRole().getRoleName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người duyệt có vai trò giám đốc"));
            // 5b. Tạo bước mới với stageOrder = maxOrder + 1
            int nextOrder = lastStage.getStageOrder() + 1;
            ApprovalStage directorStage = ApprovalStage.builder()
                    .stageOrder(nextOrder)
                    .approver(director)
                    .status(ApprovalStatus.NOT_STARTED)
                    .approvalWorkflow(workflow)
                    .build();
            workflow.getStages().add(directorStage);
        }

        // 6. Cập nhật lại customStagesCount và lưu workflow
        workflow.setCustomStagesCount(workflow.getStages().size());
        approvalWorkflowRepository.save(workflow);
    }

    @Override
    @Transactional
    public void deleteWorkflow(Long id) throws DataNotFoundException{
        // check if workflow exists
        ApprovalWorkflow workflow = approvalWorkflowRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.WORKFLOW_NOT_FOUND));
        // delete workflow
        approvalWorkflowRepository.delete(workflow);
    }

    @Override
    @Transactional
    public ApprovalWorkflowResponse getWorkflowById(Long id) throws DataNotFoundException {
        // get workflow by id
        ApprovalWorkflow workflow = approvalWorkflowRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.WORKFLOW_NOT_FOUND));
        // return workflow
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
                                .build())
                        .toList())
                .build();
    }

    private String translateContractStatusToVietnamese(String status) {
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
            case "PENDING":
                return "Chưa ký";
            case "REJECTED":
                return "Bị từ chối";
            case "FIXED":
                return "Đã chỉnh sửa";
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
            case "ENDED":
                return "Kết thúc";
            case "DELETED":
                return "Đã xóa";
            default:
                return status; // Trả về giá trị gốc nếu không có bản dịch
        }
    }

    private void logAuditTrail(Contract contract, String action, String fieldName, String oldValue, String newValue, String changedBy) {
        String oldStatusVi = translateContractStatusToVietnamese(oldValue);
        String newStatusVi = translateContractStatusToVietnamese(newValue);

        AuditTrail auditTrail = AuditTrail.builder()
                .contract(contract)
                .entityName("Contract")
                .entityId(contract.getId())
                .action(action)
                .fieldName(fieldName)
                .oldValue(oldStatusVi)
                .newValue(newStatusVi)
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .changeSummary(String.format("Đã cập nhật trạng thái hợp đồng từ '%s' sang '%s'", oldStatusVi, newStatusVi)) // Tóm tắt thay đổi
                .build();
        auditTrailRepository.save(auditTrail);
    }

    @Override
    @Transactional
    public void assignWorkflowToContract(Long contractId, Long workflowId) throws DataNotFoundException {
        // Tìm hợp đồng cần gán workflow
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new DataNotFoundException("Khong tìm thấy hợp đồng với id " + contractId));

        LocalDateTime now = LocalDateTime.now();
        // CHỈ CHO ÁP DỤNG KHI HÔM NAY ≤ ngày hiệu lực
        if (contract.getSigningDate().plusDays(1).isBefore(now)) {
            throw new DataNotFoundException(
                    "Không thể gán quy trình duyệt vì đã quá hạn ký:  "
                            + contract.getSigningDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            );
        }
        // Tìm workflow gốc theo workflowId
        ApprovalWorkflow originalWorkflow = approvalWorkflowRepository.findById(workflowId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.WORKFLOW_NOT_FOUND));

        ApprovalWorkflow workflowToAssign = originalWorkflow;

        // Nếu workflow đã được gán cho một hợp đồng khác, thực hiện clone
        if (originalWorkflow.getContract() != null || originalWorkflow.getAddendum() != null) {
            // Clone thông tin cơ bản của workflow
            ApprovalWorkflow clonedWorkflow = ApprovalWorkflow.builder()
                    .name(originalWorkflow.getName())
                    .customStagesCount(originalWorkflow.getCustomStagesCount())
                    .createdAt(LocalDateTime.now())
                    .contractType(originalWorkflow.getContractType())
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

        // Gán workflow (mới hoặc gốc nếu chưa gán) cho hợp đồng
        String oldStatus = contract.getStatus().name();
        contract.setApprovalWorkflow(workflowToAssign);
        contract.setStatus(ContractStatus.APPROVAL_PENDING);
        contractRepository.save(contract);

        String changedBy = SecurityContextHolder.getContext().getAuthentication().getName();
        logAuditTrail(contract, "UPDATE", "status", oldStatus, ContractStatus.APPROVAL_PENDING.name(), changedBy);

        // Lấy stage có stageOrder nhỏ nhất để gửi thông báo
        workflowToAssign.getStages().stream()
                .min(Comparator.comparingInt(ApprovalStage::getStageOrder))
                .ifPresent(firstStage -> {
                    Map<String, Object> payload = new HashMap<>();
                    String notificationMessage = "Bạn có hợp đồng cần phê duyệt đợt "
                            + firstStage.getStageOrder() + ": Hợp đồng " + contract.getTitle();
                    payload.put("message", notificationMessage);
                    payload.put("contractId", contractId);
                    User firstApprover = firstStage.getApprover();
                    mailService.sendEmailReminder(contract, firstApprover, firstStage);
                    notificationService.saveNotification(firstApprover, notificationMessage, contract);
                    messagingTemplate.convertAndSendToUser(firstApprover.getFullName(), "/queue/notifications", payload);
                    // Đặt hạn duyệt cho bước này (2 ngày kể từ bây giờ)
//                    firstStage.setStartDate(LocalDateTime.now());
//                    firstStage.setDueDate(LocalDateTime.now().plusDays(appConfigService.getApprovalDeadlineValue()));
                    firstStage.setStatus(ApprovalStatus.APPROVING);
                    approvalStageRepository.save(firstStage);
                });
    }

    @Override
    @Transactional
    public void approvedStage(Long contractId, Long stageId) throws DataNotFoundException {
        // Lấy hợp đồng theo contractId
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy hợp đồng"));

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

        // Kiểm tra: nếu người duyệt đã xử lý (approve hoặc reject) ở bất kỳ bước nào của hợp đồng, từ chối thao tác tiếp
        boolean alreadyProcessed = contract.getApprovalWorkflow().getStages().stream()
                .filter(s -> s.getApprover().getId().equals(currentUser.getId()))
                .anyMatch(s -> s.getStatus() == ApprovalStatus.APPROVED || s.getStatus() == ApprovalStatus.REJECTED);
        if (alreadyProcessed) {
            throw new RuntimeException("Bạn đã xử lý hợp đồng này rồi.");
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
//                nextStage.setStartDate(LocalDateTime.now());
//                nextStage.setDueDate(LocalDateTime.now().plusDays(appConfigService.getApprovalDeadlineValue()));
                nextStage.setStatus(ApprovalStatus.APPROVING);
                approvalStageRepository.save(nextStage);
                User nextApprover = nextStage.getApprover();

                // Tạo payload thông báo
                Map<String, Object> payload = new HashMap<>();
                String notificationMessage = "Bạn có hợp đồng cần phê duyệt đợt " + nextStage.getStageOrder() +
                        ": Hợp đồng " + contract.getTitle();
                payload.put("message", notificationMessage);
                payload.put("contractId", contractId);

                // Gửi thông báo cho người duyệt tiếp theo
                mailService.sendEmailReminder(contract, nextApprover, nextStage);
                notificationService.saveNotification(nextApprover, notificationMessage, contract);
                messagingTemplate.convertAndSendToUser(nextApprover.getFullName(), "/queue/notifications", payload);
            } else {
                // Nếu không còn bước tiếp theo thì cập nhật trạng thái hợp đồng
                String oldStatus = contract.getStatus().name();
                contract.setStatus(ContractStatus.APPROVED);
                contractRepository.save(contract);

                User finalApprover = stage.getApprover();

                // Tạo payload thông báo
                Map<String, Object> payload = new HashMap<>();
                String notificationMessage = "Hợp đồng " + contract.getTitle() + " đã được phê duyệt xong, hãy bắt đầu ký";
                String notificationMessageForStaff = "Hợp đồng " + contract.getTitle() + " đã được phê duyệt xong, đang chờ ký";
                payload.put("message", notificationMessage);
                payload.put("contractId", contractId);

                // Gửi thông báo cho người duyệt tiếp theo
                mailService.sendEmailApprovalSuccessForContract(contract, finalApprover);
                notificationService.saveNotification(finalApprover, notificationMessage, contract);
                messagingTemplate.convertAndSendToUser(finalApprover.getFullName(), "/queue/notifications", payload);

                notificationService.saveNotification(contract.getUser(), notificationMessageForStaff, contract);
                messagingTemplate.convertAndSendToUser(contract.getUser().getFullName(), "/queue/notifications", payload);


                // Ghi audit trail
                String changedBy = SecurityContextHolder.getContext().getAuthentication().getName();
                logAuditTrail(contract, "UPDATE", "status", oldStatus, ContractStatus.APPROVED.name(), changedBy);
            }
        }
    }

    @Override
    @Transactional
    public void rejectStage(Long contractId, Long stageId, WorkflowDTO workflowDTO) throws DataNotFoundException {
        // Lấy người dùng hiện tại từ SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        // Lấy hợp đồng theo contractId
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy hợp đồng"));
        ApprovalWorkflow workflow = contract.getApprovalWorkflow();
        workflow.setContractVersion(contract.getVersion());
        approvalWorkflowRepository.save(workflow);
        // Tìm ApprovalStage theo stageId
        ApprovalStage stage = approvalStageRepository.findById(stageId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy giai đoạn phê duyệt"));

        // Kiểm tra: chỉ cho phép người được giao duyệt thao tác
        if (!stage.getApprover().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền từ chối bước này.");
        }

        // Kiểm tra: nếu người duyệt đã xử lý (approve hoặc reject) ở bất kỳ bước nào của hợp đồng, từ chối thao tác tiếp
        boolean alreadyProcessed = contract.getApprovalWorkflow().getStages().stream()
                .filter(s -> s.getApprover().getId().equals(currentUser.getId()))
                .anyMatch(s -> s.getStatus() == ApprovalStatus.APPROVED || s.getStatus() == ApprovalStatus.REJECTED);
        if (alreadyProcessed) {
            throw new RuntimeException("Bạn đã xử lý hợp đồng này rồi.");
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

        // Cập nhật trạng thái hợp đồng thành REJECTED
        String oldStatus = contract.getStatus().name();
        contract.setStatus(ContractStatus.REJECTED);
        contractRepository.save(contract);

        // Ghi audit trail
        String changedBy = currentUser.getUsername();
        logAuditTrail(contract, "REJECT", "status", oldStatus, ContractStatus.REJECTED.name(), changedBy);

        // Gửi thông báo cho người tạo hợp đồng để yêu cầu chỉnh sửa
        Map<String, Object> payload = new HashMap<>();
        String notificationMessage = "Bạn có hợp đồng bị từ chối phê duyệt: Hợp đồng " + contract.getTitle();
        payload.put("message", notificationMessage);
        payload.put("contractId", contractId);
        mailService.sendUpdateContractReminder(contract, contract.getUser());
        notificationService.saveNotification(contract.getUser(), notificationMessage, contract);
        messagingTemplate.convertAndSendToUser(contract.getUser().getFullName(), "/queue/notifications", payload);
    }

    @Override
    @Transactional
    public ApprovalWorkflowResponse getWorkflowByContractId(Long contractId) throws DataNotFoundException {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy hợp đồng"));

        ApprovalWorkflow workflow = contract.getApprovalWorkflow();
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
                .reSubmitVersion(workflow.getContractVersion())
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
    public List<ApprovalWorkflowResponse> getWorkflowByContractTypeId(Long contractTypeId) throws DataNotFoundException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        // Tìm ApprovalWorkflow theo contractTypeId (bạn cần định nghĩa phương thức này trong IApprovalWorkflowRepository)
        // 1) Lấy toàn bộ workflows, sắp xếp theo createdAt desc
        List<ApprovalWorkflow> allWorkflows = approvalWorkflowRepository
                .findByContractType_IdAndUser_IdOrderByCreatedAtDesc(
                        contractTypeId, currentUser.getId());

        // 2) Lọc bỏ những workflow có bất kỳ approver nào inactive
        List<ApprovalWorkflow> validWorkflows = allWorkflows.stream()
                .filter(wf -> wf.getStages().stream()
                        .map(ApprovalStage::getApprover)
                        .allMatch(User::isActive)
                )
                .limit(3)
                .toList();

        // Chuyển đổi ApprovalWorkflow thành ApprovalWorkflowResponse
        return validWorkflows.stream()
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
    public List<CommentResponse> getApprovalStageCommentDetailsByContractId(Long contractId) throws DataNotFoundException {
        // Tìm hợp đồng theo contractId
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy hợp đồng"));

        // Lấy quy trình phê duyệt của hợp đồng
        ApprovalWorkflow workflow = contract.getApprovalWorkflow();
        if (workflow == null) {
            throw new DataNotFoundException("Không tìm thấy quy trình phê duyệt");
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
    public Page<GetContractForApproverResponse> getContractsForApprover(Long approverId, String keyword, Long contractTypeId, ContractStatus status, int page, int size) {
        // Cấu hình phân trang
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Lấy tất cả các hợp đồng đang ở trạng thái APPROVAL_PENDING và là phiên bản mới nhất
        List<Contract> pendingContracts = contractRepository.findLatestByStatusIn(List.of(
                ContractStatus.APPROVAL_PENDING,
                ContractStatus.APPROVED,
                ContractStatus.ACTIVE,
                ContractStatus.SIGNED,
                ContractStatus.REJECTED,
                ContractStatus.PENDING,
                ContractStatus.LIQUIDATED,
                ContractStatus.ENDED,
                ContractStatus.CANCELLED));

        // Lọc các hợp đồng theo approverId, keyword và contractTypeId
        List<Contract> filteredContracts = pendingContracts.stream()
                .filter(contract -> {
                    ApprovalWorkflow workflow = contract.getApprovalWorkflow();
                    if (workflow == null || workflow.getStages().isEmpty()) {
                        return false;
                    }

                    // Lọc các hợp đồng có trạng thái phù hợp
                    // Bao gồm hợp đồng có trạng thái chưa kết thúc (trạng thái khác ENDED)
                    // và hợp đồng có quy trình duyệt mà người duyệt có nhiệm vụ ở bước tiếp theo
                    return contract.getStatus() != ContractStatus.ENDED
                            && workflow.getStages().stream()
                            .anyMatch(stage -> {
                                // Kiểm tra xem người duyệt có nhiệm vụ ở bước này hay không
                                // và trạng thái của bước duyệt phải là NOT_STARTED, REJECTED, hoặc APPROVING
                                if (stage.getApprover().getId().equals(approverId)) {
                                    // Kiểm tra xem người duyệt có thể duyệt ở bước này
                                    return stage.getStatus() == ApprovalStatus.APPROVING
                                            || stage.getStatus() == ApprovalStatus.REJECTED
                                            || stage.getStatus() == ApprovalStatus.APPROVED;
                                }
                                return false;
                            });
                })
                .filter(contract -> {
                    // Tìm kiếm theo từ khóa trong tiêu đề hợp đồng hoặc số hợp đồng
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        return contract.getTitle().toLowerCase().contains(keyword.toLowerCase())
                                || contract.getContractNumber().toLowerCase().contains(keyword.toLowerCase());
                    }
                    return true;
                })
                .filter(contract -> {
                    // Lọc theo loại hợp đồng (nếu có)
                    if (contractTypeId != null) {
                        return contract.getContractType() != null
                                && contract.getContractType().getId().equals(contractTypeId);
                    }
                    return true;
                })
                .filter(contract -> {
                    // nếu statusFilter null thì không lọc, ngược lại chỉ giữ contract có status khớp
                    return status == null
                            || contract.getStatus() == status;
                })
                .collect(Collectors.toList());

        // Lấy phân trang từ danh sách đã lọc
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredContracts.size());
        Page<GetContractForApproverResponse> pageResponse = new PageImpl<>(
                filteredContracts.subList(start, end).stream()
                        .map(this::mapContractToContractResponse)
                        .collect(Collectors.toList()),
                pageable, filteredContracts.size()
        );

        return pageResponse;
    }

    @Override
    @Transactional
    public void resubmitContractForApproval(Long contractId) throws DataNotFoundException {
        // Tìm hợp đồng theo contractId
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy hợp đồng"));

        // Lấy workflow của hợp đồng
        ApprovalWorkflow workflow = contract.getApprovalWorkflow();
        if (workflow == null || workflow.getStages().isEmpty()) {
            throw new DataNotFoundException("Không tìm thấy quy trình phê duyệt cho hợp đồng");
        }

        workflow.getStages().forEach(stage -> {
            stage.setStatus(ApprovalStatus.NOT_STARTED);
            stage.setApprovedAt(null);
            stage.setComment(null);
            approvalStageRepository.save(stage);
        });

        String oldStatus = contract.getStatus().name();
        contract.setStatus(ContractStatus.APPROVAL_PENDING);
        contractRepository.save(contract);

        // Tìm bước duyệt đầu tiên (stage có stageOrder nhỏ nhất)
        Optional<ApprovalStage> firstStageOpt = workflow.getStages().stream()
                .min(Comparator.comparingInt(ApprovalStage::getStageOrder));

        String changedBy = SecurityContextHolder.getContext().getAuthentication().getName();
        logAuditTrail(contract, "RESUBMIT", "status", oldStatus, ContractStatus.APPROVAL_PENDING.name(), changedBy);

        if (firstStageOpt.isPresent()) {
            ApprovalStage firstStage = firstStageOpt.get();
            // Đặt hạn duyệt cho bước đầu tiên: 2 ngày kể từ thời điểm resubmit
//            firstStage.setStartDate(LocalDateTime.now());
//            firstStage.setDueDate(LocalDateTime.now().plusDays(appConfigService.getApprovalDeadlineValue()));
            firstStage.setStatus(ApprovalStatus.APPROVING);
            approvalStageRepository.save(firstStage);
            User firstApprover = firstStage.getApprover();

            // Tạo payload thông báo cho người duyệt ở bước đầu tiên
            Map<String, Object> payload = new HashMap<>();
            String notificationMessage = "Hợp đồng '" + contract.getTitle() + "' đã được chỉnh sửa và nộp lại để phê duyệt. Bạn có hợp đồng cần phê duyệt đợt "
                    + firstStage.getStageOrder();
            payload.put("message", notificationMessage);
            payload.put("contractId", contractId);

            // Gửi email nhắc nhở nếu cần
            mailService.sendEmailReminder(contract, firstApprover, firstStage);
            // Lưu thông báo vào hệ thống thông báo
            notificationService.saveNotification(firstApprover, notificationMessage, contract);
            // Gửi thông báo qua WebSocket
            messagingTemplate.convertAndSendToUser(firstApprover.getFullName(), "/queue/notifications", payload);
        }
    }

    @Override
    @Transactional
    public Page<GetContractForApproverResponse> getContractsForManager(Long managerId, String keyword, Long contractTypeId, int page, int size) {
        // Cấu hình phân trang
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Lấy tất cả các hợp đồng đang ở trạng thái APPROVAL_PENDING
        List<Contract> pendingContracts = contractRepository.findByStatusAndIsLatestVersion(ContractStatus.APPROVAL_PENDING, true);

        // Lọc các hợp đồng theo managerId, keyword và contractTypeId
        List<Contract> filteredContracts = pendingContracts.stream()
                .filter(contract -> {
                    ApprovalWorkflow workflow = contract.getApprovalWorkflow();
                    if (workflow == null || workflow.getStages().isEmpty()) {
                        return false;
                    }

                    // Xác định "bước duyệt hiện tại" dựa trên stage có trạng thái NOT_STARTED, REJECTED hoặc APPROVING và có stageOrder nhỏ nhất
                    Optional<ApprovalStage> currentStageOpt = workflow.getStages().stream()
                            .filter(stage -> stage.getStatus() == ApprovalStatus.NOT_STARTED
                                    || stage.getStatus() == ApprovalStatus.REJECTED
                                    || stage.getStatus() == ApprovalStatus.APPROVING)
                            .min(Comparator.comparingInt(ApprovalStage::getStageOrder));

                    return currentStageOpt.isPresent() &&
                            currentStageOpt.get().getApprover().getId().equals(managerId);
                })
                .filter(contract -> {
                    // Tìm kiếm theo từ khóa trong tiêu đề hợp đồng hoặc số hợp đồng
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        return contract.getTitle().toLowerCase().contains(keyword.toLowerCase())
                                || contract.getContractNumber().toLowerCase().contains(keyword.toLowerCase());
                    }
                    return true;
                })
                .filter(contract -> {
                    // Lọc theo loại hợp đồng (nếu có)
                    if (contractTypeId != null) {
                        return contract.getContractType() != null
                                && contract.getContractType().getId().equals(contractTypeId);
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // Lấy phân trang từ danh sách đã lọc
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredContracts.size());
        Page<GetContractForApproverResponse> pageResponse = new PageImpl<>(
                filteredContracts.subList(start, end).stream()
                        .map(this::mapContractToContractResponse)
                        .collect(Collectors.toList()),
                pageable, filteredContracts.size()
        );

        return pageResponse;
    }

    @Override
    public Map<String, Integer> getApprovalStats() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        Map<String, Integer> stats = new HashMap<>();

        if (currentUser.isManager()) {
            // Quản lý: lấy số lượng hợp đồng và phụ lục đang chờ phê duyệt mà người đó được giao
            long contractsPendingApprovalForManager = contractRepository.countByStatusAndIsLatestVersionAndApprovalWorkflow_Stages_Approver_IdAndApprovalWorkflow_Stages_Status(
                    ContractStatus.APPROVAL_PENDING, true, currentUser.getId(), ApprovalStatus.APPROVING);

            long addendaPendingApprovalForManager = addendumRepository.countByStatusAndContract_IsLatestVersionAndApprovalWorkflow_Stages_Approver_IdAndApprovalWorkflow_Stages_Status(
                    AddendumStatus.APPROVAL_PENDING, true, currentUser.getId(), ApprovalStatus.APPROVING);

            // Quản lý: lấy số lượng hợp đồng mà người đó đã từ chối phê duyệt
            long contractsRejectedByManager = contractRepository.countByStatusAndIsLatestVersionAndApprovalWorkflow_Stages_Approver_IdAndApprovalWorkflow_Stages_Status(
                    ContractStatus.REJECTED, true, currentUser.getId(), ApprovalStatus.REJECTED);

            long addendaRejectedByManager = addendumRepository.countByStatusAndContract_IsLatestVersionAndApprovalWorkflow_Stages_Approver_IdAndApprovalWorkflow_Stages_Status(
                    AddendumStatus.REJECTED, true, currentUser.getId(), ApprovalStatus.REJECTED);
            stats.put("contractsPendingApprovalForManager", (int) contractsPendingApprovalForManager);
            stats.put("addendaPendingApprovalForManager", (int) addendaPendingApprovalForManager);
            stats.put("contractsRejectedByManager", (int) contractsRejectedByManager);
            stats.put("addendaRejectedByManager", (int) addendaRejectedByManager);

        } else if (currentUser.getRole().getRoleName().equals("DIRECTOR")) {
            // CEO: lấy số lượng hợp đồng và phụ lục mà người đó được giao
            long contractsPendingApprovalForDirector = contractRepository.countByStatusAndIsLatestVersionAndApprovalWorkflow_Stages_Approver_IdAndApprovalWorkflow_Stages_Status(
                    ContractStatus.APPROVAL_PENDING, true, currentUser.getId(), ApprovalStatus.APPROVING);

            long addendaPendingApprovalForDirector = addendumRepository.countByStatusAndContract_IsLatestVersionAndApprovalWorkflow_Stages_Approver_IdAndApprovalWorkflow_Stages_Status(
                    AddendumStatus.APPROVAL_PENDING, true, currentUser.getId(), ApprovalStatus.APPROVING);

            // CEO: lấy số lượng hợp đồng và phụ lục đã phê duyệt và là phiên bản mới nhất
            long contractsApprovedForDirector = contractRepository.countByStatusAndIsLatestVersion(
                    ContractStatus.APPROVED, true);

            long addendaApprovedForDirector = addendumRepository.countByStatusAndContract_IsLatestVersion(
                    AddendumStatus.APPROVED, true);

            stats.put("contractsPendingApprovalForDirector", (int) contractsPendingApprovalForDirector);
            stats.put("addendaPendingApprovalForDirector", (int) addendaPendingApprovalForDirector);
            stats.put("contractsSignPendingForDirector", (int) contractsApprovedForDirector);
            stats.put("addendaSignPendingForDirector", (int) addendaApprovedForDirector);

        } else {
            // Nhân viên: lấy số lượng hợp đồng/phụ lục mà nhân viên đã tạo
            long contractsPendingApproval = contractRepository.countByUser_IdAndStatusAndIsLatestVersion(currentUser.getId(), ContractStatus.APPROVAL_PENDING, true);

            // Số lượng hợp đồng mà nhân viên đã tạo, bị từ chối
            long contractsRejected = contractRepository.countByUser_IdAndStatusAndIsLatestVersion(currentUser.getId(), ContractStatus.REJECTED, true);

            // Số lượng phụ lục mà nhân viên đã tạo, đang chờ phê duyệt
            long addendaPendingApproval = addendumRepository.countByContract_User_IdAndStatusAndContract_IsLatestVersion(currentUser.getId(), AddendumStatus.APPROVAL_PENDING, true);
            long addendaRejected = addendumRepository.countByContract_User_IdAndStatusAndContract_IsLatestVersion(currentUser.getId(), AddendumStatus.REJECTED, true);

            // Nhân viên được giao duyệt hợp đồng/phụ lục
            long contractsAssignedToApprove = contractRepository.countByStatusAndIsLatestVersionAndApprovalWorkflow_Stages_Approver_IdAndApprovalWorkflow_Stages_Status(
                    ContractStatus.APPROVAL_PENDING, true, currentUser.getId(), ApprovalStatus.APPROVING);
            long addendaAssignedToApprove = addendumRepository.countByStatusAndContract_IsLatestVersionAndApprovalWorkflow_Stages_Approver_IdAndApprovalWorkflow_Stages_Status(
                    AddendumStatus.APPROVAL_PENDING, true, currentUser.getId(), ApprovalStatus.APPROVING);

            stats.put("contractsPendingApproval", (int) contractsPendingApproval);
            stats.put("contractsRejected", (int) contractsRejected);
            stats.put("addendaPendingApproval", (int) addendaPendingApproval);
            stats.put("addendaRejected", (int) addendaRejected);
            stats.put("contractsAssignedToApprove", (int) contractsAssignedToApprove);
            stats.put("addendaAssignedToApprove", (int) addendaAssignedToApprove);
        }

        return stats;
    }

    // Hàm chuyển đổi Contract entity sang ContractResponse DTO
    private GetContractForApproverResponse mapContractToContractResponse(Contract contract) {
        return GetContractForApproverResponse.builder()
                .id(contract.getId())
                .title(contract.getTitle())
                .user(mapUserToUserContractResponse(contract.getUser()))
                .partnerB(contract.getPartner())
                .contractNumber(contract.getContractNumber())
                .status(contract.getStatus())
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt())
                .signingDate(contract.getSigningDate())
                .contractLocation(contract.getContractLocation())
                .amount(contract.getAmount())
                .contractType(contract.getContractType())
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
                .version(contract.getVersion())
                .signedFilePath(contract.getSignedFilePath())
                // Các danh sách term, additional config, payment schedules có thể được map theo logic riêng nếu cần.
                .legalBasisTerms(new ArrayList<>())
                .generalTerms(new ArrayList<>())
                .otherTerms(new ArrayList<>())
                .additionalTerms(new ArrayList<>())
                .additionalConfig(new HashMap<>())
                .paymentSchedules(new ArrayList<>())
                .build();
    }

    // Hàm chuyển đổi User entity sang UserContractResponse DTO (giả sử DTO này có các field: id, username, fullName, email,...)
    private UserContractResponse mapUserToUserContractResponse(User user) {
        return UserContractResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .build();
    }

}
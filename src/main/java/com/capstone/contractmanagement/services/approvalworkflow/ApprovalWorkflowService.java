package com.capstone.contractmanagement.services.approvalworkflow;

import com.capstone.contractmanagement.dtos.DataMailDTO;
import com.capstone.contractmanagement.dtos.approvalworkflow.ApprovalWorkflowDTO;
import com.capstone.contractmanagement.dtos.approvalworkflow.WorkflowDTO;
import com.capstone.contractmanagement.entities.AuditTrail;
import com.capstone.contractmanagement.entities.approval_workflow.ApprovalStage;
import com.capstone.contractmanagement.entities.approval_workflow.ApprovalWorkflow;
import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.entities.contract.ContractType;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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


    @Override
    @Transactional
    public ApprovalWorkflowResponse createWorkflow(ApprovalWorkflowDTO approvalWorkflowDTO) {
        // Tạo đối tượng workflow mà không gán contract qua builder
//        ContractType contractType = contractTypeRepository.findById(approvalWorkflowDTO.getContractTypeId())
//                .orElseThrow(() -> new RuntimeException("Contract type not found with id: " + approvalWorkflowDTO.getContractTypeId()));
        ApprovalWorkflow workflow = ApprovalWorkflow.builder()
                .name(approvalWorkflowDTO.getName())
                //.contractType(contractType)
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
        }

        // Cập nhật số lượng stage tùy chỉnh dựa trên số stage đã thêm
        workflow.setCustomStagesCount(workflow.getStages().size());
        // Lưu workflow
        approvalWorkflowRepository.save(workflow);

        // Nếu DTO có chứa contractId, gán workflow vừa tạo cho Contract tương ứng
        if (approvalWorkflowDTO.getContractId() != null) {
            Contract contract = contractRepository.findById(approvalWorkflowDTO.getContractId())
                    .orElseThrow(() -> new RuntimeException("Hợp đồng không tìm thấy với id " + approvalWorkflowDTO.getContractId()));
            contract.setApprovalWorkflow(workflow);
            contractRepository.save(contract);
        }

        if (approvalWorkflowDTO.getContractTypeId() != null) {
            ContractType contractTypes = contractTypeRepository.findById(approvalWorkflowDTO.getContractTypeId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy loại hợp đồng với id " + approvalWorkflowDTO.getContractTypeId()));
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
        // Kiểm tra xem workflow có tồn tại hay không
        ApprovalWorkflow workflow = approvalWorkflowRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.WORKFLOW_NOT_FOUND));

        // Cập nhật thông tin chung của workflow
        workflow.setName(approvalWorkflowDTO.getName());

        // Nếu muốn thay thế hoàn toàn các stage cũ, xóa sạch collection hiện có
        workflow.getStages().clear();

        // Nếu có danh sách stage trong DTO, kiểm tra duplicate approver
        if (approvalWorkflowDTO.getStages() != null) {
            // Sử dụng Set để kiểm tra duplicate
            Set<Long> approverIds = new HashSet<>();
            for (var stageDTO : approvalWorkflowDTO.getStages()) {
                if (stageDTO.getApproverId() == null) {
                    throw new RuntimeException("Trùng người duyệt tại stage: " + stageDTO.getStageOrder());
                }
                if (!approverIds.add(stageDTO.getApproverId())) {
                    throw new RuntimeException("Trùng ID người duyệt: " + stageDTO.getApproverId());
                }
            }
            // Thêm các stage mới sau khi xác nhận không có duplicate
            approvalWorkflowDTO.getStages().forEach(stageDTO -> {
                User approver = userRepository.findById(stageDTO.getApproverId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy người duyệt với id " + stageDTO.getApproverId()));
                ApprovalStage stage = ApprovalStage.builder()
                        .stageOrder(stageDTO.getStageOrder())
                        .approver(approver)
                        .status(ApprovalStatus.NOT_STARTED)
                        .approvalWorkflow(workflow)
                        .build();
                workflow.getStages().add(stage);
            });
        }

        // Cập nhật lại customStagesCount dựa trên số lượng stage hiện có
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
                .entityName("Contract") // Tên thực thể là "Contract"
                .entityId(contract.getId()) // ID của hợp đồng
                .action(action) // Hành động: "Status updated"
                .fieldName(fieldName) // Trường thay đổi: "status"
                .oldValue(oldStatusVi) // Giá trị cũ (đã dịch sang tiếng Việt)
                .newValue(newStatusVi) // Giá trị mới (đã dịch sang tiếng Việt)
                .changedBy(changedBy) // Người thực hiện thay đổi
                .changedAt(LocalDateTime.now()) // Thời điểm thay đổi
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

        // Tìm workflow gốc theo workflowId
        ApprovalWorkflow originalWorkflow = approvalWorkflowRepository.findById(workflowId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.WORKFLOW_NOT_FOUND));

        ApprovalWorkflow workflowToAssign = originalWorkflow;

        // Nếu workflow đã được gán cho một hợp đồng khác, thực hiện clone
        if (originalWorkflow.getContract() != null) {
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
                    firstStage.setStartDate(LocalDateTime.now());
                    firstStage.setDueDate(LocalDateTime.now().plusDays(appConfigService.getApprovalDeadlineValue()));
                    firstStage.setStatus(ApprovalStatus.APPROVING);
                    approvalStageRepository.save(firstStage);
                });
    }

    @Override
    @Transactional
    public void approvedStage(Long contractId, Long stageId) throws DataNotFoundException {
        // Lấy hợp đồng theo contractId
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new DataNotFoundException("Contract not found"));

        // Tìm ApprovalStage theo stageId
        ApprovalStage stage = approvalStageRepository.findById(stageId)
                .orElseThrow(() -> new DataNotFoundException("Approval stage not found with id " + stageId));

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
                nextStage.setStartDate(LocalDateTime.now());
                nextStage.setDueDate(LocalDateTime.now().plusDays(appConfigService.getApprovalDeadlineValue()));
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
                .orElseThrow(() -> new DataNotFoundException("Contract not found"));

        // Tìm ApprovalStage theo stageId
        ApprovalStage stage = approvalStageRepository.findById(stageId)
                .orElseThrow(() -> new DataNotFoundException("Approval stage not found with id " + stageId));

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
        logAuditTrail(contract, "UPDATE", "status", oldStatus, ContractStatus.REJECTED.name(), changedBy);

        // Gửi thông báo cho người tạo hợp đồng để yêu cầu chỉnh sửa
        Map<String, Object> payload = new HashMap<>();
        String notificationMessage = "Bạn có hợp đồng cần chỉnh sửa: Hợp đồng " + contract.getTitle();
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
                .orElseThrow(() -> new DataNotFoundException("Contract not found"));
        ApprovalWorkflow workflow = contract.getApprovalWorkflow();
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
                                .startDate(stage.getStartDate())
                                .endDate(stage.getDueDate())
                                .approvedAt(stage.getApprovedAt())
                                .status(stage.getStatus())
                                .comment(stage.getComment())
                                .build())
                        .toList())
                .build();
    }

    @Override
    @Transactional
    public List<ApprovalWorkflowResponse> getWorkflowByContractTypeId(Long contractTypeId) throws DataNotFoundException {

        // Tìm ApprovalWorkflow theo contractTypeId (bạn cần định nghĩa phương thức này trong IApprovalWorkflowRepository)
        List<ApprovalWorkflow> workflow = approvalWorkflowRepository.findTop3ByContractType_IdOrderByCreatedAtDesc(contractTypeId);

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
    public List<CommentResponse> getApprovalStageCommentDetailsByContractId(Long contractId) throws DataNotFoundException {
        // Tìm hợp đồng theo contractId
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy hợp đồng với id : " + contractId));

        // Lấy quy trình phê duyệt của hợp đồng
        ApprovalWorkflow workflow = contract.getApprovalWorkflow();
        if (workflow == null) {
            throw new DataNotFoundException("Không tìm thấy quy trình phê duyệt cho hợp đồng với id : " + contractId);
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
    public List<GetContractForApproverResponse> getContractsForApprover(Long approverId) {
        // Lấy tất cả các hợp đồng đang ở trạng thái APPROVAL_PENDING
        List<Contract> pendingContracts = contractRepository.findByStatus(ContractStatus.APPROVAL_PENDING);

        List<Contract> filteredContracts = pendingContracts.stream()
                .filter(contract -> {
                    ApprovalWorkflow workflow = contract.getApprovalWorkflow();
                    if (workflow == null || workflow.getStages().isEmpty()) {
                        return false;
                    }

                    // Xác định "bước duyệt hiện tại" dựa trên stage có trạng thái NOT_STARTED, REJECTED hoặc APPROVING và có stageOrder nhỏ nhất
                    OptionalInt currentStageOrderOpt = workflow.getStages().stream()
                            .filter(stage -> stage.getStatus() == ApprovalStatus.NOT_STARTED
                                    || stage.getStatus() == ApprovalStatus.REJECTED
                                    || stage.getStatus() == ApprovalStatus.APPROVING)
                            .mapToInt(ApprovalStage::getStageOrder)
                            .min();
                    if (!currentStageOrderOpt.isPresent()) {
                        return false;
                    }
                    int currentStageOrder = currentStageOrderOpt.getAsInt();

                    // Điều kiện mới:
                    // Nếu trong các bước có stageOrder nhỏ hơn hoặc bằng bước hiện tại
                    // tồn tại bước có người duyệt trùng với approverId, thì hiển thị hợp đồng.
                    // Điều này giúp hiển thị hợp đồng cho:
                    // - Người duyệt đang ở bước hiện tại (stageOrder == currentStageOrder)
                    // - Người duyệt đã xử lý ở các bước trước (stageOrder < currentStageOrder)
                    return workflow.getStages().stream()
                            .anyMatch(stage -> stage.getStageOrder() <= currentStageOrder
                                    && stage.getApprover().getId().equals(approverId));
                })
                .collect(Collectors.toList());

        // Chuyển đổi các Contract được lọc sang dạng ContractResponse
        return filteredContracts.stream()
                .map(this::mapContractToContractResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void resubmitContractForApproval(Long contractId) throws DataNotFoundException {
        // Tìm hợp đồng theo contractId
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy hợp đồng với id : " + contractId));

        // Lấy workflow của hợp đồng
        ApprovalWorkflow workflow = contract.getApprovalWorkflow();
        if (workflow == null || workflow.getStages().isEmpty()) {
            throw new DataNotFoundException("Không tìm thấy quy trình phê duyệt cho hợp đồng với id : " + contractId);
        }

        // Reset lại tất cả các bước duyệt: đặt trạng thái về PENDING, xóa approvedAt và comment
        workflow.getStages().forEach(stage -> {
            stage.setStatus(ApprovalStatus.NOT_STARTED);
            stage.setApprovedAt(null);
            stage.setComment(null);
            approvalStageRepository.save(stage);
        });

        // Cập nhật lại trạng thái của hợp đồng về APPROVAL_PENDING (để báo hiệu đang chờ duyệt lại)

        String oldStatus = contract.getStatus().name();
        contract.setStatus(ContractStatus.APPROVAL_PENDING);
        contractRepository.save(contract);

        // Tìm bước duyệt đầu tiên (stage có stageOrder nhỏ nhất)
        Optional<ApprovalStage> firstStageOpt = workflow.getStages().stream()
                .min(Comparator.comparingInt(ApprovalStage::getStageOrder));

        String changedBy = SecurityContextHolder.getContext().getAuthentication().getName();
        logAuditTrail(contract, "UPDATE", "status", oldStatus, ContractStatus.APPROVAL_PENDING.name(), changedBy);

        if (firstStageOpt.isPresent()) {
            ApprovalStage firstStage = firstStageOpt.get();
            // Đặt hạn duyệt cho bước đầu tiên: 2 ngày kể từ thời điểm resubmit
            firstStage.setStartDate(LocalDateTime.now());
            firstStage.setDueDate(LocalDateTime.now().plusDays(appConfigService.getApprovalDeadlineValue()));
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
    public List<GetContractForApproverResponse> getContractsForManager(Long managerId) {

        List<Contract> pendingContracts = contractRepository.findByStatus(ContractStatus.APPROVAL_PENDING);

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
                .collect(Collectors.toList());

        // Chuyá»ƒn Ä‘á»•i cÃ¡c Contract Ä‘Æ°á»£c lá»c sang ContractResponse
        return filteredContracts.stream()
                .map(this::mapContractToContractResponse)
                .collect(Collectors.toList());
    }

    // Hàm chuyển đổi Contract entity sang ContractResponse DTO
    private GetContractForApproverResponse mapContractToContractResponse(Contract contract) {
        return GetContractForApproverResponse.builder()
                .id(contract.getId())
                .title(contract.getTitle())
                .user(mapUserToUserContractResponse(contract.getUser()))
                .partner(contract.getPartner())
                .contractNumber(contract.getContractNumber())
                .status(contract.getStatus())
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt())
                .signingDate(contract.getSigningDate())
                .contractLocation(contract.getContractLocation())
                .amount(contract.getAmount())
                .contractTypeName(contract.getContractType().getName())
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

//    private void sendEmailReminder(Contract contract, User user, ApprovalStage stage) {
//        try {
//            DataMailDTO dataMailDTO = new DataMailDTO();
//            dataMailDTO.setTo(user.getEmail());
//            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.CONTRACT_APPROVAL_NOTIFICATION);
//            Map<String, Object> props = new HashMap<>();
//            props.put("contractTitle", contract.getTitle());
//            props.put("stage", stage.getStageOrder());
//            dataMailDTO.setProps(props); // Set props to dataMailDTO
//            mailService.sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.CONTRACT_APPROVAL_NOTIFICATION);
//        } catch (Exception e) {
//            // Xu ly loi
//            e.printStackTrace();
//        }
//    }

//    private void sendUpdateContractReminder(Contract contract, User user) {
//        try {
//            DataMailDTO dataMailDTO = new DataMailDTO();
//            dataMailDTO.setTo(user.getEmail());
//            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.UPDATE_CONTRACT_REQUEST);
//            Map<String, Object> props = new HashMap<>();
//            props.put("contractTitle", contract.getTitle());
//            dataMailDTO.setProps(props); // Set props to dataMailDTO
//            mailService.sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.UPDATE_CONTRACT_REQUEST);
//        } catch (Exception e) {
//            // Xu ly loi
//            e.printStackTrace();
//        }
//    }

    // Phương thức chạy mỗi phút (hoặc theo tần suất phù hợp)
    @Scheduled(fixedDelay = 60000)
    public void autoAdvanceExpiredStages() {
        LocalDateTime now = LocalDateTime.now();
        // Tìm tất cả các bước duyệt có trạng thái PENDING mà đã quá hạn
        List<ApprovalStage> expiredStages = approvalStageRepository
                .findByStatusAndDueDateBefore(ApprovalStatus.NOT_STARTED, now);

        for (ApprovalStage stage : expiredStages) {
            // Auto "phê duyệt" bước này (hoặc bạn có thể đánh dấu là "expired" nếu muốn)
            stage.setStatus(ApprovalStatus.SKIPPED);
            stage.setApprovedAt(now);
            approvalStageRepository.save(stage);

            // Lấy workflow và hợp đồng liên quan
            ApprovalWorkflow workflow = stage.getApprovalWorkflow();
            Contract contract = workflow.getContract();

            // Tìm bước duyệt tiếp theo
            Optional<ApprovalStage> nextStageOptional = workflow.getStages().stream()
                    .filter(s -> s.getStageOrder() > stage.getStageOrder())
                    .min(Comparator.comparingInt(ApprovalStage::getStageOrder));

            if (nextStageOptional.isPresent()) {
                ApprovalStage nextStage = nextStageOptional.get();
                // Cập nhật hạn duyệt cho bước tiếp theo
                nextStage.setStartDate(now);
                nextStage.setDueDate(now.plusDays(appConfigService.getApprovalDeadlineValue()));
                nextStage.setStatus(ApprovalStatus.APPROVING);
                approvalStageRepository.save(nextStage);
                User nextApprover = nextStage.getApprover();
                // Gửi thông báo cho người duyệt tiếp theo
                Map<String, Object> payload = new HashMap<>();
                String notificationMessage = "Bạn có hợp đồng cần phê duyệt đợt " + nextStage.getStageOrder() +
                        ": Hợp đồng " + contract.getTitle();
                payload.put("message", notificationMessage);
                payload.put("contractId", contract.getId());
                mailService.sendEmailReminder(contract, nextApprover, nextStage);
                messagingTemplate.convertAndSendToUser(nextApprover.getFullName(), "/queue/notifications", payload);
                notificationService.saveNotification(nextApprover, notificationMessage, contract);
            } else {
                // Nếu không có bước tiếp theo, cập nhật hợp đồng thành APPROVED
                contract.setStatus(ContractStatus.APPROVED);
                contractRepository.save(contract);
            }
        }
    }
}

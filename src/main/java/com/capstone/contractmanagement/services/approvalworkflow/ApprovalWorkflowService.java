package com.capstone.contractmanagement.services.approvalworkflow;

import com.capstone.contractmanagement.dtos.DataMailDTO;
import com.capstone.contractmanagement.dtos.approvalworkflow.ApprovalWorkflowDTO;
import com.capstone.contractmanagement.dtos.approvalworkflow.WorkflowDTO;
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
                    throw new RuntimeException("Duplicate approver found with id: " + stageDTO.getApproverId());
                }
            }

            // Tạo và thêm các stage sau khi xác nhận không có duplicate
            approvalWorkflowDTO.getStages().forEach(stageDTO -> {
                User approver = userRepository.findById(stageDTO.getApproverId())
                        .orElseThrow(() -> new RuntimeException("User not found with id " + stageDTO.getApproverId()));
                ApprovalStage stage = ApprovalStage.builder()
                        .stageOrder(stageDTO.getStageOrder())
                        .approver(approver)
                        .status(ApprovalStatus.PENDING)
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
                    .orElseThrow(() -> new RuntimeException("Contract not found with id " + approvalWorkflowDTO.getContractId()));
            contract.setApprovalWorkflow(workflow);
            contractRepository.save(contract);
        }

        if (approvalWorkflowDTO.getContractTypeId() != null) {
            ContractType contractTypes = contractTypeRepository.findById(approvalWorkflowDTO.getContractTypeId())
                    .orElseThrow(() -> new RuntimeException("Contract type not found with id " + approvalWorkflowDTO.getContractTypeId()));
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
                    throw new RuntimeException("Approver id must not be null for stage with stageOrder: " + stageDTO.getStageOrder());
                }
                if (!approverIds.add(stageDTO.getApproverId())) {
                    throw new RuntimeException("Duplicate approver found with id: " + stageDTO.getApproverId());
                }
            }
            // Thêm các stage mới sau khi xác nhận không có duplicate
            approvalWorkflowDTO.getStages().forEach(stageDTO -> {
                User approver = userRepository.findById(stageDTO.getApproverId())
                        .orElseThrow(() -> new RuntimeException("User not found with id " + stageDTO.getApproverId()));
                ApprovalStage stage = ApprovalStage.builder()
                        .stageOrder(stageDTO.getStageOrder())
                        .approver(approver)
                        .status(ApprovalStatus.PENDING)
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
                                .build())
                        .toList())
                .build();
    }

    @Override
    @Transactional
    public void assignWorkflowToContract(Long contractId, Long workflowId) throws DataNotFoundException {
        // Tìm hợp đồng cần gán workflow
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new DataNotFoundException("Contract not found"));

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
                        .status(ApprovalStatus.PENDING)
                        .approvalWorkflow(clonedWorkflow)
                        .build();
                clonedWorkflow.getStages().add(clonedStage);
            });

            // Lưu workflow mới (với các stage tương ứng)
            approvalWorkflowRepository.save(clonedWorkflow);
            workflowToAssign = clonedWorkflow;
        }

        // Gán workflow (mới hoặc gốc nếu chưa gán) cho hợp đồng
        contract.setApprovalWorkflow(workflowToAssign);
        contract.setStatus(ContractStatus.APPROVAL_PENDING);
        contractRepository.save(contract);

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
                    messagingTemplate.convertAndSendToUser(firstApprover.getFullName(), "/queue/notifications", payload);
                    sendEmailReminder(contract, firstApprover, firstStage);
                    notificationService.saveNotification(firstApprover, notificationMessage, contract);
                    // Đặt hạn duyệt cho bước này (2 ngày kể từ bây giờ)
                    firstStage.setDueDate(LocalDateTime.now().plusDays(2));
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

        // Cập nhật trạng thái và thời gian phê duyệt cho bước hiện tại
        stage.setStatus(ApprovalStatus.APPROVED);
        stage.setApprovedAt(LocalDateTime.now());
        approvalStageRepository.save(stage);

        // Nếu trạng thái đã chuyển thành APPROVED
        if (stage.getStatus() == ApprovalStatus.APPROVED) {
            // Lấy workflow hiện tại
            ApprovalWorkflow workflow = stage.getApprovalWorkflow();

            // Tìm bước duyệt tiếp theo (nếu có) có stageOrder lớn hơn bước hiện tại, với thứ tự nhỏ nhất
            Optional<ApprovalStage> nextStageOptional = workflow.getStages().stream()
                    .filter(s -> s.getStageOrder() > stage.getStageOrder())
                    .min(Comparator.comparingInt(ApprovalStage::getStageOrder));

            if (nextStageOptional.isPresent()) {
                ApprovalStage nextStage = nextStageOptional.get();
                // Đặt hạn duyệt cho bước tiếp theo (2 ngày kể từ thời điểm duyệt)
                nextStage.setDueDate(LocalDateTime.now().plusDays(2));
                User nextApprover = nextStage.getApprover();
                // Tạo payload thông báo
                Map<String, Object> payload = new HashMap<>();
                String notificationMessage = "Bạn có hợp đồng cần phê duyệt đợt " + nextStage.getStageOrder() +
                        ": Hợp đồng " + contract.getTitle();
                payload.put("message", notificationMessage);
                payload.put("contractId", contractId);
                // Gửi thông báo qua WebSocket đến người duyệt tiếp theo
                messagingTemplate.convertAndSendToUser(nextApprover.getFullName(), "/queue/notifications", payload);
                sendEmailReminder(contract, nextApprover, nextStage);
                notificationService.saveNotification(nextApprover, notificationMessage, contract);
            } else {
                // Nếu không có bước duyệt tiếp theo => đây là bước duyệt cuối cùng, cập nhật status hợp đồng thành APPROVED
                contract.setStatus(ContractStatus.APPROVED);
                contractRepository.save(contract);
            }
        }
    }

    @Override
    @Transactional
    public void rejectStage(Long contractId, Long stageId, WorkflowDTO workflowDTO) throws DataNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new DataNotFoundException("Contract not found"));
        // Tìm ApprovalStage theo id
        ApprovalStage stage = approvalStageRepository.findById(stageId)
                .orElseThrow(() -> new DataNotFoundException("Approval stage not found with id " + stageId));
        Long workflowId = contract.getApprovalWorkflow().getId();
        // Cập nhật trạng thái mới
        stage.setStatus(ApprovalStatus.REJECTED);
        stage.setApprovedAt(LocalDateTime.now());
        stage.setComment(workflowDTO.getComment());
        approvalStageRepository.save(stage);
        contract.setStatus(ContractStatus.REJECTED);
        contract.setApprovalWorkflow(null);
        contractRepository.save(contract);
        //workflowService.createWorkflow(contract, workflowDTO, currentUser);
        Map<String, Object> payload = new HashMap<>();
        String notificationMessage = "Bạn có hợp đồng cần chỉnh sửa: Hợp đồng " + contract.getTitle();
        payload.put("message", notificationMessage);
        payload.put("contractId", contractId);
        payload.put("workflowId", workflowId);
        // Gửi thông báo đến user, sử dụng username làm định danh user destination
        messagingTemplate.convertAndSendToUser(contract.getUser().getFullName(), "/queue/notifications", payload);
        notificationService.saveNotification(contract.getUser(), notificationMessage, contract);
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
                .orElseThrow(() -> new DataNotFoundException("Contract not found with id: " + contractId));

        // Lấy quy trình phê duyệt của hợp đồng
        ApprovalWorkflow workflow = contract.getApprovalWorkflow();
        if (workflow == null) {
            throw new DataNotFoundException("Approval workflow not found for contract id: " + contractId);
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

        // Lọc hợp đồng để chọn những hợp đồng mà bước duyệt hiện tại (bước có trạng thái PENDING hoặc REJECTED, với stageOrder nhỏ nhất)
        // có người duyệt (approver) trùng với approverId được truyền vào
        List<Contract> filteredContracts = pendingContracts.stream()
                .filter(contract -> {
                    ApprovalWorkflow workflow = contract.getApprovalWorkflow();
                    if (workflow == null || workflow.getStages().isEmpty()) {
                        return false;
                    }
                    // Tìm bước duyệt hiện tại: bước có trạng thái PENDING hoặc REJECTED với stageOrder nhỏ nhất
                    Optional<ApprovalStage> currentStageOpt = workflow.getStages().stream()
                            .filter(stage -> stage.getStatus() == ApprovalStatus.PENDING
                                    || stage.getStatus() == ApprovalStatus.REJECTED)
                            .min(Comparator.comparingInt(ApprovalStage::getStageOrder));
                    return currentStageOpt.isPresent() &&
                            currentStageOpt.get().getApprover().getId().equals(approverId);
                })
                .collect(Collectors.toList());

        // Chuyển đổi các Contract được lọc sang ContractResponse
        return filteredContracts.stream()
                .map(this::mapContractToContractResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void resubmitContractForApproval(Long contractId, Long workflowId) throws DataNotFoundException {
        // Tìm hợp đồng theo contractId
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new DataNotFoundException("Contract not found with id: " + contractId));

        // Lấy lại workflow từ workflowId đã lưu trước đó
        ApprovalWorkflow workflow = approvalWorkflowRepository.findById(workflowId)
                .orElseThrow(() -> new DataNotFoundException("Approval workflow not found with id: " + workflowId));

        // Gán lại workflow cho hợp đồng
        contract.setApprovalWorkflow(workflow);
        contract.setStatus(ContractStatus.APPROVAL_PENDING);
        contractRepository.save(contract);

        // Reset lại trạng thái của tất cả các stage: PENDING, xóa approvedAt và comment
        workflow.getStages().forEach(stage -> {
            stage.setStatus(ApprovalStatus.PENDING);
            stage.setApprovedAt(null);
            stage.setComment(null);
            approvalStageRepository.save(stage);
        });

        // Tìm bước duyệt đầu tiên (stage có stageOrder nhỏ nhất)
        Optional<ApprovalStage> firstStageOpt = workflow.getStages().stream()
                .min(Comparator.comparingInt(ApprovalStage::getStageOrder));

        if (firstStageOpt.isPresent()) {
            ApprovalStage firstStage = firstStageOpt.get();
            // Đặt hạn duyệt cho bước đầu tiên: 2 ngày kể từ thời điểm resubmit
            firstStage.setDueDate(LocalDateTime.now().plusDays(2));
            approvalStageRepository.save(firstStage);
            User firstApprover = firstStage.getApprover();

            // Gửi thông báo cho người duyệt bước đầu tiên
            Map<String, Object> payload = new HashMap<>();
            String notificationMessage = "Hợp đồng '" + contract.getTitle() +
                    "' đã được chỉnh sửa và nộp lại để phê duyệt. Bạn có hợp đồng cần phê duyệt đợt " +
                    firstStage.getStageOrder();
            payload.put("message", notificationMessage);
            payload.put("contractId", contractId);
            messagingTemplate.convertAndSendToUser(firstApprover.getFullName(), "/queue/notifications", payload);
            sendEmailReminder(contract, firstApprover, firstStage);
            notificationService.saveNotification(firstApprover, notificationMessage, contract);
        }
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

    private void sendEmailReminder(Contract contract, User user, ApprovalStage stage) {
        try {
            DataMailDTO dataMailDTO = new DataMailDTO();
            dataMailDTO.setTo(user.getEmail());
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.CONTRACT_APPROVAL_NOTIFICATION);
            Map<String, Object> props = new HashMap<>();
            props.put("contractTitle", contract.getTitle());
            props.put("stage", stage.getStageOrder());
            dataMailDTO.setProps(props); // Set props to dataMailDTO
            mailService.sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.CONTRACT_APPROVAL_NOTIFICATION);
        } catch (Exception e) {
            // Xu ly loi
            e.printStackTrace();
        }
    }

    // Phương thức chạy mỗi phút (hoặc theo tần suất phù hợp)
    @Scheduled(fixedDelay = 60000)
    public void autoAdvanceExpiredStages() {
        LocalDateTime now = LocalDateTime.now();
        // Tìm tất cả các bước duyệt có trạng thái PENDING mà đã quá hạn
        List<ApprovalStage> expiredStages = approvalStageRepository
                .findByStatusAndDueDateBefore(ApprovalStatus.PENDING, now);

        for (ApprovalStage stage : expiredStages) {
            // Auto "phê duyệt" bước này (hoặc bạn có thể đánh dấu là "expired" nếu muốn)
            stage.setStatus(ApprovalStatus.APPROVED);
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
                nextStage.setDueDate(now.plusDays(2));
                approvalStageRepository.save(nextStage);
                User nextApprover = nextStage.getApprover();
                // Gửi thông báo cho người duyệt tiếp theo
                Map<String, Object> payload = new HashMap<>();
                String notificationMessage = "Bạn có hợp đồng cần phê duyệt đợt " + nextStage.getStageOrder() +
                        ": Hợp đồng " + contract.getTitle();
                payload.put("message", notificationMessage);
                payload.put("contractId", contract.getId());
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

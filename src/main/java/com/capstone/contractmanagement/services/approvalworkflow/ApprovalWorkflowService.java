package com.capstone.contractmanagement.services.approvalworkflow;

import com.capstone.contractmanagement.dtos.approvalworkflow.ApprovalWorkflowDTO;
import com.capstone.contractmanagement.dtos.approvalworkflow.UpdateApprovalStageDTO;
import com.capstone.contractmanagement.entities.ApprovalStage;
import com.capstone.contractmanagement.entities.ApprovalWorkflow;
import com.capstone.contractmanagement.entities.Contract;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.enums.ApprovalStatus;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.IApprovalStageRepository;
import com.capstone.contractmanagement.repositories.IApprovalWorkflowRepository;
import com.capstone.contractmanagement.repositories.IContractRepository;
import com.capstone.contractmanagement.repositories.IUserRepository;
import com.capstone.contractmanagement.responses.approvalworkflow.ApprovalStageResponse;
import com.capstone.contractmanagement.responses.approvalworkflow.ApprovalWorkflowResponse;
import com.capstone.contractmanagement.services.notification.INotificationService;
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApprovalWorkflowService implements IApprovalWorkflowService {
    private final IApprovalWorkflowRepository approvalWorkflowRepository;
    private final IApprovalStageRepository approvalStageRepository;
    private final IContractRepository contractRepository;
    private final IUserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final INotificationService notificationService;

    @Override
    @Transactional
    public ApprovalWorkflowResponse createWorkflow(ApprovalWorkflowDTO approvalWorkflowDTO) {
        // Tạo đối tượng workflow mà không gán contract qua builder
        ApprovalWorkflow workflow = ApprovalWorkflow.builder()
                .name(approvalWorkflowDTO.getName())
                .createdAt(LocalDateTime.now())
                .build();

        // Tạo và thêm các stage nếu có
        if (approvalWorkflowDTO.getStages() != null) {
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

        // Sau đó duyệt qua danh sách stage mới từ DTO và thêm vào workflow
        if (approvalWorkflowDTO.getStages() != null) {
            approvalWorkflowDTO.getStages().forEach(stageDTO -> {
                if (stageDTO.getApproverId() == null) {
                    throw new RuntimeException("Approver id must not be null for stage with stageOrder: " + stageDTO.getStageOrder());
                }
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
                                .stageOrder(stage.getStageOrder())
                                .approver(stage.getApprover().getId())
                                .build())
                        .toList())
                .build();
    }

    @Override
    public void assignWorkflowToContract(Long contractId, Long workflowId) throws DataNotFoundException {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new DataNotFoundException("Contract not found"));
        ApprovalWorkflow workflow = approvalWorkflowRepository.findById(workflowId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.WORKFLOW_NOT_FOUND));
        contract.setApprovalWorkflow(workflow);
        contractRepository.save(contract);
        // Lấy stage có stageOrder nhỏ nhất
        workflow.getStages().stream()
                .min(Comparator.comparingInt(stage -> stage.getStageOrder()))
                .ifPresent(firstStage -> {
                    Map<String, Object> payload = new HashMap<>();
                    // Ví dụ payload thông báo
                    String notificationMessage = "Bạn có hợp đồng cần phê duyệt đợt "+firstStage.getStageOrder()+": Hợp đồng " + contract.getTitle();
                    payload.put("message", notificationMessage);
                    payload.put("contractId", contractId);
                    // Gửi thông báo đến user (sử dụng username làm định danh user destination)
                    User firstApprover = firstStage.getApprover();
                    messagingTemplate.convertAndSendToUser(firstApprover.getFullName(), "/queue/notifications", payload);
                    notificationService.saveNotification(firstApprover, notificationMessage, contractId);
                });
    }

    @Override
    public void updateApprovalStageStatus(Long contractId, Long stageId, UpdateApprovalStageDTO dto) throws DataNotFoundException {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new DataNotFoundException("Contract not found"));
        // Tìm ApprovalStage theo id
        ApprovalStage stage = approvalStageRepository.findById(stageId)
                .orElseThrow(() -> new DataNotFoundException("Approval stage not found with id " + stageId));
        // Cập nhật trạng thái mới
        stage.setStatus(dto.getStatus());
        // Nếu trạng thái được chuyển sang APPROVED (hoặc REJECTED), cập nhật approvedAt
        if (dto.getStatus() == ApprovalStatus.APPROVED || dto.getStatus() == ApprovalStatus.REJECTED) {
            stage.setApprovedAt(LocalDateTime.now());
        }
        approvalStageRepository.save(stage);

        // Nếu trạng thái chuyển thành APPROVED, gửi thông báo qua WebSocket cho người duyệt ở đợt tiếp theo
        if (dto.getStatus() == ApprovalStatus.APPROVED) {
            // Lấy workflow của stage hiện tại
            ApprovalWorkflow workflow = stage.getApprovalWorkflow();
            // Tìm đợt duyệt có stageOrder lớn hơn stage hiện tại, với thứ tự nhỏ nhất (đợt tiếp theo)
            workflow.getStages().stream()
                    .filter(s -> s.getStageOrder() > stage.getStageOrder())
                    .min(Comparator.comparingInt(ApprovalStage::getStageOrder))
                    .ifPresent(nextStage -> {
                        User nextApprover = nextStage.getApprover();
                        // Tạo payload thông báo (có thể bổ sung thêm thông tin theo yêu cầu)
                        Map<String, Object> payload = new HashMap<>();
                        String notificationMessage = "Bạn có hợp đồng cần phê duyệt đợt "+nextStage.getStageOrder()+": Hợp đồng " + contract.getTitle();
                        payload.put("message", notificationMessage);
                        payload.put("contractId", contractId);
                        // Gửi thông báo đến user, sử dụng username làm định danh user destination
                        messagingTemplate.convertAndSendToUser(nextApprover.getFullName(), "/queue/notifications", payload);
                        notificationService.saveNotification(nextApprover, notificationMessage, contractId);
                    });
        }

        if (dto.getStatus() == ApprovalStatus.REJECTED) {
            // comment cho can sua trong hop dong
        }
    }
}

package com.capstone.contractmanagement.services.approvalworkflow;

import com.capstone.contractmanagement.dtos.DataMailDTO;
import com.capstone.contractmanagement.dtos.approvalworkflow.ApprovalWorkflowDTO;
import com.capstone.contractmanagement.dtos.approvalworkflow.UpdateApprovalStageDTO;
import com.capstone.contractmanagement.dtos.approvalworkflow.WorkflowDTO;
import com.capstone.contractmanagement.entities.approval_workflow.ApprovalStage;
import com.capstone.contractmanagement.entities.approval_workflow.ApprovalWorkflow;
import com.capstone.contractmanagement.entities.contract.Contract;
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
import com.capstone.contractmanagement.services.sendmails.IMailService;
import com.capstone.contractmanagement.services.workflow.IWorkflowService;
import com.capstone.contractmanagement.utils.MailTemplate;
import com.capstone.contractmanagement.utils.MessageKeys;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

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
    private final IWorkflowService workflowService;

    @Override
    @Transactional
    public ApprovalWorkflowResponse createWorkflow(ApprovalWorkflowDTO approvalWorkflowDTO) {
        // Tạo đối tượng workflow mà không gán contract qua builder
        ApprovalWorkflow workflow = ApprovalWorkflow.builder()
                .name(approvalWorkflowDTO.getName())
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
                    sendEmailReminder(contract, firstApprover, firstStage);
                    notificationService.saveNotification(firstApprover, notificationMessage, contractId);
                });
    }

    @Override
    public void approvedStage(Long contractId, Long stageId) throws DataNotFoundException {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new DataNotFoundException("Contract not found"));
        // Tìm ApprovalStage theo id
        ApprovalStage stage = approvalStageRepository.findById(stageId)
                .orElseThrow(() -> new DataNotFoundException("Approval stage not found with id " + stageId));
        // Cập nhật trạng thái mới
        stage.setStatus(ApprovalStatus.APPROVED);
        stage.setApprovedAt(LocalDateTime.now());

        approvalStageRepository.save(stage);

        // Nếu trạng thái chuyển thành APPROVED, gửi thông báo qua WebSocket cho người duyệt ở đợt tiếp theo
        if (stage.getStatus() == ApprovalStatus.APPROVED) {
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
                        sendEmailReminder(contract, nextApprover, nextStage);
                        notificationService.saveNotification(nextApprover, notificationMessage, contractId);
                    });
        }
    }

    @Override
    public void rejectStage(Long contractId, Long stageId, WorkflowDTO workflowDTO) throws DataNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new DataNotFoundException("Contract not found"));
        // Tìm ApprovalStage theo id
        ApprovalStage stage = approvalStageRepository.findById(stageId)
                .orElseThrow(() -> new DataNotFoundException("Approval stage not found with id " + stageId));
        // Cập nhật trạng thái mới
        stage.setStatus(ApprovalStatus.REJECTED);
        stage.setApprovedAt(LocalDateTime.now());
        approvalStageRepository.save(stage);
        workflowService.createWorkflow(contract, workflowDTO, currentUser);
        Map<String, Object> payload = new HashMap<>();
        String notificationMessage = "Bạn có hợp đồng cần chỉnh sửa: Hợp đồng " + contract.getTitle();
        payload.put("message", notificationMessage);
        payload.put("contractId", contractId);
        // Gửi thông báo đến user, sử dụng username làm định danh user destination
        messagingTemplate.convertAndSendToUser(contract.getUser().getFullName(), "/queue/notifications", payload);
        notificationService.saveNotification(contract.getUser(), notificationMessage, contractId);
    }

    @Override
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
                                .build())
                        .toList())
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
}

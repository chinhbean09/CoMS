package com.capstone.contractmanagement.services.approvalworkflow;

import com.capstone.contractmanagement.dtos.approvalworkflow.ApprovalWorkflowDTO;
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
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApprovalWorkflowService implements IApprovalWorkflowService {
    private final IApprovalWorkflowRepository approvalWorkflowRepository;
    private final IApprovalStageRepository approvalStageRepository;
    private final IContractRepository contractRepository;
    private final IUserRepository userRepository;

    @Override
    @Transactional
    public void createWorkflow(ApprovalWorkflowDTO approvalWorkflowDTO) {
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
    }
}

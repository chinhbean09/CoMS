package com.capstone.contractmanagement.services.approvalworkflow;

import com.capstone.contractmanagement.dtos.approvalworkflow.ApprovalWorkflowDTO;
import com.capstone.contractmanagement.dtos.approvalworkflow.UpdateApprovalStageDTO;
import com.capstone.contractmanagement.dtos.approvalworkflow.WorkflowDTO;
import com.capstone.contractmanagement.enums.ApprovalStatus;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.approvalworkflow.ApprovalStageResponse;
import com.capstone.contractmanagement.responses.approvalworkflow.ApprovalWorkflowResponse;
import com.capstone.contractmanagement.responses.approvalworkflow.CommentResponse;
import com.capstone.contractmanagement.responses.contract.ContractResponse;

import java.util.List;

public interface IApprovalWorkflowService {
    ApprovalWorkflowResponse createWorkflow(ApprovalWorkflowDTO approvalWorkflowDTO);

    List<ApprovalWorkflowResponse> getAllWorkflows();

    // Update approval workflow
    void updateWorkflow(Long id, ApprovalWorkflowDTO approvalWorkflowDTO) throws DataNotFoundException;

    // Delete approval workflow
    void deleteWorkflow(Long id) throws DataNotFoundException;

    // get approval workflow by id
    ApprovalWorkflowResponse getWorkflowById(Long id) throws DataNotFoundException;

    // assign approval workflow to contract
    void assignWorkflowToContract(Long contractId, Long workflowId) throws DataNotFoundException;
    void approvedStage(Long contractId, Long stageId) throws DataNotFoundException;
    void rejectStage(Long contractId, Long stageId, WorkflowDTO workflowDTO) throws DataNotFoundException;

    ApprovalWorkflowResponse getWorkflowByContractId(Long contractId) throws DataNotFoundException;

    // get approval workflow by contractTypeId
    List<ApprovalWorkflowResponse> getWorkflowByContractTypeId(Long contractTypeId) throws DataNotFoundException;

    List<CommentResponse> getApprovalStageCommentDetailsByContractId(Long contractId) throws DataNotFoundException;

    List<ContractResponse> getContractsForApprover(Long approverId);
    void resubmitContractForApproval(Long contractId) throws DataNotFoundException;

}

package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.dtos.approvalworkflow.ApprovalWorkflowDTO;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.responses.approvalworkflow.ApprovalWorkflowResponse;
import com.capstone.contractmanagement.services.approvalworkflow.IApprovalWorkflowService;
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/approval-workflows")
@RequiredArgsConstructor
public class ApprovalWorkflowController {
    private final IApprovalWorkflowService approvalWorkflowService;

    // api get all approval workflows
    @GetMapping("/get-all")
    public ResponseEntity<ResponseObject> getAllApprovalWorkflows() {
        List<ApprovalWorkflowResponse> approvalWorkflows = approvalWorkflowService.getAllWorkflows();
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.GET_ALL_APPROVAL_WORKFLOWS_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .data(approvalWorkflows)
                .build());
    }

    // api create approval workflow
    @PostMapping("/create")
    public ResponseEntity<ResponseObject> createApprovalWorkflow(@RequestBody ApprovalWorkflowDTO approvalWorkflowDTO) {
        ApprovalWorkflowResponse response = approvalWorkflowService.createWorkflow(approvalWorkflowDTO);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.CREATE_APPROVAL_WORKFLOW_SUCCESSFULLY)
                .status(HttpStatus.CREATED)
                .data(response)
                .build());
    }

    // api update approval workflow
    @PutMapping("/update/{id}")
    public ResponseEntity<ResponseObject> updateApprovalWorkflow(@PathVariable Long id, @RequestBody ApprovalWorkflowDTO approvalWorkflowDTO) throws DataNotFoundException {
        approvalWorkflowService.updateWorkflow(id, approvalWorkflowDTO);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.UPDATE_APPROVAL_WORKFLOW_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .build());
    }

    // api delete approval workflow
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ResponseObject> deleteApprovalWorkflow(@PathVariable Long id) throws DataNotFoundException {
        approvalWorkflowService.deleteWorkflow(id);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.DELETE_APPROVAL_WORKFLOW_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .build());
    }

    // api get approval workflow by id
    @GetMapping("/get-by-id/{id}")
    public ResponseEntity<ResponseObject> getApprovalWorkflowById(@PathVariable Long id) throws DataNotFoundException {
        ApprovalWorkflowResponse approvalWorkflowResponse = approvalWorkflowService.getWorkflowById(id);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.GET_APPROVAL_WORKFLOW_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .data(approvalWorkflowResponse)
                .build());
    }

    // assign approval workflow to contract
    @PutMapping("/assign/{contractId}/{workflowId}")
    public ResponseEntity<ResponseObject> assignWorkflowToContract(@PathVariable Long contractId, @PathVariable Long workflowId) throws DataNotFoundException {
        approvalWorkflowService.assignWorkflowToContract(contractId, workflowId);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.ASSIGN_APPROVAL_WORKFLOW_TO_CONTRACT_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .build());
    }

    // api APPROVED stage
    @PutMapping("/approve/{contractId}/{stageId}")
    public ResponseEntity<ResponseObject> approveStage(@PathVariable Long contractId, @PathVariable Long stageId) throws DataNotFoundException {
        approvalWorkflowService.approvedStage(contractId, stageId);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.APPROVE_STAGE_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .build());
    }

    // api REJECTED stage
    @PutMapping("/reject/{contractId}/{stageId}")
    public ResponseEntity<ResponseObject> rejectStage(@PathVariable Long contractId, @PathVariable Long stageId) throws DataNotFoundException {
        approvalWorkflowService.rejectStage(contractId, stageId, null);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.REJECT_STAGE_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .build());
    }
}

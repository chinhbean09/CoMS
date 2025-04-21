package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.dtos.approvalworkflow.ApprovalWorkflowDTO;
import com.capstone.contractmanagement.dtos.approvalworkflow.WorkflowDTO;
import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.responses.approvalworkflow.ApprovalWorkflowResponse;
import com.capstone.contractmanagement.responses.approvalworkflow.CommentResponse;
import com.capstone.contractmanagement.responses.contract.ContractResponse;
import com.capstone.contractmanagement.responses.contract.GetContractForApproverResponse;
import com.capstone.contractmanagement.services.approvalworkflow.IApprovalWorkflowService;
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("${api.prefix}/approval-workflows")
@RequiredArgsConstructor
public class ApprovalWorkflowController {
    private final IApprovalWorkflowService approvalWorkflowService;

    // api get all approval workflows
    @GetMapping("/get-all")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_ADMIN')")
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
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_ADMIN')")
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
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> updateApprovalWorkflow(@PathVariable Long id, @RequestBody ApprovalWorkflowDTO approvalWorkflowDTO) throws DataNotFoundException {
        approvalWorkflowService.updateWorkflow(id, approvalWorkflowDTO);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.UPDATE_APPROVAL_WORKFLOW_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .build());
    }

    // api delete approval workflow
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> deleteApprovalWorkflow(@PathVariable Long id) throws DataNotFoundException {
        approvalWorkflowService.deleteWorkflow(id);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.DELETE_APPROVAL_WORKFLOW_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .build());
    }

    // api get approval workflow by id
    @GetMapping("/get-by-id/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_ADMIN')")
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
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF')")
    public ResponseEntity<ResponseObject> assignWorkflowToContract(@PathVariable Long contractId, @PathVariable Long workflowId) throws DataNotFoundException {
        approvalWorkflowService.assignWorkflowToContract(contractId, workflowId);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.ASSIGN_APPROVAL_WORKFLOW_TO_CONTRACT_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .build());
    }

    // api APPROVED stage
    @PutMapping("/approve/{contractId}/{stageId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> approveStage(@PathVariable Long contractId, @PathVariable Long stageId) throws DataNotFoundException {
        approvalWorkflowService.approvedStage(contractId, stageId);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.APPROVE_STAGE_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .build());
    }

    // api REJECTED stage
    @PutMapping("/reject/{contractId}/{stageId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> rejectStage(@PathVariable Long contractId, @PathVariable Long stageId, @RequestBody WorkflowDTO workflowDTO) throws DataNotFoundException {
        approvalWorkflowService.rejectStage(contractId, stageId, workflowDTO);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.REJECT_STAGE_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .build());
    }

    // api get approval workflow by contract id
    @GetMapping("/get-by-contract-id/{contractId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> getApprovalWorkflowByContractId(@PathVariable Long contractId) throws DataNotFoundException {
        ApprovalWorkflowResponse approvalWorkflowResponse = approvalWorkflowService.getWorkflowByContractId(contractId);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.GET_APPROVAL_WORKFLOW_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .data(approvalWorkflowResponse)
                .build());
    }

    // api get approval workflow by contract type id
    @GetMapping("/get-by-contract-type-id/{contractTypeId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> getApprovalWorkflowByContractTypeId(@PathVariable Long contractTypeId) throws DataNotFoundException {
        List<ApprovalWorkflowResponse> approvalWorkflowResponse = approvalWorkflowService.getWorkflowByContractTypeId(contractTypeId);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.GET_APPROVAL_WORKFLOW_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .data(approvalWorkflowResponse)
                .build());
    }

    @GetMapping("/get-contract-comments/{contractId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> getApprovalComments(@PathVariable Long contractId) throws DataNotFoundException {
        List<CommentResponse> comments = approvalWorkflowService.getApprovalStageCommentDetailsByContractId(contractId);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.GET_APPROVAL_WORKFLOW_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .data(comments)
                .build());
    }
    @GetMapping("/get-contract-for-approver/{approverId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> getContractForApprover(
            @PathVariable Long approverId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "contractTypeId", required = false) Long contractTypeId,
            @RequestParam(value = "status", required = false) ContractStatus status,  // <-- mới
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size)
            throws DataNotFoundException {

        Page<GetContractForApproverResponse> contracts =
                approvalWorkflowService.getContractsForApprover(
                        approverId, keyword, contractTypeId, status, page, size);

        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.GET_APPROVAL_WORKFLOW_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .data(contracts)
                .build());
    }

    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF')")
    @PostMapping("/resubmit/{contractId}")
    public ResponseEntity<String> resubmitContract(@PathVariable Long contractId) {
        try {
            approvalWorkflowService.resubmitContractForApproval(contractId);
            return ResponseEntity.ok("Contract resubmitted for approval successfully.");
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while resubmitting the contract.");
        }
    }

    @GetMapping("/get-contract-for-manager/{managerId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> getContractForManager(@PathVariable Long managerId,
                                                                @RequestParam(value = "keyword", required = false) String keyword,
                                                                @RequestParam(value = "contractTypeId", required = false) Long contractTypeId,
                                                                @RequestParam(value = "page", defaultValue = "0") int page,
                                                                @RequestParam(value = "size", defaultValue = "10") int size) throws DataNotFoundException {
        Page<GetContractForApproverResponse> contracts = approvalWorkflowService.getContractsForManager(managerId, keyword, contractTypeId, page, size);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.GET_APPROVAL_WORKFLOW_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .data(contracts)
                .build());
    }

    // Endpoint để lấy số lượng hợp đồng và phụ lục cần phê duyệt và bị từ chối
    @GetMapping("/get-approval-stats")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> getApprovalStats() {
        // Gọi service để lấy thống kê và trả về cho client
        return ResponseEntity.ok(ResponseObject.builder()
                .message("Lấy thống kê phê duyệt")
                .status(HttpStatus.OK)
                .data(approvalWorkflowService.getApprovalStats())
                .build());
    }

}

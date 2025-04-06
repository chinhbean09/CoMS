package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.dtos.addendum.AddendumDTO;
import com.capstone.contractmanagement.dtos.approvalworkflow.AddendumApprovalWorkflowDTO;
import com.capstone.contractmanagement.dtos.approvalworkflow.ApprovalWorkflowDTO;
import com.capstone.contractmanagement.dtos.approvalworkflow.WorkflowDTO;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.enums.AddendumStatus;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.responses.addendum.AddendumResponse;
import com.capstone.contractmanagement.responses.approvalworkflow.ApprovalWorkflowResponse;
import com.capstone.contractmanagement.responses.approvalworkflow.CommentResponse;
import com.capstone.contractmanagement.responses.contract.GetContractForApproverResponse;
import com.capstone.contractmanagement.services.addendum.IAddendumService;
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/addendums")
@RequiredArgsConstructor
public class AddendumController {
    private final IAddendumService addendumService;

    // api create addendum
    @PostMapping("/create")
    public ResponseEntity<ResponseObject> createAddendum(@RequestBody AddendumDTO addendumDTO) throws DataNotFoundException {
        AddendumResponse addendumResponse = addendumService.createAddendum(addendumDTO);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.CREATED)
                .message("Tạo phụ lục thành công")
                .data(addendumResponse)
                .build());
    }

    @GetMapping("/get-by-contract-id/{contractId}")
    public ResponseEntity<ResponseObject> getAllByContract(@PathVariable Long contractId) throws DataNotFoundException {
        List<AddendumResponse> addendumResponseList = addendumService.getAllByContractId(contractId);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Lấy danh sách phụ lục thành công")
                .data(addendumResponseList)
                .build());
    }

    @GetMapping("/get-by-type/{addendumTypeId}")
    public ResponseEntity<ResponseObject> getAllByAddendumType(@PathVariable Long addendumTypeId) throws DataNotFoundException {
        List<AddendumResponse> addendumResponseList = addendumService.getAllByAddendumType(addendumTypeId);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Lấy danh sách phụ lục thành công")
                .data(addendumResponseList)
                .build());
    }

    @PutMapping("/update/{addendumId}")
    public ResponseEntity<String> updateAddendum(@PathVariable Long addendumId,
                                                 @RequestBody AddendumDTO addendumDTO) throws DataNotFoundException {
        return ResponseEntity.ok(addendumService.updateAddendum(addendumId, addendumDTO));
    }

    @DeleteMapping("/delete/{addendumId}")
    public ResponseEntity<ResponseObject> deleteAddendum(@PathVariable Long addendumId) throws DataNotFoundException {
        addendumService.deleteAddendum(addendumId);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Xóa phụ lục thành công")
                .data(null)
                .build());
    }

    @GetMapping("/get-by-id/{addendumId}")
    public ResponseEntity<ResponseObject> getAddendumById(@PathVariable Long addendumId) throws DataNotFoundException {
        AddendumResponse addendumResponse = addendumService.getAddendumById(addendumId);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Lấy phụ lục theo id")
                .data(addendumResponse)
                .build());
    }

    // assign approval workflow to contract
    @PutMapping("/assign-old-workflow-of-contract/{addendumId}")
    public ResponseEntity<ResponseObject> assignWorkflowToContract(@PathVariable Long addendumId) throws DataNotFoundException {
        addendumService.assignApprovalWorkflowOfContractToAddendum(addendumId);
        return ResponseEntity.ok(ResponseObject.builder()
                .message("Chọn quy trình phe duyet cho phụ lục")
                .status(HttpStatus.OK)
                .build());
    }

    // assign approval workflow to contract
    @PutMapping("/assign-new-workflow/{addendumId}/{workflowId}")
    public ResponseEntity<ResponseObject> assignNewWorkflow(@PathVariable Long addendumId, @PathVariable Long workflowId) throws DataNotFoundException {
        addendumService.assignWorkflowToAddendum(addendumId, workflowId);
        return ResponseEntity.ok(ResponseObject.builder()
                .message("Chọn quy trình phe duyet cho phụ lục")
                .status(HttpStatus.OK)
                .build());
    }

    @PutMapping("/approve/{addendumId}/{stageId}")
    public ResponseEntity<ResponseObject> approveStage(@PathVariable Long addendumId, @PathVariable Long stageId) throws DataNotFoundException {
        addendumService.approvedStageForAddendum(addendumId, stageId);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.APPROVE_STAGE_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .build());
    }

    @PutMapping("/reject/{addendumId}/{stageId}")
    public ResponseEntity<ResponseObject> rejectStage(@PathVariable Long addendumId, @PathVariable Long stageId, @RequestBody WorkflowDTO workflowDTO) throws DataNotFoundException {
        addendumService.rejectStageForAddendum(addendumId, stageId, workflowDTO);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.REJECT_STAGE_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .build());
    }

    @PostMapping("/resubmit/{addendumId}")
    public ResponseEntity<ResponseObject> resubmitContract(@PathVariable Long addendumId) throws DataNotFoundException {
        addendumService.resubmitAddendumForApproval(addendumId);
        return ResponseEntity.ok(ResponseObject.builder()
                .message("Nộp lại cho quy trình duyệt thành công")
                .status(HttpStatus.OK)
                .build());
    }


    @GetMapping("/get-all")
    public ResponseEntity<ResponseObject> getFilteredAddenda(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<AddendumStatus> statuses,
            @RequestParam(required = false) List<Long> addendumTypeIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User currentUser
    ) {
        Page<AddendumResponse> responsePage = addendumService.getAddendaByUserWithFilters(
                currentUser.getId(), keyword, statuses, addendumTypeIds, page, size, currentUser
        );

        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Lấy danh sách phụ lục theo bộ lọc thành công")
                .data(responsePage)
                .build());
    }

    @GetMapping("/get-addendum-for-approver/{approverId}")
    public ResponseEntity<ResponseObject> getContractForApprover(@PathVariable Long approverId,
                                                                 @RequestParam(value = "keyword", required = false) String keyword,
                                                                 @RequestParam(value = "addendumTypeId", required = false) Long addendumTypeId,
                                                                 @RequestParam(value = "page", defaultValue = "0") int page,
                                                                 @RequestParam(value = "size", defaultValue = "10") int size) throws DataNotFoundException {
        Page<AddendumResponse> addendumResponses = addendumService.getAddendaForManager(approverId, keyword, addendumTypeId, page, size);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.GET_APPROVAL_WORKFLOW_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .data(addendumResponses)
                .build());
    }

    @GetMapping("/get-addendum-for-manager/{managerId}")
    public ResponseEntity<ResponseObject> getAddendaForManager(@PathVariable Long managerId,
                                                               @RequestParam(value = "keyword", required = false) String keyword,
                                                               @RequestParam(value = "addendumTypeId", required = false) Long addendumTypeId,
                                                               @RequestParam(value = "page", defaultValue = "0") int page,
                                                               @RequestParam(value = "size", defaultValue = "10") int size) throws DataNotFoundException {
        Page<AddendumResponse> addendumResponses = addendumService.getAddendaForApprover(managerId, keyword, addendumTypeId, page, size);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.GET_APPROVAL_WORKFLOW_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .data(addendumResponses)
                .build());
    }

    @GetMapping("/get-workflow-by-addendum/{addendumId}")
    public ResponseEntity<ResponseObject> getApprovalWorkflowByContractId(@PathVariable Long addendumId) throws DataNotFoundException {
        ApprovalWorkflowResponse approvalWorkflowResponse = addendumService.getWorkflowByAddendumId(addendumId);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.GET_APPROVAL_WORKFLOW_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .data(approvalWorkflowResponse)
                .build());
    }

    @PostMapping("/create-workflow")
    public ResponseEntity<ResponseObject> createApprovalWorkflow(@RequestBody AddendumApprovalWorkflowDTO approvalWorkflowDTO) {
        ApprovalWorkflowResponse response = addendumService.createWorkflowForAddendum(approvalWorkflowDTO);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.CREATE_APPROVAL_WORKFLOW_SUCCESSFULLY)
                .status(HttpStatus.CREATED)
                .data(response)
                .build());
    }

    // api get approval workflow by contract type id
    @GetMapping("/get-workflow-by-addendum-type/{addendumTypeId}")
    public ResponseEntity<ResponseObject> getApprovalWorkflowByAddendumTypeId(@PathVariable Long addendumTypeId) {
        List<ApprovalWorkflowResponse> approvalWorkflowResponse = addendumService.getWorkflowByAddendumTypeId(addendumTypeId);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.GET_APPROVAL_WORKFLOW_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .data(approvalWorkflowResponse)
                .build());
    }

    @GetMapping("/get-addendum-comments/{addendumId}")
    public ResponseEntity<ResponseObject> getApprovalComments(@PathVariable Long addendumId) throws DataNotFoundException {
        List<CommentResponse> comments = addendumService.getApprovalStageCommentDetailsByAddendumId(addendumId);
        return ResponseEntity.ok(ResponseObject.builder()
                .message("Lấy comment phụ lục")
                .status(HttpStatus.OK)
                .data(comments)
                .build());
    }

    @PostMapping("/duplicate/{addendumId}/{contractId}")
    public ResponseEntity<ResponseObject> duplicateAddendum(@PathVariable Long addendumId, @PathVariable Long contractId) throws DataNotFoundException {
        AddendumResponse addendumResponse = addendumService.duplicateAddendum(addendumId, contractId);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.CREATED)
                .message("Nhân bản phụ lục thành công")
                .data(addendumResponse)
                .build());
    }
}

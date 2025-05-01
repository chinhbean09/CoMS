package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.components.SecurityUtils;
import com.capstone.contractmanagement.dtos.FileBase64DTO;
import com.capstone.contractmanagement.dtos.addendum.AddendumDTO;
import com.capstone.contractmanagement.dtos.addendum.SignAddendumRequest;
import com.capstone.contractmanagement.dtos.approvalworkflow.AddendumApprovalWorkflowDTO;
import com.capstone.contractmanagement.dtos.approvalworkflow.WorkflowDTO;
import com.capstone.contractmanagement.entities.addendum.Addendum;
import com.capstone.contractmanagement.entities.Role;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.enums.AddendumStatus;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.IAddendumRepository;
import com.capstone.contractmanagement.repositories.IAuditTrailRepository;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.responses.addendum.AddendumResponse;
import com.capstone.contractmanagement.responses.approvalworkflow.ApprovalWorkflowResponse;
import com.capstone.contractmanagement.responses.approvalworkflow.CommentResponse;
import com.capstone.contractmanagement.services.addendum.IAddendumService;
import com.capstone.contractmanagement.services.sendmails.IMailService;
import com.capstone.contractmanagement.utils.MessageKeys;
import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@RestController
@RequestMapping("${api.prefix}/addendums")
@RequiredArgsConstructor
public class AddendumController {
    private final IAddendumService addendumService;
    private final IAddendumRepository addendumRepository;
    private final IMailService mailService;
    private final Cloudinary cloudinary;
    private final IAuditTrailRepository auditTrailRepository;

    // api create addendum
    @PostMapping("/create")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF')")
    public ResponseEntity<ResponseObject> createAddendum(@RequestBody AddendumDTO addendumDTO) throws DataNotFoundException {
        AddendumResponse addendumResponse = addendumService.createAddendum(addendumDTO);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.CREATED)
                .message("Tạo phụ lục thành công")
                .data(addendumResponse)
                .build());
    }

    @GetMapping("/get-by-contract-id/{contractId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> getAllByContract(@PathVariable Long contractId) throws DataNotFoundException {
        List<AddendumResponse> addendumResponseList = addendumService.getAllByContractId(contractId);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Lấy danh sách phụ lục thành công")
                .data(addendumResponseList)
                .build());
    }

    @PutMapping("/update/{addendumId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF')")
    public ResponseEntity<String> updateAddendum(@PathVariable Long addendumId,
                                                 @RequestBody AddendumDTO addendumDTO) throws DataNotFoundException {
        return ResponseEntity.ok(addendumService.updateAddendum(addendumId, addendumDTO));
    }

    @DeleteMapping("/delete/{addendumId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> deleteAddendum(@PathVariable Long addendumId) throws DataNotFoundException {
        addendumService.deleteAddendum(addendumId);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Xóa phụ lục thành công")
                .data(null)
                .build());
    }

    @GetMapping("/get-by-id/{addendumId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> getAddendumById(@PathVariable Long addendumId) throws DataNotFoundException {
        Optional<AddendumResponse> addendumResponse = addendumService.getAddendumById(addendumId);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Lấy phụ lục theo id")
                .data(addendumResponse)
                .build());
    }

    // assign approval workflow to contract
    @PutMapping("/assign-old-workflow-of-contract/{addendumId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF')")
    public ResponseEntity<ResponseObject> assignWorkflowToContract(@PathVariable Long addendumId) throws DataNotFoundException {
        addendumService.assignApprovalWorkflowOfContractToAddendum(addendumId);
        return ResponseEntity.ok(ResponseObject.builder()
                .message("Chọn quy trình phe duyet cho phụ lục")
                .status(HttpStatus.OK)
                .build());
    }

    // assign approval workflow to contract
    @PutMapping("/assign-new-workflow/{addendumId}/{workflowId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF')")
    public ResponseEntity<ResponseObject> assignNewWorkflow(@PathVariable Long addendumId, @PathVariable Long workflowId) throws DataNotFoundException {
        addendumService.assignWorkflowToAddendum(addendumId, workflowId);
        return ResponseEntity.ok(ResponseObject.builder()
                .message("Chọn quy trình phe duyet cho phụ lục")
                .status(HttpStatus.OK)
                .build());
    }

    @PutMapping("/approve/{addendumId}/{stageId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> approveStage(@PathVariable Long addendumId, @PathVariable Long stageId) throws DataNotFoundException {
        addendumService.approvedStageForAddendum(addendumId, stageId);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.APPROVE_STAGE_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .build());
    }

    @PutMapping("/reject/{addendumId}/{stageId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> rejectStage(@PathVariable Long addendumId, @PathVariable Long stageId, @RequestBody WorkflowDTO workflowDTO) throws DataNotFoundException {
        addendumService.rejectStageForAddendum(addendumId, stageId, workflowDTO);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.REJECT_STAGE_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .build());
    }

    @PostMapping("/resubmit/{addendumId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF')")
    public ResponseEntity<ResponseObject> resubmitContract(@PathVariable Long addendumId) throws DataNotFoundException {
        addendumService.resubmitAddendumForApproval(addendumId);
        return ResponseEntity.ok(ResponseObject.builder()
                .message("Nộp lại cho quy trình duyệt thành công")
                .status(HttpStatus.OK)
                .build());
    }


    @GetMapping("/get-all")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> getFilteredAddenda(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<AddendumStatus> statuses,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User currentUser
    ) {
        Page<AddendumResponse> responsePage = addendumService.getAddendaByUserWithFilters(
                currentUser.getId(), keyword, statuses, page, size, currentUser
        );

        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Lấy danh sách phụ lục theo bộ lọc thành công")
                .data(responsePage)
                .build());
    }

    @GetMapping("/get-addendum-for-approver/{approverId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> getContractForApprover(@PathVariable Long approverId,
                                                                 @RequestParam(value = "keyword", required = false) String keyword,
                                                                 @RequestParam(value = "page", defaultValue = "0") int page,
                                                                 @RequestParam(value = "size", defaultValue = "10") int size) throws DataNotFoundException {
        Page<AddendumResponse> addendumResponses = addendumService.getAddendaForManager(approverId, keyword, page, size);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.GET_APPROVAL_WORKFLOW_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .data(addendumResponses)
                .build());
    }

    @GetMapping("/get-addendum-for-manager/{managerId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> getAddendaForManager(@PathVariable Long managerId,
                                                               @RequestParam(value = "keyword", required = false) String keyword,
                                                               @RequestParam(value = "page", defaultValue = "0") int page,
                                                               @RequestParam(value = "size", defaultValue = "10") int size) throws DataNotFoundException {
        Page<AddendumResponse> addendumResponses = addendumService.getAddendaForApprover(managerId, keyword, page, size);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.GET_APPROVAL_WORKFLOW_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .data(addendumResponses)
                .build());
    }

    @GetMapping("/get-workflow-by-addendum/{addendumId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> getApprovalWorkflowByContractId(@PathVariable Long addendumId) throws DataNotFoundException {
        ApprovalWorkflowResponse approvalWorkflowResponse = addendumService.getWorkflowByAddendumId(addendumId);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.GET_APPROVAL_WORKFLOW_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .data(approvalWorkflowResponse)
                .build());
    }

    @PostMapping("/create-workflow")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF')")
    public ResponseEntity<ResponseObject> createApprovalWorkflow(@RequestBody AddendumApprovalWorkflowDTO approvalWorkflowDTO) {
        ApprovalWorkflowResponse response = addendumService.createWorkflowForAddendum(approvalWorkflowDTO);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.CREATE_APPROVAL_WORKFLOW_SUCCESSFULLY)
                .status(HttpStatus.CREATED)
                .data(response)
                .build());
    }

    // api get approval workflow by contract type id
    @GetMapping("/get-workflow-by-addendum-type")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> getApprovalWorkflowByAddendumTypeId() {
        List<ApprovalWorkflowResponse> approvalWorkflowResponse = addendumService.getWorkflowByAddendumTypeId();
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.GET_APPROVAL_WORKFLOW_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .data(approvalWorkflowResponse)
                .build());
    }

    @GetMapping("/get-addendum-comments/{addendumId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF')")
    public ResponseEntity<ResponseObject> getApprovalComments(@PathVariable Long addendumId) throws DataNotFoundException {
        List<CommentResponse> comments = addendumService.getApprovalStageCommentDetailsByAddendumId(addendumId);
        return ResponseEntity.ok(ResponseObject.builder()
                .message("Lấy comment phụ lục")
                .status(HttpStatus.OK)
                .data(comments)
                .build());
    }

    @PostMapping("/duplicate/{addendumId}/{contractId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF')")
    public ResponseEntity<ResponseObject> duplicateAddendum(@PathVariable Long addendumId, @PathVariable Long contractId) throws DataNotFoundException {
        AddendumResponse addendumResponse = addendumService.duplicateAddendum(addendumId, contractId);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.CREATED)
                .message("Nhân bản phụ lục thành công")
                .data(addendumResponse)
                .build());
    }

    @PostMapping("/sign")
    @PreAuthorize("hasAnyAuthority('ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> signAddenda(@RequestBody @Valid SignAddendumRequest request) {
        try {
            return addendumService.signAddendum(request);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseObject.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .message("Lỗi hệ thống: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }



    @PutMapping(value = "/upload-signed-addenda-file/{addendumId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_STAFF')")
    public ResponseEntity<ResponseObject> uploadSignedAddendum(@PathVariable long addendumId,
                                                               @RequestParam("files") List<MultipartFile> files) throws DataNotFoundException {
        addendumService.uploadSignedAddendum(addendumId, files);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .data(null)
                .message("Cập nhật các hình ảnh phụ lục đã kí")
                .build());
    }

    @GetMapping("/signed-addenda-urls/{addendumId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> getBillUrls(@PathVariable Long addendumId) throws DataNotFoundException {
        return ResponseEntity.ok(ResponseObject.builder()
                .message("Lấy link phụ lục đã ký")
                .status(HttpStatus.OK)
                .data(addendumService.getSignedAddendumUrl(addendumId))
                .build());
    }

    @PostMapping("/upload-file-base64")
    @PreAuthorize("hasAnyAuthority('ROLE_STAFF', 'ROLE_MANAGER', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> uploadFileBase64(@RequestParam Long addendumId,
                                                           @RequestBody FileBase64DTO fileBase64DTO,
                                                           @RequestParam String fileName) throws DataNotFoundException, IOException {
        addendumService.uploadFileBase64(addendumId, fileBase64DTO, fileName);
        return ResponseEntity.ok(ResponseObject.builder()
                .message("Tải lên thành công")
                .status(HttpStatus.OK)
                .data(null)
                .build());
    }

    @PutMapping(value = "/upload-bills/{paymentScheduleId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF')")
    public ResponseEntity<ResponseObject> uploadAddendumPaymentBillUrls(@PathVariable long paymentScheduleId,
                                                                @RequestParam("files") List<MultipartFile> files) throws DataNotFoundException {
        addendumService.uploadPaymentBillUrls(paymentScheduleId, files);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .data(null)
                .message("Cập nhật các hóa đơn thanh toán thành công")
                .build());
    }

    @GetMapping("/get-bill-urls/{paymentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> getAddendumBillUrls(@PathVariable Long paymentId) throws DataNotFoundException {
        return ResponseEntity.ok(ResponseObject.builder()
                .message("Lấy link hóa đơn thanh toán")
                .status(HttpStatus.OK)
                .data(addendumService.getBillUrlsByAddendumPaymentId(paymentId))
                .build());
    }

}

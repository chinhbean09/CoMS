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

//    @GetMapping("/get-by-type/{addendumTypeId}")
//    public ResponseEntity<ResponseObject> getAllByAddendumType(@PathVariable Long addendumTypeId) throws DataNotFoundException {
//        List<AddendumResponse> addendumResponseList = addendumService.getAllByAddendumType(addendumTypeId);
//        return ResponseEntity.ok(ResponseObject.builder()
//                .status(HttpStatus.OK)
//                .message("Lấy danh sách phụ lục thành công")
//                .data(addendumResponseList)
//                .build());
//    }

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
        Optional<AddendumResponse> addendumResponse = addendumService.getAddendumById(addendumId);
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
    @GetMapping("/get-workflow-by-addendum-type")
    public ResponseEntity<ResponseObject> getApprovalWorkflowByAddendumTypeId() {
        List<ApprovalWorkflowResponse> approvalWorkflowResponse = addendumService.getWorkflowByAddendumTypeId();
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

    @PostMapping("/sign")
    public ResponseEntity<ResponseObject> signAddenda(@RequestBody @Valid SignAddendumRequest request) {
        try {
            // Fetch contract by ID from the repository
            Optional<Addendum> optionalAddendum = addendumRepository.findById(request.getAddendumId());
            if (optionalAddendum.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        ResponseObject.builder()
                                .status(HttpStatus.BAD_REQUEST)
                                .message("Addendum not found")
                                .data(null)
                                .build()
                );
            }

            if (optionalAddendum.get().getStatus() == AddendumStatus.SIGNED) {
                return ResponseEntity.badRequest().body(
                        ResponseObject.builder()
                                .status(HttpStatus.BAD_REQUEST)
                                .message("Phụ lục trên đã được kí")
                                .data(null)
                                .build()
                );
            }

            SecurityUtils securityUtils = new SecurityUtils();
            User currentUser = securityUtils.getLoggedInUser();
            if (!Objects.equals(currentUser.getRole().getRoleName(), Role.DIRECTOR)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseObject.builder()
                                .status(HttpStatus.UNAUTHORIZED)
                                .message("Chỉ có giám đốc mới được quyền ký")
                                .data(null)
                                .build());
            }

            Addendum addendum = optionalAddendum.get();

            if (addendum.getStatus() != AddendumStatus.APPROVED) {
                return ResponseEntity.badRequest().body(
                        ResponseObject.builder()
                                .status(HttpStatus.BAD_REQUEST)
                                .message("Chỉ được ký phụ lục ở trạng thái 'Đã phê duyệt'")
                                .data(null)
                                .build()
                );
            }

            // Save signed file (can throw IOException)
            String filePath = saveSignedFile(request.getFileName(), request.getFileBase64());

            // Update contract details
            String oldStatus = addendum.getStatus() != null ? addendum.getStatus().name() : "UNKNOWN";
            //addendum.setSignedFilePath(filePath);
            addendum.setSignedBy(currentUser.getFullName());

            // Parse signedAt with error handling
            try {
                LocalDateTime signedAt = LocalDateTime.parse(request.getSignedAt(), DateTimeFormatter.ISO_DATE_TIME);
                addendum.setSignedAt(signedAt);
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest().body(
                        ResponseObject.builder()
                                .status(HttpStatus.BAD_REQUEST)
                                .message("Invalid signedAt format. Use ISO-8601 format.")
                                .data(null)
                                .build()
                );
            }

            // Update contract status
            addendum.setStatus(AddendumStatus.SIGNED);
            addendum.setSignedFilePath(filePath);

            // Save the contract changes
            addendumRepository.save(addendum);
            // send mail
            mailService.sendEmailAddendumSignedSuccess(addendum);

            // Ghi audit trail
            //logAuditTrail(contract, "UPDATE", "status", oldStatus, ContractStatus.SIGNED.name(), currentUser.getFullName() );

            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Phụ lục đã được ký và sao lưu thành công.")
                    .data(null)
                    .build());

            //Xóa thông báo cũ của hợp đồng




        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseObject.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .message("Lỗi hệ thống: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }

    private String saveSignedFile(String fileName, String fileBase64) throws IOException {

        byte[] fileBytes = Base64.getDecoder().decode(fileBase64);

        // Upload as a raw file to Cloudinary
        Map<String, Object> uploadResult = cloudinary.uploader().upload(fileBytes, ObjectUtils.asMap(
                "resource_type", "raw",      // Cho phép upload file dạng raw
                "folder", "signed_addenda",
                "use_filename", true,        // Sử dụng tên file gốc làm public_id
                "unique_filename", true,
                "format", "pdf"
        ));

        // Lấy public ID của file đã upload
        String publicId = (String) uploadResult.get("public_id");

        // Lấy tên file gốc và chuẩn hóa (loại bỏ dấu, ký tự không hợp lệ)
        String customFilename = normalizeFilename(fileName);

        // URL-encode tên file (một lần encoding là đủ khi tên đã là ASCII)
        String encodedFilename = URLEncoder.encode(customFilename, "UTF-8");

        // Tạo URL bảo mật với transformation flag attachment:<custom_filename>
        // Khi tải file về, trình duyệt sẽ đặt tên file theo customFilename
        String secureUrl = cloudinary.url()
                .resourceType("raw")
                .publicId(publicId)
                .secure(true)
                .transformation(new Transformation().flags("attachment:" + customFilename))
                .generate();

        return secureUrl;
    }

    private String normalizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "file";
        }
        // Loại bỏ extension nếu có
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex != -1) {
            filename = filename.substring(0, dotIndex);
        }
        // Chuẩn hóa Unicode: tách dấu
        String normalized = Normalizer.normalize(filename, Normalizer.Form.NFD);
        // Loại bỏ dấu (diacritics)
        normalized = normalized.replaceAll("\\p{M}", "");
        // Giữ lại chữ, số, dấu gạch dưới, dấu gạch ngang, khoảng trắng và dấu chấm than
        normalized = normalized.replaceAll("[^\\w\\-\\s!]", "");
        // Chuyển khoảng trắng thành dấu gạch dưới và trim
        normalized = normalized.trim().replaceAll("\\s+", "_");
        return normalized;
    }

    @PutMapping(value = "/upload-signed-addenda-file/{addendumId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseObject> uploadPaymentBillUrls(@PathVariable long addendumId,
                                                                @RequestParam("files") List<MultipartFile> files) throws DataNotFoundException {
        addendumService.uploadSignedAddendum(addendumId, files);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .data(null)
                .message("Cập nhật các hình ảnh phụ lục đã kí")
                .build());
    }

    @GetMapping("/signed-addenda-urls/{addendumId}")
    public ResponseEntity<ResponseObject> getBillUrls(@PathVariable Long addendumId) throws DataNotFoundException {
        return ResponseEntity.ok(ResponseObject.builder()
                .message("Lấy link phụ lục đã ký")
                .status(HttpStatus.OK)
                .data(addendumService.getSignedAddendumUrl(addendumId))
                .build());
    }

//    private void logAuditTrail(Contract contract, String action, String fieldName, String oldValue, String newValue, String changedBy) {
//        String oldStatusVi = translateContractStatusToVietnamese(oldValue);
//        String newStatusVi = translateContractStatusToVietnamese(newValue);
//
//        AuditTrail auditTrail = AuditTrail.builder()
//                .contract(contract) // Liên kết với hợp đồng
//                .entityName("Addenda")
//                .entityId(contract.getId())
//                .action(action)
//                .fieldName(fieldName)
//                .oldValue(oldStatusVi)
//                .newValue(newStatusVi)
//                .changedBy(changedBy)
//                .changedAt(LocalDateTime.now())
//                .changeSummary(String.format("Phụ lục được ký bởi %s vào lúc %s. Trạng thái thay đổi từ '%s' sang '%s'",
//                        changedBy,
//                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").format(LocalDateTime.now()),
//                        oldStatusVi,
//                        newStatusVi))
//                .build();
//        auditTrailRepository.save(auditTrail);
//    }

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
                return status;
        }
    }

    @PostMapping("/upload-file-base64")
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

}

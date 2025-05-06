    package com.capstone.contractmanagement.controllers;

    import com.capstone.contractmanagement.components.SecurityUtils;
    import com.capstone.contractmanagement.dtos.FileBase64DTO;
    import com.capstone.contractmanagement.dtos.contract.*;
    import com.capstone.contractmanagement.entities.AuditTrail;
    import com.capstone.contractmanagement.entities.PaymentSchedule;
    import com.capstone.contractmanagement.entities.Role;
    import com.capstone.contractmanagement.entities.User;
    import com.capstone.contractmanagement.entities.contract.Contract;
    import com.capstone.contractmanagement.entities.contract.ContractAdditionalTermDetail;
    import com.capstone.contractmanagement.entities.contract.ContractTerm;
    import com.capstone.contractmanagement.enums.ContractStatus;
    import com.capstone.contractmanagement.exceptions.DataNotFoundException;
    import com.capstone.contractmanagement.exceptions.ResourceNotFoundException;
    import com.capstone.contractmanagement.repositories.IAuditTrailRepository;
    import com.capstone.contractmanagement.repositories.IContractRepository;
    import com.capstone.contractmanagement.responses.ResponseObject;
    import com.capstone.contractmanagement.responses.contract.CancelContractResponse;
    import com.capstone.contractmanagement.responses.contract.ContractLiquidationResponse;
    import com.capstone.contractmanagement.responses.contract.ContractResponse;
    import com.capstone.contractmanagement.responses.contract.GetAllContractReponse;
    import com.capstone.contractmanagement.services.contract.IContractService;
    import com.capstone.contractmanagement.services.file_process.IPdfSignatureLocatorService;
    import com.capstone.contractmanagement.services.file_process.PdfSignatureLocatorService;
    import com.capstone.contractmanagement.services.sendmails.IMailService;
    import com.capstone.contractmanagement.utils.MessageKeys;
    import com.cloudinary.Cloudinary;
    import com.cloudinary.Transformation;
    import com.cloudinary.utils.ObjectUtils;
    import jakarta.validation.Valid;
    import lombok.RequiredArgsConstructor;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.PageRequest;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.domain.Sort;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.MediaType;
    import org.springframework.http.ResponseEntity;
    import org.springframework.scheduling.annotation.Async;
    import org.springframework.security.access.prepost.PreAuthorize;
    import org.springframework.security.core.annotation.AuthenticationPrincipal;
    import org.springframework.transaction.annotation.Transactional;
    import org.springframework.web.bind.annotation.*;
    import org.springframework.web.multipart.MultipartFile;

    import java.io.File;
    import java.io.IOException;
    import java.net.URLEncoder;
    import java.nio.file.Files;
    import java.nio.file.Paths;
    import java.text.Normalizer;
    import java.time.LocalDateTime;
    import java.time.ZoneId;
    import java.time.ZonedDateTime;
    import java.time.format.DateTimeFormatter;
    import java.time.format.DateTimeParseException;
    import java.util.*;
    import java.util.stream.Collectors;

    @RestController
    @RequestMapping("${api.prefix}/contracts")
    @RequiredArgsConstructor

    public class ContractController {
        private final IContractService contractService;
        private final SecurityUtils securityUtils;
        private final IContractRepository contractRepository;
        private final IAuditTrailRepository auditTrailRepository;
        private final IPdfSignatureLocatorService signatureLocatorService;
        private final Cloudinary cloudinary;
        private final IMailService mailService;


        @PostMapping
        @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF')")
        @Transactional
        public ResponseEntity<ResponseObject> createContract(@Valid @RequestBody ContractDTO contractDTO) {
            try {
                Contract contract = contractService.createContractFromTemplate(contractDTO);
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(ResponseObject.builder()
                                .status(HttpStatus.CREATED)
                                .message(MessageKeys.CREATE_CONTRACT_SUCCESSFULLY)
                                .data(contract)
                                .build());

            } catch (IllegalArgumentException e) {
                // Lỗi dữ liệu đầu vào không hợp lệ (400)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseObject.builder()
                                .status(HttpStatus.BAD_REQUEST)
                                .message("Dữ liệu không hợp lệ: " + e.getMessage())
                                .data(null)
                                .build());

            } catch (DataNotFoundException e) {
                // Lỗi không tìm thấy dữ liệu (404)
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseObject.builder()
                                .status(HttpStatus.NOT_FOUND)
                                .message("Không tìm thấy dữ liệu: " + e.getMessage())
                                .data(null)
                                .build());

            } catch (IllegalStateException e) {
                // Lỗi trạng thái không hợp lệ (409)
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ResponseObject.builder()
                                .status(HttpStatus.CONFLICT)
                                .message("Trạng thái không hợp lệ: " + e.getMessage())
                                .data(null)
                                .build());

            } catch (Exception e) {
                // Lỗi server không xác định (500)
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ResponseObject.builder()
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .message("Lỗi hệ thống: " + e.getMessage())
                                .data(null)
                                .build());
            }
        }

        @GetMapping
        @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR', 'ROLE_ADMIN')")
        public ResponseEntity<ResponseObject> getAllContracts(
                @RequestParam(defaultValue = "0" ) int page,
                @RequestParam(defaultValue = "10" ) int size,
                @RequestParam(required = false) String keyword,
                @RequestParam(required = false) List<ContractStatus> statuses,  // Thay đổi thành danh sách
                @RequestParam(required = false) Long contractTypeId,
                @RequestParam(defaultValue = "id" ) String sortBy,
                @RequestParam(defaultValue = "asc" ) String order) {  // Thêm thông tin user hiện tại
            try {

                User currentUser = securityUtils.getLoggedInUser();
                Sort sort = order.equalsIgnoreCase("desc" )
                        ? Sort.by(sortBy).descending()
                        : Sort.by(sortBy).ascending();
                Pageable pageable = PageRequest.of(page, size, sort);

                // Truyền thêm currentUser vào service
                Page<GetAllContractReponse> contracts = contractService.getAllContracts(
                        pageable, keyword, statuses, contractTypeId, currentUser);

                return ResponseEntity.ok(ResponseObject.builder()
                        .message(MessageKeys.GET_ALL_CONTRACTS_SUCCESSFULLY)
                        .status(HttpStatus.OK)
                        .data(contracts)
                        .build());
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ResponseObject.builder()
                                .message("Error retrieving contracts: " + e.getMessage())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .data(null)
                                .build());
            }
        }

        @GetMapping("/{id}" )
        @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
        public ResponseEntity<ResponseObject> getContractById(@PathVariable Long id) throws DataNotFoundException {
            Optional<ContractResponse> contract = contractService.getContractById(id);
            if (contract.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseObject.builder()
                        .status(HttpStatus.NOT_FOUND)
                        .message(MessageKeys.CONTRACT_NOT_FOUND)
                        .data(null)
                        .build());
            }
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message(MessageKeys.GET_CONTRACT_SUCCESSFULLY)
                    .data(contract)
                    .build());
        }

        @DeleteMapping("/{id}" )
        @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
        public ResponseEntity<ResponseObject> deleteContract(@PathVariable Long id) {
            contractService.deleteContract(id);
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message(MessageKeys.DELETE_CONTRACT_SUCCESSFULLY)
                    .build());
        }

        @PostMapping("/{id}/duplicate" )
        @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF')")
        public ResponseEntity<ResponseObject> duplicateContract(@PathVariable Long id) {
            try {
                Contract duplicateContract = contractService.duplicateContract(id);
                if (duplicateContract != null) {
                    return ResponseEntity.ok(ResponseObject.builder()
                            .message(MessageKeys.DUPLICATE_CONTRACT_SUCCESSFULLY)
                            .status(HttpStatus.OK)
                            .data(duplicateContract)
                            .build());
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ResponseObject.builder()
                                    .message("Template not found with id: " + id)
                                    .status(HttpStatus.NOT_FOUND)
                                    .data(null)
                                    .build());
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ResponseObject.builder()
                                .message("Internal server error: " + e.getMessage())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .data(null)
                                .build());
            }
        }

        @PostMapping("/{id}/duplicate-with-partner" )
        @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF')")
        public ResponseEntity<ResponseObject> duplicateContractWithPartner(
                @PathVariable Long id,
                @RequestParam("partnerId" ) Long partnerId) {
            try {
                Contract duplicateContract = contractService.duplicateContractWithPartner(id, partnerId);
                if (duplicateContract != null) {
                    return ResponseEntity.ok(ResponseObject.builder()
                            .message(MessageKeys.DUPLICATE_CONTRACT_SUCCESSFULLY)
                            .status(HttpStatus.OK)
                            .data(duplicateContract)
                            .build());
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ResponseObject.builder()
                                    .message("Contract not found with id: " + id)
                                    .status(HttpStatus.NOT_FOUND)
                                    .data(null)
                                    .build());
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ResponseObject.builder()
                                .message("Internal server error: " + e.getMessage())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .data(null)
                                .build());
            }
        }

        @DeleteMapping("/soft-delete/{id}" )
        @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
        public ResponseEntity<ResponseObject> softDeleteContract(@PathVariable Long id) {
            try {
                boolean deleted = contractService.softDelete(id);
                if (deleted) {
                    return ResponseEntity.ok(ResponseObject.builder()
                            .message(MessageKeys.DELETE_CONTRACT_SUCCESSFULLY)
                            .status(HttpStatus.OK)
                            .data(null)
                            .build());
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ResponseObject.builder()
                                    .message("Template not found with id: " + id)
                                    .status(HttpStatus.NOT_FOUND)
                                    .data(null)
                                    .build());
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ResponseObject.builder()
                                .message("Internal server error: " + e.getMessage())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .data(null)
                                .build());
            }
        }

        @PutMapping("/status/{contractId}" )
        @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF')")
        public ResponseEntity<ResponseObject> updateContractStatus(
                @PathVariable Long contractId,
                @RequestParam ContractStatus status) {
            try {
                ContractStatus updatedContract = contractService.updateContractStatus(contractId, status);
                return ResponseEntity.ok(ResponseObject.builder()
                        .status(HttpStatus.OK)
                        .message(MessageKeys.UPDATE_CONTRACT_STATUS_SUCCESSFULLY)
                        .data(updatedContract)
                        .build());
            } catch (DataNotFoundException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseObject.builder()
                                .status(HttpStatus.NOT_FOUND)
                                .message(e.getMessage())
                                .data(null)
                                .build());
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ResponseObject.builder()
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .message("Error updating contract status: " + e.getMessage())
                                .data(null)
                                .build());
            }
        }

        @PutMapping("/update/{contractId}" )
        @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF')")
        public ResponseEntity<ResponseObject> updateContract(@PathVariable Long contractId, @RequestBody ContractUpdateDTO dto) {
            try {
                Contract contract = contractService.updateContract(contractId, dto);
                return ResponseEntity.ok(ResponseObject.builder()
                        .status(HttpStatus.OK)
                        .message(MessageKeys.UPDATE_CONTRACT_SUCCESSFULLY)
                        .data(contract)
                        .build());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(
                        new ResponseObject(e.getMessage(), HttpStatus.BAD_REQUEST, null)
                );
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(
                        new ResponseObject(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, null)
                );
            }
        }

        @PostMapping("/rollback" )
        @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF')")
        public ResponseEntity<ResponseObject> rollbackContract(@RequestParam Long originalContractId,
                                                               @RequestParam int version) {
            try {
                Contract rollbackContract = contractService.rollbackContract(originalContractId, version);
                return ResponseEntity.ok(ResponseObject.builder()
                        .status(HttpStatus.CREATED)
                        .message(MessageKeys.ROLLBACK_CONTRACT_SUCCESSFULLY)
                        .data(rollbackContract)
                        .build());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(
                        new ResponseObject(e.getMessage(), HttpStatus.BAD_REQUEST, null)
                );
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(
                        new ResponseObject(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, null)
                );
            }
        }

        @GetMapping("/original/{originalContractId}/versions" )
        @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR', 'ROLE_ADMIN')")
        public ResponseEntity<ResponseObject> getAllVersions(
                @PathVariable Long originalContractId,
                @RequestParam(defaultValue = "0" ) int page,
                @RequestParam(defaultValue = "10" ) int size,
                @RequestParam(defaultValue = "version" ) String sortBy,
                @RequestParam(defaultValue = "asc" ) String order) {
            try {
                User currentUser = securityUtils.getLoggedInUser();
                Sort sort = order.equalsIgnoreCase("desc" )
                        ? Sort.by(sortBy).descending()
                        : Sort.by(sortBy).ascending();
                Pageable pageable = PageRequest.of(page, size, sort);

                Page<GetAllContractReponse> versions = contractService.getAllVersionsByOriginalContractId(originalContractId, pageable, currentUser);

                return ResponseEntity.ok(ResponseObject.builder()
                        .message("Lấy tất cả phiên bản hợp đồng thành công" )
                        .status(HttpStatus.OK)
                        .data(versions)
                        .build());
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ResponseObject.builder()
                                .message("Lỗi khi lấy phiên bản hợp đồng: " + e.getMessage())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .data(null)
                                .build());
            }
        }

        @GetMapping("/compare-versions" )
        public ResponseEntity<List<ContractResponse>> getContractVersions(
                @RequestParam("originalContractId" ) Long originalContractId,
                @RequestParam("version1" ) Integer version1,
                @RequestParam("version2" ) Integer version2
        ) {
            List<ContractResponse> responses = contractService.getContractsByOriginalIdAndVersions(
                    originalContractId, version1, version2
            );
            return ResponseEntity.ok(responses);
        }

        @GetMapping("/partner/{partnerId}" )
        @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
        public ResponseEntity<ResponseObject> getAllContractsByPartnerId(
                @PathVariable Long partnerId,
                @RequestParam(defaultValue = "0" ) int page,
                @RequestParam(defaultValue = "10" ) int size,
                @RequestParam(required = false) String keyword,
                @RequestParam(required = false) ContractStatus status,
                @RequestParam(required = false) LocalDateTime signingDate,
                @RequestParam(defaultValue = "id" ) String sortBy,
                @RequestParam(defaultValue = "asc" ) String order) {
            try {
                Sort sort = order.equalsIgnoreCase("desc" )
                        ? Sort.by(sortBy).descending()
                        : Sort.by(sortBy).ascending();
                Pageable pageable = PageRequest.of(page, size, sort);

                Page<GetAllContractReponse> contracts = contractService.getAllContractsByPartnerId(
                        partnerId,
                        pageable,
                        keyword,
                        status,
                        signingDate
                );

                return ResponseEntity.ok(ResponseObject.builder()
                        .message("Lấy hợp đồng theo partner thành công" )
                        .status(HttpStatus.OK)
                        .data(contracts)
                        .build());
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ResponseObject.builder()
                                .message("Lỗi khi lấy hợp đồng theo partner: " + e.getMessage())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .build());
            }
        }

        @PostMapping("/sign")
        @PreAuthorize("hasAnyAuthority('ROLE_DIRECTOR')")
        public ResponseEntity<ResponseObject> signContract(@RequestBody @Valid SignContractRequest request) {
            try {
                return contractService.signContract(request);

            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ResponseObject.builder()
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .message("Lỗi hệ thống: " + e.getMessage())
                                .data(null)
                                .build());
            }
        }


        @PostMapping("/find-location/pdf")
        @PreAuthorize("hasAnyAuthority('ROLE_DIRECTOR')")
        public ResponseEntity<ResponseObject> locateSignature(@RequestParam("file") MultipartFile file) {
            try {
                PdfSignatureLocatorService.SignatureCoordinates coords = signatureLocatorService.findCoordinates(file.getInputStream());

                if (coords == null) {
                    return ResponseEntity.badRequest().body(ResponseObject.builder()
                            .message("Không tìm thấy 'ĐẠI DIỆN BÊN A' hoặc 'KÝ VÀ GHI RÕ HỌ TÊN' phù hợp trong file PDF.")
                            .status(HttpStatus.BAD_REQUEST)
                            .build());
                }

                return ResponseEntity.ok(ResponseObject.builder()
                        .message("Lấy tọa độ thành công.")
                        .data(coords)
                        .status(HttpStatus.OK)
                        .build());
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ResponseObject.builder()
                                .message("Lỗi server: " + e.getMessage())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .build());
            }


        }

        @PutMapping(value = "/upload-signed-contracts-file/{contractId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF')")
        public ResponseEntity<ResponseObject> uploadPaymentBillUrls(@PathVariable long contractId,
                                                                    @RequestParam("files") List<MultipartFile> files) throws DataNotFoundException {
            contractService.uploadSignedContract(contractId, files);
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .data(null)
                    .message("Cập nhật các hình ảnh hợp đồng đã kí")
                    .build());
        }

        @GetMapping("/signed-contract-urls/{contractId}")
        @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
        public ResponseEntity<ResponseObject> getBillUrls(@PathVariable Long contractId) throws DataNotFoundException {
            return ResponseEntity.ok(ResponseObject.builder()
                    .message("Lấy link hợp đồng đã ký")
                    .status(HttpStatus.OK)
                    .data(contractService.getSignedContractUrl(contractId))
                    .build());
        }

        @PostMapping("/upload-file-base64")
        @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
        public ResponseEntity<ResponseObject> uploadFileBase64(@RequestParam Long contractId,
                                                               @RequestBody FileBase64DTO fileBase64,
                                                               @RequestParam String fileName) throws DataNotFoundException, IOException {
            contractService.uploadSignedContractBase64(contractId, fileBase64, fileName);
            return ResponseEntity.ok(ResponseObject.builder()
                    .message("Tải lên thành công")
                    .status(HttpStatus.OK)
                    .data(null)
                    .build());
        }

        @PostMapping("/upload-file-base64get-count")
        @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
        public ResponseEntity<ResponseObject> uploadFileBase64GetCount(
                                                               @RequestBody FileBase64DTO fileBase64
                                                               ) throws IOException {
            return ResponseEntity.ok(ResponseObject.builder()
                    .message("Tải lên file lên thành công")
                    .status(HttpStatus.OK)
                    .data(signatureLocatorService.getPdfPageCountFromBase64(fileBase64.getFileBase64()))
                    .build());
        }

        @PostMapping("/send-approve-reminder/{contractId}")
        @PreAuthorize("hasAnyAuthority('ROLE_DIRECTOR')")
        public ResponseEntity<ResponseObject> sendApproveReminder( @PathVariable Long contractId
        ) throws DataNotFoundException {
            contractService.notifyNextApprover(contractId);
            return ResponseEntity.ok(ResponseObject.builder()
                    .message("Gửi thông báo nhắc nhở thành công")
                    .status(HttpStatus.OK)
                    .build());
        }

        @GetMapping("/get-contracts-nearly-expired")
        @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
        public ResponseEntity<ResponseObject> getContractsNearlyExpired(
                @RequestParam int days,
                @RequestParam(required = false) String keyword,
                @RequestParam(defaultValue = "0")  int page,
                @RequestParam(defaultValue = "10") int size
        ) {
            Page<GetAllContractReponse> resultPage =
                    contractService.getAllContractsNearlyExpiryDate(days, keyword, page, size);

            return ResponseEntity.ok(
                    ResponseObject.builder()
                            .message("Lấy danh sách hợp đồng sắp hết hạn thành công")
                            .status(HttpStatus.OK)
                            .data(resultPage)
                            .build()
            );
        }

        @PutMapping(value = "/cancel-contract/{contractId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @PreAuthorize("hasAnyAuthority('ROLE_STAFF', 'ROLE_DIRECTOR', 'ROLE_MANAGER')")
        public ResponseEntity<ResponseObject> cancelContract(@PathVariable Long contractId, @RequestPart(name = "files") List<MultipartFile> files, @RequestPart(name = "contractCancelDTO") ContractCancelDTO contractCancelDTO) throws DataNotFoundException {
            contractService.cancelContract(contractId, files, contractCancelDTO);
            return ResponseEntity.ok(ResponseObject.builder()
                    .message("Hủy hợp đồng thành công")
                    .status(HttpStatus.OK)
                    .build());
        }

        @GetMapping("/get-cancel-reason/{contractId}")
        @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
        public ResponseEntity<ResponseObject> getContractVersions(@PathVariable Long contractId) throws DataNotFoundException {
            CancelContractResponse responses = contractService.getContractCancelReason(contractId);
            return ResponseEntity.ok(ResponseObject.builder()
                    .message("lấy lí do hủy thành công")
                    .status(HttpStatus.OK)
                    .data(responses)
                    .build());
        }

        @PutMapping(value = "/liquidate-contract/{contractId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @PreAuthorize("hasAnyAuthority('ROLE_STAFF', 'ROLE_DIRECTOR', 'ROLE_MANAGER')")
        public ResponseEntity<ResponseObject> liquidateContract(@PathVariable Long contractId, @RequestPart(name = "files") List<MultipartFile> files, @RequestPart(name = "contractLiquidateDTO") ContractLiquidateDTO contractLiquidateDTO) throws DataNotFoundException {
            contractService.liquidateContract(contractId, files, contractLiquidateDTO);
            return ResponseEntity.ok(ResponseObject.builder()
                    .message("Thanh lý hợp đồng thành công")
                    .status(HttpStatus.OK)
                    .build());
        }

        @GetMapping("/get-liquidate-reason/{contractId}")
        @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
        public ResponseEntity<ResponseObject> getLiquidateReason(@PathVariable Long contractId) throws DataNotFoundException {
            ContractLiquidationResponse responses = contractService.getContractLiquidateReason(contractId);
            return ResponseEntity.ok(ResponseObject.builder()
                    .message("lấy lí do thành công")
                    .status(HttpStatus.OK)
                    .data(responses)
                    .build());
        }
    }


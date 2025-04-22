package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.dtos.contract_partner.PartnerContractDTO;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.services.contract_partner.IPartnerContractService;
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("${api.prefix}/partner-contracts")
@RequiredArgsConstructor
public class PartnerContractController {
    private final IPartnerContractService contractPartnerService;

    @PostMapping("/upload-contract-file")
    @PreAuthorize("hasAnyAuthority('ROLE_STAFF', 'ROLE_DIRECTOR')")
    public ResponseEntity<String> uploadCourseImage(@RequestParam("file") MultipartFile file) throws IOException {
        String result = contractPartnerService.uploadPdfToCloudinary(file);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyAuthority('ROLE_STAFF')")
    public ResponseEntity<ResponseObject> createContractPartner(@RequestBody PartnerContractDTO contractDTO) throws Exception {
        contractPartnerService.createContractPartner(contractDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseObject.builder()
                .status(HttpStatus.CREATED)
                .message(MessageKeys.CREATE_CONTRACT_PARTNER_SUCCESSFULLY)
                .build());
    }

    @GetMapping("/get-all")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF','ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> getAllContractPartners(@RequestParam(value = "search", required = false) String search,
                                                                 @RequestParam(value = "page", defaultValue = "0") int page,
                                                                 @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message(MessageKeys.GET_ALL_CONTRACT_PARTNERS_SUCCESSFULLY)
                .data(contractPartnerService.getAllContractPartners(search, page, size))
                .build());
    }

    @DeleteMapping("/delete/{contractPartnerId}")
    @PreAuthorize("hasAnyAuthority('ROLE_STAFF', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> deleteContractPartner(@PathVariable Long contractPartnerId) throws DataNotFoundException {
        contractPartnerService.deleteContractPartner(contractPartnerId);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message(MessageKeys.DELETE_CONTRACT_PARTNER_SUCCESSFULLY)
                .build());
    }

    @PutMapping("/update/{contractPartnerId}")
    @PreAuthorize("hasAnyAuthority('ROLE_STAFF')")
    public ResponseEntity<ResponseObject> updateContractPartner(@PathVariable Long contractPartnerId, @RequestBody PartnerContractDTO contractDTO) throws DataNotFoundException {
        contractPartnerService.updateContractPartner(contractPartnerId, contractDTO);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message(MessageKeys.UPDATE_CONTRACT_PARTNER_SUCCESSFULLY)
                .build());
    }

    @PutMapping(value = "/upload-bills/{paymentScheduleId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF')")
    public ResponseEntity<ResponseObject> uploadPaymentBillUrls(@PathVariable long paymentScheduleId,
                                                                @RequestParam("files") List<MultipartFile> files) throws DataNotFoundException {
        contractPartnerService.uploadPaymentBillUrls(paymentScheduleId, files);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .data(null)
                .message("Cập nhật các hóa đơn thanh toán thành công")
                .build());
    }

}

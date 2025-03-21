package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.dtos.contract_partner.ContractPartnerDTO;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.services.contract_partner.IContractPartnerService;
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("${api.prefix}/contract-partners")
@RequiredArgsConstructor
public class ContractPartnerController {
    private final IContractPartnerService contractPartnerService;

    @PostMapping("/upload-contract-file")
    public ResponseEntity<String> uploadCourseImage(@RequestParam("file") MultipartFile file) throws IOException {
        String result = contractPartnerService.uploadPdfToCloudinary(file);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/create")
    public ResponseEntity<ResponseObject> createContractPartner(@RequestBody ContractPartnerDTO contractDTO) throws Exception {
        contractPartnerService.createContractPartner(contractDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseObject.builder()
                .status(HttpStatus.CREATED)
                .message(MessageKeys.CREATE_CONTRACT_PARTNER_SUCCESSFULLY)
                .build());
    }

    @GetMapping("/get-all")
    public ResponseEntity<ResponseObject> getAllContractPartners() {
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message(MessageKeys.GET_ALL_CONTRACT_PARTNERS_SUCCESSFULLY)
                .data(contractPartnerService.getAllContractPartners())
                .build());
    }
}

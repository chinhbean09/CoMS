package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.dtos.contract.ContractDTO;
import com.capstone.contractmanagement.dtos.contract.ContractUpdateDTO;
import com.capstone.contractmanagement.dtos.contract_template.ContractTemplateDTO;
import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.entities.contract_template.ContractTemplate;
import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.responses.contract.ContractResponse;
import com.capstone.contractmanagement.responses.contract.GetAllContractReponse;
import com.capstone.contractmanagement.responses.template.ContractTemplateResponse;
import com.capstone.contractmanagement.services.contract.IContractService;
import com.capstone.contractmanagement.utils.MessageKeys;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("${api.prefix}/contracts")
@RequiredArgsConstructor

public class ContractController {
    private final IContractService contractService;

    @PostMapping
    @Transactional
    public ResponseEntity<ResponseObject> createContract(@Valid @RequestBody ContractDTO contractDTO) throws DataNotFoundException {
        Contract contract = contractService.createContractFromTemplate(contractDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseObject.builder()
                .status(HttpStatus.CREATED)
                .message(MessageKeys.CREATE_CONTRACT_SUCCESSFULLY)
                .data(contract)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseObject> getContractById(@PathVariable Long id) throws DataNotFoundException {
        Optional<ContractResponse> contract = contractService.getContractById(id);
        if(contract.isEmpty()) {
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

    @GetMapping
    public ResponseEntity<ResponseObject> getAllContracts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String order) {
        try {
            Sort sort = order.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<GetAllContractReponse> contracts = contractService.getAllContracts(pageable, keyword, status);
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


    @PutMapping("/update/{id}")
    public ResponseEntity<ResponseObject> updateContract(
            @PathVariable Long id,
            @Valid @RequestBody ContractDTO contractDTO) {
        ContractResponse contract = contractService.updateContract(id, contractDTO);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message(MessageKeys.UPDATE_CONTRACT_SUCCESSFULLY)
                .data(contract)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseObject> deleteContract(@PathVariable Long id) {
        contractService.deleteContract(id);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message(MessageKeys.DELETE_CONTRACT_SUCCESSFULLY)
                .build());
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<ResponseObject> duplicateContract(@PathVariable Long id) {
        try {
         Contract duplicateContract = contractService.duplicateContract(id);
            if (duplicateContract!=null) {
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

    @DeleteMapping("/soft-delete/{id}")
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

    @PutMapping("/{id}/status")
    public ResponseEntity<ResponseObject> updateContractStatus(
            @PathVariable Long id,
            @RequestParam ContractStatus status) {
        try {
            ContractStatus updatedContract = contractService.updateContractStatus(id, status);
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

    @PostMapping("/update/{contractId}")
    public ResponseEntity<ResponseObject> updateTemplate(@PathVariable Long contractId, @RequestBody ContractUpdateDTO dto) {
        try {
            Contract contract = contractService.updateContract(contractId, dto);
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.CREATED)
                    .message(MessageKeys.UPDATE_TEMPLATE_SUCCESSFULLY)
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


}

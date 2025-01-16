package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.dtos.contract.ContractDTO;
import com.capstone.contractmanagement.entities.Contract;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.responses.contract.ContractResponse;
import com.capstone.contractmanagement.services.contract.IContractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/contracts")
@RequiredArgsConstructor

public class ContractController {
    private final IContractService contractService;

    @GetMapping
    public ResponseEntity<ResponseObject> getAllContracts() {
        List<ContractResponse> contracts = contractService.getAllContracts();
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Fetched all contracts successfully")
                .data(contracts)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseObject> getContractById(@PathVariable Long id) {
        ContractResponse contract = contractService.getContractById(id);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Fetched contract successfully")
                .data(contract)
                .build());
    }

    @PostMapping
    public ResponseEntity<ResponseObject> createContract(@Valid @RequestBody ContractDTO contractDTO) throws DataNotFoundException {
        Contract contract = contractService.createContract(contractDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseObject.builder()
                .status(HttpStatus.CREATED)
                .message("Created contract successfully")
                .data(contract)
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseObject> updateContract(
            @PathVariable Long id,
            @Valid @RequestBody ContractDTO contractDTO) {
        ContractResponse contract = contractService.updateContract(id, contractDTO);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Updated contract successfully")
                .data(contract)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseObject> deleteContract(@PathVariable Long id) {
        contractService.deleteContract(id);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Deleted contract successfully")
                .build());
    }

}

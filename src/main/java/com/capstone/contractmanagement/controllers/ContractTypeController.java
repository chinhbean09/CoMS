package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.entities.ContractType;
import com.capstone.contractmanagement.services.contract_type.IContractTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/contract-types")
@RequiredArgsConstructor

public class ContractTypeController {
    private final IContractTypeService contractTypeService;
    @GetMapping
    public List<ContractType> getAllContractTypes() {
        return contractTypeService.findAll();
    }

    @GetMapping("/{id}")
    public ContractType getContractTypeById(@PathVariable Long id) {
        return contractTypeService.findById(id);
    }

    @PostMapping
    public ContractType createContractType(@RequestBody ContractType contractType) {
        return contractTypeService.save(contractType);
    }

    @PutMapping("/{id}")
    public ContractType updateContractType(@PathVariable Long id, @RequestBody ContractType contractType) {
        return contractTypeService.update(id, contractType);
    }

    @DeleteMapping("/{id}")
    public void deleteContractType(@PathVariable Long id) {
        contractTypeService.delete(id);
    }
}

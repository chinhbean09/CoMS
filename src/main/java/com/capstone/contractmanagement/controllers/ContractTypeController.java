package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.dtos.contract_type.ContractTypeDTO;
import com.capstone.contractmanagement.entities.ContractType;
import com.capstone.contractmanagement.repositories.IContractTypeRepository;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.services.contract_type.IContractTypeService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("${api.prefix}/contract-types")
@RequiredArgsConstructor

public class ContractTypeController {
    private final IContractTypeService contractTypeService;
    private final IContractTypeRepository contractTypeRepository;

    @GetMapping
    public ResponseEntity<List<ContractTypeDTO>> getAllContractTypes() {
        List<ContractTypeDTO> contractTypes = contractTypeService.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        Collections.reverse(contractTypes);
        return ResponseEntity.ok(contractTypes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContractTypeDTO> getContractTypeById(@PathVariable Long id) {
        Optional<ContractType> contractType = contractTypeService.findById(id);
        return contractType.map(type -> ResponseEntity.ok(convertToDTO(type)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    @PostMapping
    @Transactional
    public ResponseEntity<?> createContractType(@RequestBody ContractTypeDTO contractTypeDTO) {
        try {
            if (contractTypeRepository.existsByName(contractTypeDTO.getName())) {
                throw new IllegalArgumentException("Tên loại hợp đồng đã tồn tại, vui lòng chọn tên khác!");
            }
            ContractType contractType = convertToEntity(contractTypeDTO);
            ContractType savedType = contractTypeService.save(contractType);
            return ResponseEntity.ok(convertToDTO(savedType));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateContractType(@PathVariable Long id, @RequestBody ContractTypeDTO contractTypeDTO) {
        try {
            ContractType contractType = convertToEntity(contractTypeDTO);
            ContractType updatedType = contractTypeService.update(id, contractType);
            return ResponseEntity.ok(convertToDTO(updatedType));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteContractType(@PathVariable Long id) {
        try {
            contractTypeService.delete(id);
            return ResponseEntity.ok(" successfully");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy hợp đồng có ID: " + id);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    private ContractTypeDTO convertToDTO(ContractType contractType) {
        ContractTypeDTO dto = new ContractTypeDTO();
        dto.setId(contractType.getId());
        dto.setName(contractType.getName());
        return dto;
    }

    private ContractType convertToEntity(ContractTypeDTO dto) {
        ContractType contractType = new ContractType();
        contractType.setId(dto.getId());
        contractType.setName(dto.getName());
        return contractType;
    }

    @PatchMapping("/{id}/delete-status")
    public ResponseEntity<ResponseObject> updateDeleteStatus(
            @PathVariable Long id,
            @RequestParam Boolean isDeleted) {
        try {
            contractTypeService.updateDeleteStatus(id, isDeleted);
            String message = isDeleted ? "Contract type soft deleted successfully." : "Contract type restored successfully.";
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ResponseObject.builder()
                            .message(message)
                            .status(HttpStatus.OK)
                            .data(null)
                            .build());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseObject.builder()
                            .message(e.getMessage())
                            .status(HttpStatus.NOT_FOUND)
                            .data(null)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseObject.builder()
                            .message("Internal server error: " + e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .data(null)
                            .build());
        }
    }

}

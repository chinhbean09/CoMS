package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.dtos.contract_template.ContractTemplateDTO;
import com.capstone.contractmanagement.dtos.contract_template.ContractTemplateIdDTO;
import com.capstone.contractmanagement.entities.contract_template.ContractTemplate;
import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.enums.ContractTemplateStatus;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.exceptions.ResourceNotFoundException;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.responses.template.*;
import com.capstone.contractmanagement.services.template.IContractTemplateService;
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("${api.prefix}/templates")
@RequiredArgsConstructor
public class ContractTemplateController {

    private final IContractTemplateService templateService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> createTemplate(@RequestBody ContractTemplateDTO dto) {
        try {
            ContractTemplate template = templateService.createTemplate(dto);
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.CREATED)
                    .message(MessageKeys.CREATE_TEMPLATE_SUCCESSFULLY)
                    .data(template)
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

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_ADMIN', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> getTemplateById(@PathVariable Long id) {
        try {
            Optional<ContractTemplateResponse> templateOpt = templateService.getTemplateById(id);
            if (templateOpt.isPresent()) {
                return ResponseEntity.ok(ResponseObject.builder()
                        .message(MessageKeys.GET_TEMPLATE_SUCCESSFULLY)
                        .status(HttpStatus.OK)
                        .data(templateOpt.get())
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


    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_ADMIN', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> getAllTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long contractTypeId,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String order) {
        try {
            Sort sort = order.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<ContractTemplateSimpleResponse> templates = templateService.getAllTemplates(pageable, keyword, status, contractTypeId);
            return ResponseEntity.ok(ResponseObject.builder()
                    .message(MessageKeys.GET_ALL_TEMPLATES_SUCCESSFULLY)
                    .status(HttpStatus.OK)
                    .data(templates)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseObject.builder()
                            .message("Error retrieving templates: " + e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .data(null)
                            .build());
        }
    }


    @GetMapping("/titles")
    public ResponseEntity<ResponseObject> getAllTemplateTitles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String order) {
        try {
            Sort sort = order.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<ContractTemplateTitleResponse> pageTemplates = templateService.getAllTemplateTitles(pageable);

            return ResponseEntity.ok(ResponseObject.builder()
                    .message(MessageKeys.GET_ALL_TEMPLATES_SUCCESSFULLY)
                    .status(HttpStatus.OK)
                    .data(pageTemplates)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseObject.builder()
                            .message("Error retrieving templates: " + e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .data(null)
                            .build());
        }
    }

    @GetMapping("/{id}/ids")
    public ResponseEntity<ResponseObject> getTemplateIdsById(@PathVariable Long id) {
        try {
            Optional<ContractTemplateResponseIds> templateOpt = templateService.getTemplateIdsById(id);
            if (templateOpt.isPresent()) {
                return ResponseEntity.ok(ResponseObject.builder()
                        .message(MessageKeys.GET_TEMPLATE_SUCCESSFULLY)
                        .status(HttpStatus.OK)
                        .data(templateOpt.get())
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


    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseObject> deleteTemplate(@PathVariable Long id) {
        try {
            templateService.deleteTemplate(id);
            return ResponseEntity.ok(new ResponseObject(MessageKeys.DELETE_TEMPLATE_SUCCESSFULLY, HttpStatus.OK, null));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ResponseObject(e.getMessage(), HttpStatus.NOT_FOUND, null)
            );
        }
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<ResponseObject> duplicateTemplate(@PathVariable Long id) {
        try {
            Optional<ContractTemplateResponse> duplicateTemplateOpt = templateService.duplicateTemplate(id);
            if (duplicateTemplateOpt.isPresent()) {
                return ResponseEntity.ok(ResponseObject.builder()
                        .message(MessageKeys.DUPLICATE_TEMPLATE_SUCCESSFULLY)
                        .status(HttpStatus.OK)
                        .data(duplicateTemplateOpt.get())
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

    @PutMapping("/update/{templateId}")
    public ResponseEntity<ResponseObject> updateTemplate(@PathVariable Long templateId, @RequestBody ContractTemplateDTO dto) {
        try {
            ContractTemplate template = templateService.updateTemplate(templateId, dto);
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.CREATED)
                    .message(MessageKeys.UPDATE_TEMPLATE_SUCCESSFULLY)
                    .data(template)
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

    @DeleteMapping("/soft-delete/{id}")
    public ResponseEntity<ResponseObject> softDeleteContractTemplate(@PathVariable Long id) {
        try {
            boolean deleted = templateService.softDelete(id);
            if (deleted) {
                return ResponseEntity.ok(ResponseObject.builder()
                        .message(MessageKeys.DELETE_TEMPLATE_SUCCESSFULLY)
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
            @RequestParam ContractTemplateStatus status) {
        try {
            ContractTemplateStatus updatedContract = templateService.updateContractStatus(id, status);
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

    @GetMapping("/by-contract-type/{contractTypeId}")
    public ResponseEntity<Page<ContractTemplateIdDTO>> getTemplatesByContractType(
            @PathVariable Long contractTypeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String order) {

        Sort sort = order.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ContractTemplateIdDTO> templates = templateService.getTemplatesByContractType(contractTypeId, pageable);
        return ResponseEntity.ok(templates);
    }

}
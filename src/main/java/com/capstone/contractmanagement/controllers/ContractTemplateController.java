package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.dtos.template.ContractTemplateDTO;
import com.capstone.contractmanagement.entities.ContractTemplate;
import com.capstone.contractmanagement.exceptions.ResourceNotFoundException;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.services.template.IContractTemplateService;
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
                    new ResponseObject(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR, null)
            );
        }
    }

    @GetMapping
    public ResponseEntity<ResponseObject> getAllTemplates(
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        try {
            Page<ContractTemplate> templates = templateService.getAllTemplates(pageable);
            return ResponseEntity.ok(
                    new ResponseObject(MessageKeys.GET_ALL_TEMPLATES_SUCCESSFULLY, HttpStatus.OK, templates)
            );
        } catch (Exception e) {
            // Log lỗi nếu cần, ví dụ: logger.error("Error retrieving templates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseObject("Error retrieving templates: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR, null));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseObject> getTemplateById(@PathVariable Long id) {
        try {
            Optional<ContractTemplate> template = templateService.getTemplateById(id);
            return ResponseEntity.ok(new ResponseObject(MessageKeys.GET_TEMPLATE_SUCCESSFULLY,HttpStatus.OK, template));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ResponseObject(e.getMessage(),HttpStatus.NOT_FOUND, null)
            );
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseObject> deleteTemplate(@PathVariable Long id) {
        try {
            templateService.deleteTemplate(id);
            return ResponseEntity.ok(new ResponseObject(MessageKeys.DELETE_TEMPLATE_SUCCESSFULLY,HttpStatus.OK, null));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ResponseObject( e.getMessage(),HttpStatus.NOT_FOUND, null)
            );
        }
    }


}

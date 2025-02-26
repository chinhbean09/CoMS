package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.dtos.template.ContractTemplateDTO;
import com.capstone.contractmanagement.dtos.term.TermSimpleDTO;
import com.capstone.contractmanagement.entities.ContractTemplate;
import com.capstone.contractmanagement.exceptions.ResourceNotFoundException;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.responses.template.ContractTemplateAdditionalTermDetailResponse;
import com.capstone.contractmanagement.responses.template.ContractTemplateResponse;
import com.capstone.contractmanagement.responses.template.ContractTemplateSimpleResponse;
import com.capstone.contractmanagement.responses.term.TermResponse;
import com.capstone.contractmanagement.services.template.IContractTemplateService;
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String order) {
        try {
            Sort sort = order.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<ContractTemplateSimpleResponse> templates = templateService.getAllTemplates(pageable,keyword);
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


    @GetMapping("/{id}")
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

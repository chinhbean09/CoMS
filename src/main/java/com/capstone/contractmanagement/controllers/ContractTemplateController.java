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
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String order) {
        try {
            Sort sort = order.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<ContractTemplateSimpleResponse> templates = templateService.getAllTemplates(pageable);
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

//    private ContractTemplateResponse convertToResponseDTO(ContractTemplate template) {
//        List<TermResponse> legalBasisTerms = template.getLegalBasisTerms().stream()
//                .map(term -> TermResponse.builder()
//                        .id(term.getId())
//                        .label(term.getLabel())
//                        .value(term.getValue())
//                        .build())
//                .collect(Collectors.toList());
//
//        List<TermResponse> generalTerms = template.getGeneralTerms().stream()
//                .map(term -> TermResponse.builder()
//                        .id(term.getId())
//                        .label(term.getLabel())
//                        .value(term.getValue())
//                        .build())
//                .collect(Collectors.toList());
//
//        List<TermResponse> otherTerms = template.getOtherTerms().stream()
//                .map(term -> TermResponse.builder()
//                        .id(term.getId())
//                        .label(term.getLabel())
//                        .value(term.getValue())
//                        .build())
//                .collect(Collectors.toList());
//
//        List<TermResponse> additionalTerms = template.getAdditionalTerms().stream()
//                .map(term -> TermResponse.builder()
//                        .id(term.getId())
//                        .label(term.getLabel())
//                        .value(term.getValue())
//                        .build())
//                .collect(Collectors.toList());
//
//        // Chuyển đổi additionalTermConfigs
//        List<ContractTemplateAdditionalTermDetailResponse> additionalTermConfigs = template.getAdditionalTermConfigs().stream()
//                .map(config -> ContractTemplateAdditionalTermDetailResponse.builder()
//                        .typeTermId(config.getTypeTermId())
//                        .commonTermIds(config.getCommonTermIds())
//                        .aTermIds(config.getATermIds())  // Giả sử getter là getATermIds()
//                        .bTermIds(config.getBTermIds())  // Giả sử getter là getBTermIds()
//                        .build())
//                .collect(Collectors.toList());
//
//        return ContractTemplateResponse.builder()
//                .id(template.getId())
//                .contractTitle(template.getContractTitle())
//                .partyInfo(template.getPartyInfo())
//                .specialTermsA(template.getSpecialTermsA())
//                .specialTermsB(template.getSpecialTermsB())
//                .appendixEnabled(template.getAppendixEnabled())
//                .transferEnabled(template.getTransferEnabled())
//                .createdAt(template.getCreatedAt())
//                .updatedAt(template.getUpdatedAt())
//                .violate(template.getViolate())
//                .suspend(template.getSuspend())
//                .suspendContent(template.getSuspendContent())
//                .contractContent(template.getContractContent())
//                .autoAddVAT(template.getAutoAddVAT())
//                .vatPercentage(template.getVatPercentage())
//                .isDateLateChecked(template.getIsDateLateChecked())
//                .maxDateLate(template.getMaxDateLate())
//                .autoRenew(template.getAutoRenew())
//                .legalBasisTerms(legalBasisTerms)
//                .generalTerms(generalTerms)
//                .otherTerms(otherTerms)
//                .additionalTerms(additionalTerms)
//                .contractTypeId(template.getContractType() != null ? template.getContractType().getId() : null)
//                .additionalTermConfigs(additionalTermConfigs)
//                .build();
//    }



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

package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.dtos.term.*;
import com.capstone.contractmanagement.entities.term.Term;
import com.capstone.contractmanagement.entities.term.TypeTerm;
import com.capstone.contractmanagement.enums.TypeTermIdentifier;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.ITermRepository;
import com.capstone.contractmanagement.repositories.ITypeTermRepository;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.responses.term.CreateTermResponse;
import com.capstone.contractmanagement.responses.term.GetAllTermsResponse;
import com.capstone.contractmanagement.responses.term.GetAllTermsResponseLessField;
import com.capstone.contractmanagement.responses.term.TypeTermResponse;
import com.capstone.contractmanagement.services.term.ITermService;
import com.capstone.contractmanagement.utils.MessageKeys;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("${api.prefix}/terms")
@RequiredArgsConstructor
public class TermController {

    private final ITermService termService;
    private final ITermRepository termRepository;
    private final ITypeTermRepository typeTermRepository;

    @PostMapping("/create/{typeTermId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR', 'ROLE_ADMIN')")
    public ResponseObject createTerm(@PathVariable Long typeTermId, @RequestBody CreateTermDTO termRequest) {
        CreateTermResponse termResponse = termService.createTerm(typeTermId, termRequest);
        return ResponseObject.builder()
                .message(MessageKeys.CREATE_TERM_SUCCESSFULLY)
                .data(termResponse)
                .status(HttpStatus.CREATED)
                .build();
    }

    @PostMapping("/create-type-term")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR', 'ROLE_ADMIN')")
    public ResponseObject createTypeTerm(@RequestBody CreateTypeTermDTO request) {
        TypeTerm typeTerm = termService.createTypeTerm(request);
                 return ResponseObject.builder()
                .message(MessageKeys.CREATE_TYPE_TERM_SUCCESSFULLY)
                .data(typeTerm)
                .status(HttpStatus.CREATED)
                .build();
    }

    @PutMapping("/update/{termId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR','ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> updateTerm(@PathVariable Long termId, @RequestBody UpdateTermDTO termRequest) throws DataNotFoundException {
        CreateTermResponse termResponse = termService.updateTerm(termId, termRequest);
        return ResponseEntity.status(HttpStatus.OK).body(ResponseObject.builder()
                .message(MessageKeys.UPDATE_TERM_SUCCESSFULLY)
                .data(termResponse)
                .status(HttpStatus.OK).build());
    }

    @GetMapping("/get-all")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR', 'ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> getAllTerms(
            @RequestParam(required = false) List<Long> typeTermIds,
            @RequestParam(defaultValue = "false") boolean includeLegalBasis,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0", required = false) int page,
            @RequestParam(defaultValue = "10", required = false) int size,
            @RequestParam(defaultValue = "id", required = false) String sortBy,
            @RequestParam(defaultValue = "asc", required = false) String order) {
        try {
            Sort sort = order.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<GetAllTermsResponse> termResponses = termService.getAllTerms(typeTermIds, includeLegalBasis,keyword, pageable);
            return ResponseEntity.status(HttpStatus.OK).body(ResponseObject.builder()
                    .message(MessageKeys.GET_ALL_TERMS_SUCCESSFULLY)
                    .data(termResponses)
                    .status(HttpStatus.OK)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ResponseObject.builder()
                            .message(e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .data(null)
                            .build());
        }
    }

    @GetMapping("/get-all-less-field")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR', 'ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> getAllTermsLessField(
            @RequestParam(required = false) List<Long> typeTermIds,
            @RequestParam(defaultValue = "false") boolean includeLegalBasis,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0", required = false) int page,
            @RequestParam(defaultValue = "10", required = false) int size,
            @RequestParam(defaultValue = "id", required = false) String sortBy,
            @RequestParam(defaultValue = "asc", required = false) String order) {
        try {
            Sort sort = order.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<GetAllTermsResponseLessField> termResponses = termService.getAllTermsLessField(typeTermIds, includeLegalBasis,keyword, pageable);
            return ResponseEntity.status(HttpStatus.OK).body(ResponseObject.builder()
                    .message(MessageKeys.GET_ALL_TERMS_SUCCESSFULLY)
                    .data(termResponses)
                    .status(HttpStatus.OK)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ResponseObject.builder()
                            .message(e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .data(null)
                            .build());
        }
    }


    @GetMapping("/get-by-id/{termId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR', 'ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> getTermById(@PathVariable Long termId) throws DataNotFoundException {
        CreateTermResponse termResponse = termService.getTermById(termId);
        return ResponseEntity.status(HttpStatus.OK).body(ResponseObject.builder()
                .message(MessageKeys.GET_TERM_SUCCESSFULLY)
                .data(termResponse)
                .status(HttpStatus.OK).build());
    }

    @DeleteMapping("/delete/{termId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR', 'ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> deleteTerm(@PathVariable Long termId) throws DataNotFoundException {
        termService.deleteTerm(termId);
        return ResponseEntity.status(HttpStatus.OK).body(ResponseObject.builder()
                .message(MessageKeys.DELETE_TERM_SUCCESSFULLY)
                .status(HttpStatus.NO_CONTENT)
                .build());// Trả về mã 204 (No Content)
    }


    // update type term
    @PutMapping("/update-type-term/{typeTermId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR', 'ROLE_ADMIN')")
    public ResponseEntity<String> updateTypeTerm(@PathVariable Long typeTermId, @RequestBody UpdateTypeTermDTO request) {
        return ResponseEntity.ok(termService.updateTypeTerm(typeTermId, request));
    }

    // delete type term
    @DeleteMapping("/delete-type-term/{typeTermId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR', 'ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> deleteTypeTerm(@PathVariable Long typeTermId) {
        termService.deleteTypeTerm(typeTermId);
        return ResponseEntity.status(HttpStatus.OK).body(ResponseObject.builder()
                .message(MessageKeys.DELETE_TERM_SUCCESSFULLY)
                .status(HttpStatus.NO_CONTENT)
                .build());// Trả về mã 204 (No Content)
    }

    @GetMapping("/get-all-type-terms")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR', 'ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> getAllTypeTerms( ) {
        List<TypeTermResponse> typeTerms = termService.getAllTypeTerms();

//        if (lang != null && !lang.equalsIgnoreCase("vi")) {
//            typeTerms.forEach(typeTerm -> {
//                String translatedName = translationService.translateText(typeTerm.getName(), lang);
//                typeTerm.setName(translatedName);
//            });
//        }
        return ResponseEntity.status(HttpStatus.OK).body(ResponseObject.builder()
                .message(MessageKeys.GET_ALL_TYPE_TERMS_SUCCESSFULLY)
                .data(typeTerms)
                .status(HttpStatus.OK)
                .build());
    }

    @GetMapping("/get-type-term-by-id/{typeTermId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR', 'ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> getTypeTermById(@PathVariable Long typeTermId) {
        TypeTermResponse typeTermResponse = termService.getTypeTermById(typeTermId);
        return ResponseEntity.status(HttpStatus.OK).body(ResponseObject.builder()
                .message(MessageKeys.GET_TYPE_TERM_SUCCESSFULLY)
                .data(typeTermResponse)
                .status(HttpStatus.OK).build());
    }

    // get all terms by type term id
    @GetMapping("/get-terms-by-type-term-id/{typeTermId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR', 'ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> getTermsByTypeTermId(@PathVariable Long typeTermId,
                                                               @RequestParam(required = false) String keyword,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        Page<GetAllTermsResponse> terms = termService.getAllTermsByTypeTermId(typeTermId, keyword, page, size);
        return ResponseEntity.status(HttpStatus.OK).body(ResponseObject.builder()
                .message(MessageKeys.GET_ALL_TERMS_SUCCESSFULLY)
                .data(terms)
                .status(HttpStatus.OK).build());
    }

    @PutMapping("/update-status/{termId}/{isDeleted}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR', 'ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> updateTermStatus(@PathVariable Long termId, @PathVariable Boolean isDeleted) {
        try {
            termService.updateTermStatus(termId, true);
            return ResponseEntity.ok(
                    ResponseObject.builder()
                            .message("Xóa điều khoản thành công")
                            .status(HttpStatus.OK)
                            .data(null)
                            .build());
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseObject.builder()
                            .message(e.getMessage())
                            .status(HttpStatus.NOT_FOUND)
                            .data(null)
                            .build());

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ResponseObject.builder()
                            .message(e.getMessage())
                            .status(HttpStatus.CONFLICT)
                            .data(null)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseObject.builder()
                            .message(e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .data(null)
                            .build());
        }

    }

    @GetMapping("/legal-basis")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR', 'ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> getAllLegalBasisTerms() {
        try {
            List<Term> legalBasisTerms = termRepository.findAllLegalBasisTerms();
            List<GetAllTermsResponse> dtos = legalBasisTerms.stream()
                    .map(term -> GetAllTermsResponse.builder()
                            .id(term.getId())
                            .clauseCode(term.getClauseCode())
                            .label(term.getLabel())
                            .value(term.getValue())
                            .type(term.getTypeTerm().getName())
                            .identifier(term.getTypeTerm().getIdentifier().name())
                            .createdAt(term.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());

            return ResponseEntity.ok(
                    ResponseObject.builder()
                            .message("Get all LEGAL_BASIS terms successfully")
                            .status(HttpStatus.OK)
                            .data(dtos)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseObject.builder()
                            .message("Internal server error: " + e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .data(null)
                            .build());
        }
    }

    @GetMapping("/additional")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR', 'ROLE_ADMIN')")
    public ResponseEntity<List<TypeTermResponse>> getAdditionalTypeTerms() {
        List<TypeTerm> typeTerms = typeTermRepository.findByIdentifier(TypeTermIdentifier.ADDITIONAL_TERMS);
        List<TypeTermResponse> responses = typeTerms.stream()
                .map(tt -> TypeTermResponse.builder()
                        .id(tt.getId())
                        .name(tt.getName())
                        .identifier(tt.getIdentifier())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.OK).body(responses);
    }

    @PostMapping("/batch-create")
    public ResponseEntity<ResponseObject> batchCreateTerms(
            @Valid @RequestBody List<BatchCreateTermDTO> dtos
    ) {
        try {
            List<CreateTermResponse> responses = termService.batchCreateTerms(dtos);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ResponseObject.builder()
                            .message(MessageKeys.CREATE_TERM_SUCCESSFULLY)
                            .data(responses)
                            .status(HttpStatus.CREATED)
                            .build()
            );
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ResponseObject.builder()
                            .message(e.getMessage())
                            .status(HttpStatus.BAD_REQUEST)
                            .build()
            );
        }
    }

    @PostMapping("/import-file-excel")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR', 'ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> importTermsFromExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("typeTermId") Long typeTermId) throws IOException {

        // Kiểm tra và nhập khẩu các điều khoản từ file Excel
        List<CreateTermResponse> termResponses = termService.importTermsFromExcel(file, typeTermId);

        // Trả về danh sách các điều khoản đã nhập khẩu thành công
        return ResponseEntity.ok(ResponseObject.builder()
                .message("Import file excel để tạo điều khoản")
                .status(HttpStatus.CREATED)
                .data(termResponses)
                .build());
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR', 'ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> searchTerms(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<CreateTermResponse> termsPage = termService.searchTerm(keyword, page, size);
        return ResponseEntity.status(HttpStatus.OK).body(ResponseObject.builder()
                .message(MessageKeys.GET_ALL_TERMS_SUCCESSFULLY)
                .data(termsPage)
                .status(HttpStatus.OK).build());
    }

    @GetMapping("/get-all-by-user")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR', 'ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> getAllTermsByUser(
            @RequestParam(required = false) List<Long> typeTermIds,
            @RequestParam(defaultValue = "false") boolean includeLegalBasis,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0", required = false) int page,
            @RequestParam(defaultValue = "10", required = false) int size,
            @RequestParam(defaultValue = "id", required = false) String sortBy,
            @RequestParam(defaultValue = "asc", required = false) String order) {
        try {
            Sort sort = order.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<GetAllTermsResponse> termResponses = termService.getAllTermsByUser(typeTermIds, includeLegalBasis,keyword, pageable);
            return ResponseEntity.status(HttpStatus.OK).body(ResponseObject.builder()
                    .message(MessageKeys.GET_ALL_TERMS_SUCCESSFULLY)
                    .data(termResponses)
                    .status(HttpStatus.OK)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ResponseObject.builder()
                            .message(e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .data(null)
                            .build());
        }
    }
}

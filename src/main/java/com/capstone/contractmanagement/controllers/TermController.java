package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.dtos.term.CreateTermDTO;
import com.capstone.contractmanagement.dtos.term.CreateTypeTermDTO;
import com.capstone.contractmanagement.dtos.term.UpdateTermDTO;
import com.capstone.contractmanagement.dtos.term.UpdateTypeTermDTO;
import com.capstone.contractmanagement.entities.Term;
import com.capstone.contractmanagement.entities.TypeTerm;
import com.capstone.contractmanagement.enums.TypeTermIdentifier;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.ITermRepository;
import com.capstone.contractmanagement.repositories.ITypeTermRepository;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.responses.term.CreateTermResponse;
import com.capstone.contractmanagement.responses.term.GetAllTermsResponse;
import com.capstone.contractmanagement.responses.term.TypeTermResponse;
import com.capstone.contractmanagement.services.term.ITermService;
import com.capstone.contractmanagement.services.translation.TranslationService;
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("${api.prefix}/terms")
@RequiredArgsConstructor
public class TermController {

    private final ITermService termService;
    private final TranslationService translationService;
    private final ITermRepository termRepository;
    private final ITypeTermRepository typeTermRepository;

    @PostMapping("/create/{typeTermId}")
    public ResponseObject createTerm(@PathVariable Long typeTermId, @RequestBody CreateTermDTO termRequest) {
        CreateTermResponse termResponse = termService.createTerm(typeTermId, termRequest);
        return ResponseObject.builder()
                .message(MessageKeys.CREATE_TERM_SUCCESSFULLY)
                .data(termResponse)
                .status(HttpStatus.CREATED)
                .build();
    }

    @PostMapping("/create-type-term")
    public ResponseObject createTypeTerm(@RequestBody CreateTypeTermDTO request) {
        TypeTerm typeTerm = termService.createTypeTerm(request);
                 return ResponseObject.builder()
                .message(MessageKeys.CREATE_TYPE_TERM_SUCCESSFULLY)
                .data(typeTerm)
                .status(HttpStatus.CREATED)
                .build();
    }

    @PutMapping("/update/{termId}")
    public ResponseEntity<ResponseObject> updateTerm(@PathVariable Long termId, @RequestBody UpdateTermDTO termRequest) throws DataNotFoundException {
        CreateTermResponse termResponse = termService.updateTerm(termId, termRequest);
        return ResponseEntity.status(HttpStatus.OK).body(ResponseObject.builder()
                .message(MessageKeys.UPDATE_TERM_SUCCESSFULLY)
                .data(termResponse)
                .status(HttpStatus.OK).build());
    }

    @GetMapping("/get-all")
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

    @GetMapping("/get-by-id/{termId}")
    public ResponseEntity<ResponseObject> getTermById(@PathVariable Long termId) throws DataNotFoundException {
        CreateTermResponse termResponse = termService.getTermById(termId);
        return ResponseEntity.status(HttpStatus.OK).body(ResponseObject.builder()
                .message(MessageKeys.GET_TERM_SUCCESSFULLY)
                .data(termResponse)
                .status(HttpStatus.OK).build());
    }

    @DeleteMapping("/delete/{termId}")
    public ResponseEntity<ResponseObject> deleteTerm(@PathVariable Long termId) throws DataNotFoundException {
        termService.deleteTerm(termId);
        return ResponseEntity.status(HttpStatus.OK).body(ResponseObject.builder()
                .message(MessageKeys.DELETE_TERM_SUCCESSFULLY)
                .status(HttpStatus.NO_CONTENT)
                .build());// Trả về mã 204 (No Content)
    }


    // update type term
    @PutMapping("/update-type-term/{typeTermId}")
    public ResponseEntity<String> updateTypeTerm(@PathVariable Long typeTermId, @RequestBody UpdateTypeTermDTO request) {
        return ResponseEntity.ok(termService.updateTypeTerm(typeTermId, request));
    }

    // delete type term
    @DeleteMapping("/delete-type-term/{typeTermId}")
    public ResponseEntity<ResponseObject> deleteTypeTerm(@PathVariable Long typeTermId) {
        termService.deleteTypeTerm(typeTermId);
        return ResponseEntity.status(HttpStatus.OK).body(ResponseObject.builder()
                .message(MessageKeys.DELETE_TERM_SUCCESSFULLY)
                .status(HttpStatus.NO_CONTENT)
                .build());// Trả về mã 204 (No Content)
    }

    @GetMapping("/get-all-type-terms")
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
    public ResponseEntity<ResponseObject> getTypeTermById(@PathVariable Long typeTermId) {
        TypeTermResponse typeTermResponse = termService.getTypeTermById(typeTermId);
        return ResponseEntity.status(HttpStatus.OK).body(ResponseObject.builder()
                .message(MessageKeys.GET_TYPE_TERM_SUCCESSFULLY)
                .data(typeTermResponse)
                .status(HttpStatus.OK).build());
    }

    // get all terms by type term id
    @GetMapping("/get-terms-by-type-term-id/{typeTermId}")
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
    public ResponseEntity<String> updateTermStatus(@PathVariable Long termId, @PathVariable Boolean isDeleted) {
        try {
            termService.updateTermStatus(termId, isDeleted);
            String message = isDeleted ? "Xóa mềm điều khoản thành công" : "Khôi phục điều khoản thành công";
            return ResponseEntity.ok(message);
        } catch (DataNotFoundException e) {
            return ResponseEntity.badRequest().body("Không tìm thấy điều khoản");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/legal-basis")
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
    public ResponseEntity<List<TypeTermResponse>> getAdditionalTypeTerms() {
        List<TypeTerm> typeTerms = typeTermRepository.findByIdentifier(TypeTermIdentifier.ADDITIONAL_TERMS);
        List<TypeTermResponse> responses = typeTerms.stream()
                .map(tt -> TypeTermResponse.builder()
                        .id(tt.getId())
                        .name(tt.getName())
                        .identifier(tt.getIdentifier().name())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.OK).body(responses);
    }

}

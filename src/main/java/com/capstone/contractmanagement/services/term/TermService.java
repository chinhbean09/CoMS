package com.capstone.contractmanagement.services.term;

import com.capstone.contractmanagement.dtos.term.CreateTermDTO;
import com.capstone.contractmanagement.entities.Term;
import com.capstone.contractmanagement.entities.TypeTerm;
import com.capstone.contractmanagement.repositories.ITermRepository;
import com.capstone.contractmanagement.repositories.ITypeTermRepository;
import com.capstone.contractmanagement.responses.term.CreateTermResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TermService implements ITermService{

    private final ITermRepository termRepository;
    private final ITypeTermRepository typeTermRepository;
    @Override
    public CreateTermResponse createTerm(Long typeTermId, CreateTermDTO request) {
        TypeTerm typeTerm = typeTermRepository.findById(typeTermId)
                .orElseThrow(() -> new IllegalArgumentException("TypeTerm not found"));
        Term term = Term.builder()
                .clauseCode(request.getClauseCode())
                .label(request.getLabel())
                .value(request.getValue())
                .createdAt(LocalDateTime.now())
                .typeTerm(typeTerm)
                .build();
        termRepository.save(term);
        return CreateTermResponse.builder()
                .id(term.getId())
                .clauseCode(term.getClauseCode())
                .label(term.getLabel())
                .value(term.getValue())
                .type(term.getTypeTerm().getName())
                .build();
    }


//    @Override
//    public CreateTermResponse updateTerm(Long termId, UpdateTermDTO termRequest) throws DataNotFoundException {
//        // Find the term by ID
//        Optional<Term> optionalTerm = termRepository.findById(termId);
//
//        if (!optionalTerm.isPresent()) {
//            throw new DataNotFoundException("Term not found");
//        }
//
//        // Update the term
//        Term term = optionalTerm.get();
//        term.setTitle(termRequest.getTitle());
//        term.setDescription(termRequest.getDescription());
//        term.setIsDefault(termRequest.getIsDefault());
//
//        // Save updated term
//        Term updatedTerm = termRepository.save(term);
//
//        return CreateTermResponse.builder()
//                .id(updatedTerm.getId())
//                .title(updatedTerm.getTitle())
//                .description(updatedTerm.getDescription())
//                .isDefault(updatedTerm.getIsDefault())
//                .createdAt(updatedTerm.getCreatedAt().toString())
//                .build();
//    }
//
//    @Override
//    public List<CreateTermResponse> getAllTerms() {
//        List<Term> terms = termRepository.findAll();
//
//        return terms.stream()
//                .map(term -> CreateTermResponse.builder()
//                        .id(term.getId())
//                        .title(term.getTitle())
//                        .description(term.getDescription())
//                        .isDefault(term.getIsDefault())
//                        .createdAt(term.getCreatedAt().toString())
//                        .build())
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public CreateTermResponse getTermById(Long id) throws DataNotFoundException {
//        Optional<Term> optionalTerm = termRepository.findById(id);
//
//        if (!optionalTerm.isPresent()) {
//            throw new DataNotFoundException("Term not found");
//        }
//
//        Term term = optionalTerm.get();
//        return CreateTermResponse.builder()
//                .id(term.getId())
//                .title(term.getTitle())
//                .description(term.getDescription())
//                .isDefault(term.getIsDefault())
//                .createdAt(term.getCreatedAt().toString())
//                .build();
//    }
//
//    @Override
//    public void deleteTerm(Long termId) throws DataNotFoundException {
//        Optional<Term> optionalTerm = termRepository.findById(termId);
//
//        if (!optionalTerm.isPresent()) {
//            throw new DataNotFoundException("Term not found");
//        }
//
//        termRepository.deleteById(termId);
//    }
}

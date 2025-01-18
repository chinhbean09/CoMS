package com.capstone.contractmanagement.services.term;

import com.capstone.contractmanagement.dtos.term.CreateTermDTO;
import com.capstone.contractmanagement.dtos.term.UpdateTermDTO;
import com.capstone.contractmanagement.entities.Term;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.ITermRepository;
import com.capstone.contractmanagement.responses.term.TermResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TermService implements ITermService{

    private final ITermRepository termRepository;
    @Override
    public TermResponse createTerm(CreateTermDTO termRequest) {
        // Map TermRequest to Term entity
        Term term = Term.builder()
                .title(termRequest.getTitle())
                .description(termRequest.getDescription())
                .isDefault(termRequest.getIsDefault())
                .createdAt(LocalDateTime.now())
                .build();

        // Save Term entity
        Term savedTerm = termRepository.save(term);

        // Map saved Term to TermResponse
        return TermResponse.builder()
                .id(savedTerm.getId())
                .title(savedTerm.getTitle())
                .description(savedTerm.getDescription())
                .isDefault(savedTerm.getIsDefault())
                .createdAt(savedTerm.getCreatedAt().toString())
                .build();
    }

    @Override
    public TermResponse updateTerm(Long termId, UpdateTermDTO termRequest) throws DataNotFoundException {
        // Find the term by ID
        Optional<Term> optionalTerm = termRepository.findById(termId);

        if (!optionalTerm.isPresent()) {
            throw new DataNotFoundException("Term not found");
        }

        // Update the term
        Term term = optionalTerm.get();
        term.setTitle(termRequest.getTitle());
        term.setDescription(termRequest.getDescription());
        term.setIsDefault(termRequest.getIsDefault());

        // Save updated term
        Term updatedTerm = termRepository.save(term);

        return TermResponse.builder()
                .id(updatedTerm.getId())
                .title(updatedTerm.getTitle())
                .description(updatedTerm.getDescription())
                .isDefault(updatedTerm.getIsDefault())
                .createdAt(updatedTerm.getCreatedAt().toString())
                .build();
    }

    @Override
    public List<TermResponse> getAllTerms() {
        List<Term> terms = termRepository.findAll();

        return terms.stream()
                .map(term -> TermResponse.builder()
                        .id(term.getId())
                        .title(term.getTitle())
                        .description(term.getDescription())
                        .isDefault(term.getIsDefault())
                        .createdAt(term.getCreatedAt().toString())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public TermResponse getTermById(Long id) throws DataNotFoundException {
        Optional<Term> optionalTerm = termRepository.findById(id);

        if (!optionalTerm.isPresent()) {
            throw new DataNotFoundException("Term not found");
        }

        Term term = optionalTerm.get();
        return TermResponse.builder()
                .id(term.getId())
                .title(term.getTitle())
                .description(term.getDescription())
                .isDefault(term.getIsDefault())
                .createdAt(term.getCreatedAt().toString())
                .build();
    }

    @Override
    public void deleteTerm(Long termId) throws DataNotFoundException {
        Optional<Term> optionalTerm = termRepository.findById(termId);

        if (!optionalTerm.isPresent()) {
            throw new DataNotFoundException("Term not found");
        }

        termRepository.deleteById(termId);
    }
}

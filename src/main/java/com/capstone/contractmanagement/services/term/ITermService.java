package com.capstone.contractmanagement.services.term;

import com.capstone.contractmanagement.dtos.term.CreateTermDTO;
import com.capstone.contractmanagement.dtos.term.CreateTypeTermDTO;
import com.capstone.contractmanagement.dtos.term.UpdateTermDTO;
import com.capstone.contractmanagement.dtos.term.UpdateTypeTermDTO;
import com.capstone.contractmanagement.entities.TypeTerm;
import com.capstone.contractmanagement.enums.TypeTermIdentifier;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.term.CreateTermResponse;
import com.capstone.contractmanagement.responses.term.GetAllTermsResponse;
import com.capstone.contractmanagement.responses.term.TypeTermResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ITermService {
    CreateTermResponse createTerm(Long typeTermId, CreateTermDTO request);

    TypeTerm createTypeTerm(CreateTypeTermDTO request);

    CreateTermResponse updateTerm(Long termId, UpdateTermDTO termRequest) throws DataNotFoundException;

    Page<GetAllTermsResponse> getAllTerms(List<TypeTermIdentifier> identifiers, String search, int page, int size);

    CreateTermResponse getTermById(Long id) throws DataNotFoundException;

    void deleteTerm(Long termId) throws DataNotFoundException;

    String updateTypeTerm(Long typeTermId, UpdateTypeTermDTO request);

    void deleteTypeTerm(Long typeTermId);

    TypeTermResponse getTypeTermById(Long typeTermId);

    List<TypeTermResponse> getAllTypeTerms();

    List<GetAllTermsResponse> getAllTermsByTypeTermId(Long typeTermId);

    void updateTermStatus(Long termId, Boolean isDeleted) throws DataNotFoundException;
}

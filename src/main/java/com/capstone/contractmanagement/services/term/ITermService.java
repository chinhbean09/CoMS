package com.capstone.contractmanagement.services.term;

import com.capstone.contractmanagement.dtos.term.CreateTermDTO;
import com.capstone.contractmanagement.dtos.term.CreateTypeTermDTO;
import com.capstone.contractmanagement.entities.TypeTerm;
import com.capstone.contractmanagement.responses.term.CreateTermResponse;

public interface ITermService {
    CreateTermResponse createTerm(Long typeTermId, CreateTermDTO request);

    TypeTerm createTypeTerm(CreateTypeTermDTO request);
//    CreateTermResponse updateTerm(Long termId, UpdateTermDTO termRequest) throws DataNotFoundException;
//
//    List<CreateTermResponse> getAllTerms();
//
//    CreateTermResponse getTermById(Long id) throws DataNotFoundException;
//
//    void deleteTerm(Long termId) throws DataNotFoundException;
}

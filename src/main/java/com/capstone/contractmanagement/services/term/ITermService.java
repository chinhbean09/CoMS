package com.capstone.contractmanagement.services.term;

import com.capstone.contractmanagement.dtos.term.CreateTermDTO;
import com.capstone.contractmanagement.dtos.term.UpdateTermDTO;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.term.CreateTermResponse;

import java.util.List;

public interface ITermService {
    CreateTermResponse createTerm(Long typeTermId, CreateTermDTO request);

//    CreateTermResponse updateTerm(Long termId, UpdateTermDTO termRequest) throws DataNotFoundException;
//
//    List<CreateTermResponse> getAllTerms();
//
//    CreateTermResponse getTermById(Long id) throws DataNotFoundException;
//
//    void deleteTerm(Long termId) throws DataNotFoundException;
}

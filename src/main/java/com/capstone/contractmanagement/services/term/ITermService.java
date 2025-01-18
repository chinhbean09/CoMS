package com.capstone.contractmanagement.services.term;

import com.capstone.contractmanagement.dtos.term.CreateTermDTO;
import com.capstone.contractmanagement.dtos.term.UpdateTermDTO;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.term.TermResponse;

import java.util.List;

public interface ITermService {
    TermResponse createTerm(CreateTermDTO termRequest);

    TermResponse updateTerm(Long termId, UpdateTermDTO termRequest) throws DataNotFoundException;

    List<TermResponse> getAllTerms();

    TermResponse getTermById(Long id) throws DataNotFoundException;

    void deleteTerm(Long termId) throws DataNotFoundException;
}

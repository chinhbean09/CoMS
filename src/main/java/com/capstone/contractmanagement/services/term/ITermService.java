package com.capstone.contractmanagement.services.term;

import com.capstone.contractmanagement.dtos.term.*;
import com.capstone.contractmanagement.entities.term.TypeTerm;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.term.CreateTermResponse;
import com.capstone.contractmanagement.responses.term.GetAllTermsResponse;
import com.capstone.contractmanagement.responses.term.GetAllTermsResponseLessField;
import com.capstone.contractmanagement.responses.term.TypeTermResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ITermService {
    CreateTermResponse createTerm(Long typeTermId, CreateTermDTO request);

    TypeTerm createTypeTerm(CreateTypeTermDTO request);

    CreateTermResponse updateTerm(Long termId, UpdateTermDTO termRequest) throws DataNotFoundException;

     Page<GetAllTermsResponse> getAllTerms(List<Long> typeTermIds, boolean includeLegalBasis, String search, Pageable pageable);

     CreateTermResponse getTermById(Long id) throws DataNotFoundException;

    void deleteTerm(Long termId) throws DataNotFoundException;

    String updateTypeTerm(Long typeTermId, UpdateTypeTermDTO request);

    void deleteTypeTerm(Long typeTermId);

    TypeTermResponse getTypeTermById(Long typeTermId);

    List<TypeTermResponse> getAllTypeTerms();

    Page<GetAllTermsResponse> getAllTermsByTypeTermId(Long typeTermId, String search, int page, int size);

    void updateTermStatus(Long termId, Boolean isDeleted) throws DataNotFoundException;

    Page<GetAllTermsResponseLessField> getAllTermsLessField(List<Long> typeTermIds, boolean includeLegalBasis, String search, Pageable pageable);

    List<CreateTermResponse> batchCreateTerms(List<BatchCreateTermDTO> dtos) throws DataNotFoundException;

    List<CreateTermResponse> importTermsFromExcel(MultipartFile file, Long typeTermId) throws IOException;
}

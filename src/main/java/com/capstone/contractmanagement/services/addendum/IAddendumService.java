package com.capstone.contractmanagement.services.addendum;

import com.capstone.contractmanagement.dtos.addendum.AddendumDTO;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.addendum.AddendumResponse;

import java.util.List;

public interface IAddendumService {

    AddendumResponse createAddendum(AddendumDTO addendumDTO) throws DataNotFoundException;

    List<AddendumResponse> getAllByContractId(Long contractId) throws DataNotFoundException;

    List<AddendumResponse> getAllByAddendumType(Long addendumTypeId) throws DataNotFoundException;

    String updateAddendum(Long addendumId, AddendumDTO addendumDTO) throws DataNotFoundException;

    void deleteAddendum(Long addendumId) throws DataNotFoundException;

    AddendumResponse getAddendumById(Long addendumId) throws DataNotFoundException;
}

package com.capstone.contractmanagement.services.addendum_type;

import com.capstone.contractmanagement.dtos.addendum.AddendumTypeDTO;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.addendum.AddendumTypeResponse;

import java.util.List;

public interface IAddendumTypeService {
    String createAddendumType(AddendumTypeDTO addendumTypeDTO);
    String updateAddendumType(Long addendumTypeId, AddendumTypeDTO addendumTypeDTO) throws DataNotFoundException;
    void deleteAddendumType(Long addendumTypeId) throws DataNotFoundException;
    AddendumTypeResponse getAddendumTypeById(Long addendumTypeId) throws DataNotFoundException;
    List<AddendumTypeResponse> getAllAddendumTypes();
}

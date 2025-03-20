package com.capstone.contractmanagement.services.addendum_type;

import com.capstone.contractmanagement.dtos.addendum.AddendumTypeDTO;
import com.capstone.contractmanagement.entities.AddendumType;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.IAddendumTypeRepository;
import com.capstone.contractmanagement.responses.addendum.AddendumTypeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddendumTypeService implements IAddendumTypeService{
    private final IAddendumTypeRepository addendumTypeRepository;
    @Override
    public String createAddendumType(AddendumTypeDTO addendumTypeDTO) {
        AddendumType addendumType = AddendumType.builder()
                .name(addendumTypeDTO.getName())
                .build();
        addendumTypeRepository.save(addendumType);
        return "Tạo loại phụ lục thành công";
    }

    @Override
    public String updateAddendumType(Long addendumTypeId, AddendumTypeDTO addendumTypeDTO) throws DataNotFoundException {
        AddendumType addendumType = addendumTypeRepository.findById(addendumTypeId)
                .orElseThrow(() -> new DataNotFoundException("Loại phụ lục không tìm thấy với id : " + addendumTypeId));
        addendumType.setName(addendumTypeDTO.getName());
        addendumTypeRepository.save(addendumType);
        return "Cập nhật loại phụ lục thành công";
    }

    @Override
    public void deleteAddendumType(Long addendumTypeId) throws DataNotFoundException {
        AddendumType addendumType = addendumTypeRepository.findById(addendumTypeId)
                .orElseThrow(() -> new DataNotFoundException("Loại phụ lục không tìm thấy với id : " + addendumTypeId));
        addendumTypeRepository.delete(addendumType);
    }

    @Override
    public AddendumTypeResponse getAddendumTypeById(Long addendumTypeId) throws DataNotFoundException {
        AddendumType addendumType = addendumTypeRepository.findById(addendumTypeId)
                .orElseThrow(() -> new DataNotFoundException("Loại phụ lục không tìm thấy với id : " + addendumTypeId));
        return AddendumTypeResponse.builder()
                .addendumTypeId(addendumType.getId())
                .name(addendumType.getName())
                .build();
    }

    @Override
    public List<AddendumTypeResponse> getAllAddendumTypes() {
        List<AddendumType> addendumTypes = addendumTypeRepository.findAll();
        return addendumTypes.stream().map(addendumType -> AddendumTypeResponse.builder()
                .addendumTypeId(addendumType.getId())
                .name(addendumType.getName())
                .build()).toList();
    }
}

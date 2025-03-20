package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.dtos.addendum.AddendumTypeDTO;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.services.addendum_type.IAddendumTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/addendum-types")
@RequiredArgsConstructor
public class AddendumTypeController {
    private final IAddendumTypeService addendumTypeService;

    //get all addendum types
    @GetMapping("/get-all")
    public ResponseEntity<ResponseObject> getAllAddendumTypes() {
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .data(addendumTypeService.getAllAddendumTypes())
                .build());
    }

    //get addendum type by id
    @GetMapping("/get-by-id/{addendumTypeId}")
    public ResponseEntity<ResponseObject> getAddendumTypeById(@PathVariable Long addendumTypeId) throws DataNotFoundException {
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .data(addendumTypeService.getAddendumTypeById(addendumTypeId))
                .build());
    }

    // create addendum type
    @PostMapping("/create")
    public ResponseEntity<String> createAddendumType(@RequestBody AddendumTypeDTO addendumTypeDTO) {
        return ResponseEntity.ok(addendumTypeService.createAddendumType(addendumTypeDTO));
    }

    // update addendum type
    @PutMapping("/update/{addendumTypeId}")
    public ResponseEntity<String> updateAddendumType(@PathVariable Long addendumTypeId, @RequestBody AddendumTypeDTO addendumTypeDTO) throws DataNotFoundException {
        return ResponseEntity.ok(addendumTypeService.updateAddendumType(addendumTypeId, addendumTypeDTO));
    }

    // delete addendum type
    @DeleteMapping("/delete/{addendumTypeId}")
    public ResponseEntity<ResponseObject> deleteAddendumType(@PathVariable Long addendumTypeId) throws DataNotFoundException {
        addendumTypeService.deleteAddendumType(addendumTypeId);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Xoá loại phụ lục thành công")
                .data(null)
                .build());
    }
}

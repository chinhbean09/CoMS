package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.dtos.addendum.AddendumDTO;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.responses.addendum.AddendumResponse;
import com.capstone.contractmanagement.services.addendum.IAddendumService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/addendums")
@RequiredArgsConstructor
public class AddendumController {
    private final IAddendumService addendumService;

    // api create addendum
    @PostMapping("/create")
    public ResponseEntity<ResponseObject> createAddendum(@RequestBody AddendumDTO addendumDTO) throws DataNotFoundException {
        AddendumResponse addendumResponse = addendumService.createAddendum(addendumDTO);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.CREATED)
                .message("Tạo phụ lục thành công")
                .data(addendumResponse)
                .build());
    }

    @GetMapping("/get-by-contract-id/{contractId}")
    public ResponseEntity<ResponseObject> getAllByContract(@PathVariable Long contractId) throws DataNotFoundException {
        List<AddendumResponse> addendumResponseList = addendumService.getAllByContractId(contractId);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Lấy danh sách phụ lục thành công")
                .data(addendumResponseList)
                .build());
    }

    @GetMapping("/get-by-type/{addendumTypeId}")
    public ResponseEntity<ResponseObject> getAllByAddendumType(@PathVariable Long addendumTypeId) throws DataNotFoundException {
        List<AddendumResponse> addendumResponseList = addendumService.getAllByAddendumType(addendumTypeId);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Lấy danh sách phụ lục thành công")
                .data(addendumResponseList)
                .build());
    }

    @PutMapping("/update/{addendumId}")
    public ResponseEntity<String> updateAddendum(@PathVariable Long addendumId,
                                                 @RequestBody AddendumDTO addendumDTO) throws DataNotFoundException {
        return ResponseEntity.ok(addendumService.updateAddendum(addendumId, addendumDTO));
    }

    @DeleteMapping("/delete/{addendumId}")
    public ResponseEntity<ResponseObject> deleteAddendum(@PathVariable Long addendumId) throws DataNotFoundException {
        addendumService.deleteAddendum(addendumId);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Xóa phụ lục thành công")
                .data(null)
                .build());
    }

    @GetMapping("/get-by-id/{addendumId}")
    public ResponseEntity<ResponseObject> getAddendumById(@PathVariable Long addendumId) throws DataNotFoundException {
        AddendumResponse addendumResponse = addendumService.getAddendumById(addendumId);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Lấy phụ lục theo id")
                .data(addendumResponse)
                .build());
    }
}

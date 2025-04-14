package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.dtos.department.DepartmentDTO;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.responses.department.DepartmentResponse;
import com.capstone.contractmanagement.services.department.IDepartmentService;
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/departments")
@RequiredArgsConstructor
public class DepartmentController {
    private final IDepartmentService departmentService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> createDepartment(@RequestBody DepartmentDTO departmentDTO) {
        DepartmentResponse response = departmentService.createDepartment(departmentDTO);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.CREATED)
                .message(MessageKeys.CREATE_DEPARTMENT_SUCCESSFULLY)
                .data(response)
                .build());
    }

    @PutMapping("/update/{departmentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR', 'ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> updateDepartment(@PathVariable Long departmentId, @RequestBody DepartmentDTO departmentDTO) throws DataNotFoundException {
        DepartmentResponse response = departmentService.updateDepartment(departmentId, departmentDTO);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message(MessageKeys.UPDATE_DEPARTMENT_SUCCESSFULLY)
                .data(response)
                .build());
    }

    @GetMapping("/get-all")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR', 'ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> getAllDepartment() {
        List<DepartmentResponse> responses = departmentService.getAllDepartment();
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message(MessageKeys.GET_ALL_DEPARTMENT_SUCCESSFULLY)
                .data(responses)
                .build());
    }

    @GetMapping("/get-by-id/{departmentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR', 'ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> getDepartment(@PathVariable Long departmentId) throws DataNotFoundException {
        DepartmentResponse responses = departmentService.getDepartmentById(departmentId);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message(MessageKeys.GET_ALL_DEPARTMENT_SUCCESSFULLY)
                .data(responses)
                .build());
    }


}

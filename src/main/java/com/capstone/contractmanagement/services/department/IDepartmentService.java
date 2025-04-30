package com.capstone.contractmanagement.services.department;

import com.capstone.contractmanagement.dtos.department.DepartmentDTO;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.department.DepartmentResponse;

import java.util.List;

public interface IDepartmentService {

    DepartmentResponse createDepartment(DepartmentDTO departmentDTO) throws DataNotFoundException;

    DepartmentResponse updateDepartment(Long departmentId, DepartmentDTO departmentDTO) throws DataNotFoundException;

    List<DepartmentResponse> getAllDepartment();

    DepartmentResponse getDepartmentById(Long departmentId) throws DataNotFoundException;

    void deleteDepartment(Long departmentId) throws DataNotFoundException;
}

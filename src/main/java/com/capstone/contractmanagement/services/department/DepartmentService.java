package com.capstone.contractmanagement.services.department;

import com.capstone.contractmanagement.dtos.department.DepartmentDTO;
import com.capstone.contractmanagement.entities.Department;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.IDepartmentRepository;
import com.capstone.contractmanagement.responses.department.DepartmentResponse;
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService implements IDepartmentService {
    private final IDepartmentRepository departmentRepository;

    @Override
    public DepartmentResponse createDepartment(DepartmentDTO departmentDTO) throws DataNotFoundException {
        if (departmentRepository.existsByDepartmentName(departmentDTO.getDepartmentName())) {
            throw new DataNotFoundException("Phòng ban đã tồn tại.");
        }
        Department department = Department.builder()
                .departmentName(departmentDTO.getDepartmentName())
                .build();
        departmentRepository.save(department);
        return DepartmentResponse.builder()
                .departmentId(department.getId())
                .departmentName(department.getDepartmentName())
                .build();
    }

    @Override
    public DepartmentResponse updateDepartment(Long departmentId, DepartmentDTO departmentDTO) throws DataNotFoundException {

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.DEPARTMENT_NOT_FOUND));
        if (departmentRepository.existsByDepartmentName(departmentDTO.getDepartmentName())) {
            throw new DataNotFoundException("Phòng ban đã tồn tại.");
        }

        department.setDepartmentName(departmentDTO.getDepartmentName());

        departmentRepository.save(department);
        return DepartmentResponse.builder()
                .departmentId(department.getId())
                .departmentName(department.getDepartmentName())
                .build();
    }

    @Override
    public List<DepartmentResponse> getAllDepartment() {
        return departmentRepository.findAll().stream().map(department -> DepartmentResponse.builder()
                        .departmentId(department.getId())
                        .departmentName(department.getDepartmentName())
                        .build())
                .toList();
    }

    @Override
    public DepartmentResponse getDepartmentById(Long departmentId) throws DataNotFoundException {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.DEPARTMENT_NOT_FOUND));

        return DepartmentResponse.builder()
                .departmentId(department.getId())
                .departmentName(department.getDepartmentName())
                .build();
    }

    @Override
    public void deleteDepartment(Long departmentId) throws DataNotFoundException {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.DEPARTMENT_NOT_FOUND));

        departmentRepository.delete(department);
    }
}

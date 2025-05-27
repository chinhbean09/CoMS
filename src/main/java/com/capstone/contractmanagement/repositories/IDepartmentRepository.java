package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IDepartmentRepository extends JpaRepository<Department, Long> {
    boolean existsByDepartmentName(String departmentName);
}

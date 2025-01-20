package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ITaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByManagerId(Long managerId);
}

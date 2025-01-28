package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ITaskRepository extends JpaRepository<Task, Long> {
    //List<Task> findAllByManagerId(Long managerId);

    @Query("SELECT t FROM Task t WHERE t.manager.id = :managerId")
    List<Task> findByManagerId(Long managerId);

    @Query("SELECT t FROM Task t WHERE t.taskName LIKE %:keyword% OR t.description LIKE %:keyword%")
    List<Task> searchTasks(String keyword);

    List<Task> findByAssigneeId(Long assigneeId);
}

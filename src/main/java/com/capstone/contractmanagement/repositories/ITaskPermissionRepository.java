//package com.capstone.contractmanagement.repositories;
//
//import com.capstone.contractmanagement.entities.Task;
//import com.capstone.contractmanagement.entities.TaskPermission;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//
//@Repository
//public interface ITaskPermissionRepository extends JpaRepository<TaskPermission, Long> {
//    List<TaskPermission> findByTaskId(Long taskId);
//    void deleteAllByTask(Task task);
//    void deleteByTaskId(Long taskId);
//}

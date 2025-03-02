//package com.capstone.contractmanagement.services.task;
//
//import com.capstone.contractmanagement.dtos.task.CreateTaskDTO;
//import com.capstone.contractmanagement.dtos.task.UpdateTaskDTO;
//import com.capstone.contractmanagement.exceptions.DataNotFoundException;
//import com.capstone.contractmanagement.responses.task.TaskResponse;
//
//import java.util.List;
//
//public interface ITaskService {
//
//    TaskResponse updateTask(Long taskId, UpdateTaskDTO updateTaskDTO) throws DataNotFoundException;
//    void deleteTask(Long taskId) throws DataNotFoundException;
//
//    TaskResponse createTask(CreateTaskDTO request) throws DataNotFoundException;
//    List<TaskResponse> getTasksByManager(Long managerId);
//    List<TaskResponse> searchTasks(String keyword);
//    void updateLastViewedAt(Long taskId) throws DataNotFoundException;
//    List<TaskResponse> getTasksByEmployee(Long employeeId);
//}

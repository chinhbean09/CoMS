package com.capstone.contractmanagement.services.task;

import com.capstone.contractmanagement.dtos.task.CreateTaskDTO;
import com.capstone.contractmanagement.dtos.task.UpdateTaskDTO;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.task.TaskResponse;

import java.util.List;

public interface ITaskService {
    TaskResponse createTask(CreateTaskDTO createTaskDTO) throws DataNotFoundException;

    List<TaskResponse> getTasksByManager(Long managerId);

    TaskResponse updateTask(Long taskId, UpdateTaskDTO updateTaskDTO) throws DataNotFoundException;
    void deleteTask(Long taskId) throws DataNotFoundException;
}

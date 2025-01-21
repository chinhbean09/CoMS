package com.capstone.contractmanagement.controllers;


import com.capstone.contractmanagement.dtos.task.CreateTaskDTO;
import com.capstone.contractmanagement.dtos.task.UpdateTaskDTO;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.responses.task.TaskResponse;
import com.capstone.contractmanagement.services.task.ITaskService;
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final ITaskService taskService;

    @GetMapping("/search")
    public ResponseEntity<ResponseObject> searchTasks(@RequestParam String keyword) {
        List<TaskResponse> tasks = taskService.searchTasks(keyword);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.SEARCH_TASK_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .data(tasks).build());
    }

    // create a new task
    @PostMapping("/create")
    public ResponseEntity<ResponseObject> createTask(@RequestBody CreateTaskDTO createTaskDTO) throws DataNotFoundException {
        TaskResponse taskResponse = taskService.createTask(createTaskDTO);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.CREATE_TASK_SUCCESSFULLY)
                .status(HttpStatus.CREATED)
                .data(taskResponse).build());
    }

    // get tasks assigned by manager
    @GetMapping("/get-task-by-manager/{managerId}")
    public ResponseEntity<ResponseObject> getTasksByManager(@PathVariable Long managerId) {
        List<TaskResponse> tasks = taskService.getTasksByManager(managerId);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.GET_TASK_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .data(tasks).build());
    }

    // update task
    @PutMapping("/update/{taskId}")
    public ResponseEntity<ResponseObject> updateTask(@PathVariable Long taskId, @RequestBody UpdateTaskDTO updateTaskDTO) throws DataNotFoundException {
        TaskResponse taskResponse = taskService.updateTask(taskId, updateTaskDTO);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.UPDATE_TASK_SUCCESSFULLY)
                .status(HttpStatus.OK)
                .data(taskResponse).build());
    }

    // delete task
    @DeleteMapping("/delete/{taskId}")
    public ResponseEntity<ResponseObject> deleteTask(@PathVariable Long taskId) throws DataNotFoundException {
        taskService.deleteTask(taskId);
        return ResponseEntity.ok(ResponseObject.builder()
                .message(MessageKeys.DELETE_TASK_SUCCESSFULLY)
                .status(HttpStatus.NO_CONTENT)
                .build());
    }
}

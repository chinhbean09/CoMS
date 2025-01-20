package com.capstone.contractmanagement.services.task;

import com.capstone.contractmanagement.dtos.task.CreateTaskDTO;
import com.capstone.contractmanagement.dtos.task.UpdateTaskDTO;
import com.capstone.contractmanagement.entities.Task;
import com.capstone.contractmanagement.entities.TaskPermission;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.enums.TaskStatus;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.ITaskPermissionRepository;
import com.capstone.contractmanagement.repositories.ITaskRepository;
import com.capstone.contractmanagement.repositories.IUserRepository;
import com.capstone.contractmanagement.responses.permission.PermissionResponse;
import com.capstone.contractmanagement.responses.task.TaskResponse;
import com.capstone.contractmanagement.responses.task.TaskUserResponse;
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService implements ITaskService {

    private final ITaskRepository taskRepository;
    private final IUserRepository userRepository;
    private final ITaskPermissionRepository taskPermissionRepository;

    @Override
    public TaskResponse createTask(CreateTaskDTO createTaskDTO) throws DataNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User manager = (User) authentication.getPrincipal();

        Task task = Task.builder()
                .taskName(createTaskDTO.getName())
                .description(createTaskDTO.getDescription())
                .dueDate(createTaskDTO.getDueDate())
                .manager(manager)
                .status(TaskStatus.ASSIGNED)
                .createdAt(LocalDateTime.now())
                .build();

        Task savedTask = taskRepository.save(task);

        // Assign permissions and assignees
        if (createTaskDTO.getPermissions() != null) {
            List<TaskPermission> permissions = createTaskDTO.getPermissions().stream().map(permissionDTO -> {
                User user = null;
                try {
                    user = userRepository.findById(permissionDTO.getUserId())
                            .orElseThrow(() -> new DataNotFoundException(MessageKeys.USER_NOT_FOUND));
                } catch (DataNotFoundException e) {
                    throw new RuntimeException(e);
                }

                return TaskPermission.builder()
                        .task(savedTask)
                        .user(user)
                        .canEdit(permissionDTO.isCanEdit())
                        .canView(permissionDTO.isCanView())
                        .build();
            }).collect(Collectors.toList());

            taskPermissionRepository.saveAll(permissions);
        }

        return mapToResponse(savedTask);
    }

    @Override
    public List<TaskResponse> getTasksByManager(Long managerId) {
        List<Task> tasks = taskRepository.findAllByManagerId(managerId);
        return tasks.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TaskResponse updateTask(Long taskId, UpdateTaskDTO updateTaskDTO) throws DataNotFoundException {
        Task existingTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.TASK_NOT_FOUND));

        // Update task details
        existingTask.setTaskName(updateTaskDTO.getName());
        existingTask.setDescription(updateTaskDTO.getDescription());
        existingTask.setDueDate(updateTaskDTO.getDueDate());

        // Clear current permissions and reassign based on update request
        taskPermissionRepository.deleteByTaskId(taskId);
        List<TaskPermission> newPermissions = updateTaskDTO.getPermissions().stream().map(permissionDTO -> {
            User user = null;
            try {
                user = userRepository.findById(permissionDTO.getUserId())
                        .orElseThrow(() -> new DataNotFoundException(MessageKeys.USER_NOT_FOUND));
            } catch (DataNotFoundException e) {
                throw new RuntimeException(e);
            }

            return TaskPermission.builder()
                    .task(existingTask)
                    .user(user)
                    .canEdit(permissionDTO.isCanEdit())
                    .canView(permissionDTO.isCanView())
                    .build();
        }).collect(Collectors.toList());

        taskPermissionRepository.saveAll(newPermissions);

        // Save updated task
        Task updatedTask = taskRepository.save(existingTask);
        return mapToResponse(updatedTask);
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId) throws DataNotFoundException {
        Task existingTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.TASK_NOT_FOUND));

        // Delete associated permissions
        taskPermissionRepository.deleteByTaskId(taskId);

        // Delete the task itself
        taskRepository.delete(existingTask);
    }

    // Helper to map Task to TaskResponse
    private TaskResponse mapToResponse(Task task) {
        List<TaskUserResponse> assignees = taskPermissionRepository.findByTaskId(task.getId())
                .stream()
                .map(permission -> {
                    TaskUserResponse userResponse = new TaskUserResponse();
                    userResponse.setUserId(permission.getUser().getId());
                    userResponse.setName(permission.getUser().getFullName());
                    //userResponse.setLastViewedAt(LocalDateTime.parse(permission.getUser().get().toString()));
                    return userResponse;
                }).collect(Collectors.toList());

        List<PermissionResponse> permissions = taskPermissionRepository.findByTaskId(task.getId())
                .stream()
                .map(permission -> {
                    PermissionResponse permissionResponse = new PermissionResponse();
                    permissionResponse.setUserId(permission.getUser().getId());
                    permissionResponse.setCanEdit(permission.isCanEdit());
                    permissionResponse.setCanView(permission.isCanView());
                    return permissionResponse;
                }).collect(Collectors.toList());

        TaskResponse response = new TaskResponse();
        response.setTaskId(task.getId());
        response.setName(task.getTaskName());
        response.setDescription(task.getDescription());
        response.setCreatedAt(java.sql.Date.valueOf(task.getCreatedAt().toLocalDate()));
        response.setDueDate(task.getDueDate());
        response.setStatus(task.getStatus().name());
        response.setAssignees(assignees);
        response.setPermissions(permissions);

        return response;
    }
}

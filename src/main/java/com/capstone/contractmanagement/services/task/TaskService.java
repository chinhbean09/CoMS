package com.capstone.contractmanagement.services.task;

import com.capstone.contractmanagement.dtos.task.CreateTaskDTO;
import com.capstone.contractmanagement.dtos.task.UpdateTaskDTO;
import com.capstone.contractmanagement.entities.Task;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.enums.TaskStatus;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.ITaskRepository;
import com.capstone.contractmanagement.repositories.IUserRepository;
import com.capstone.contractmanagement.responses.task.TaskResponse;
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService implements ITaskService {

    private final ITaskRepository taskRepository;
    private final IUserRepository userRepository;

    @Override
    @Transactional
    public TaskResponse updateTask(Long taskId, UpdateTaskDTO updateTaskDTO) throws DataNotFoundException {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.TASK_NOT_FOUND));

        if (updateTaskDTO.getTaskName() != null) task.setTaskName(updateTaskDTO.getTaskName());
        if (updateTaskDTO.getDescription() != null) task.setDescription(updateTaskDTO.getDescription());

        if (updateTaskDTO.getAssignedToId() != null) {
            User assignee = userRepository.findById(updateTaskDTO.getAssignedToId())
                    .orElseThrow(() -> new DataNotFoundException(MessageKeys.USER_NOT_FOUND));
            task.setAssignee(assignee);
        }

        if (updateTaskDTO.getSupervisorIds() != null) {
            List<User> supervisors = userRepository.findAllById(updateTaskDTO.getSupervisorIds());
            task.setSupervisors(supervisors);
        }

        if (updateTaskDTO.getDueDate() != null) task.setDueDate(updateTaskDTO.getDueDate());
        if (updateTaskDTO.getStatus() != null) task.setStatus(updateTaskDTO.getStatus());

        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);

        return mapToResponse(task);
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId) throws DataNotFoundException {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.TASK_NOT_FOUND));
        taskRepository.delete(task);
    }

    @Override
    public TaskResponse createTask(CreateTaskDTO request) throws DataNotFoundException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User manager = (User) authentication.getPrincipal();

        User assignee = userRepository.findById(request.getAssignedToId()).orElseThrow(() -> new DataNotFoundException(MessageKeys.USER_NOT_FOUND));

        List<User> supervisors = userRepository.findAllById(request.getSupervisorIds());

        Task task = Task.builder()
                .taskName(request.getTaskName())
                .description(request.getDescription())
                .manager(manager)
                .assignee(assignee)
                .supervisors(supervisors)
                .dueDate(request.getDueDate())
                .status(TaskStatus.ASSIGNED)
                .createdAt(LocalDateTime.now())
                .build();

        Task savedTask = taskRepository.save(task);

        return mapToResponse(savedTask);
    }

    @Override
    @Transactional
    public List<TaskResponse> getTasksByManager(Long managerId) {
        return taskRepository.findByManagerId(managerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<TaskResponse> searchTasks(String keyword) {
        return taskRepository.searchTasks(keyword)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TaskResponse mapToResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .taskName(task.getTaskName())
                .description(task.getDescription())
                .createdAt(task.getCreatedAt())
                .assignedTo(task.getAssignee().getFullName())
                .supervisors(task.getSupervisors().stream()
                        .map(User::getFullName)
                        .collect(Collectors.toList()))
                .dueDate(task.getDueDate())
                .status(task.getStatus().name())
                .lastViewedAt(task.getLastViewedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}

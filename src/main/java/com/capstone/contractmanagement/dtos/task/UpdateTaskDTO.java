package com.capstone.contractmanagement.dtos.task;

import com.capstone.contractmanagement.dtos.taskpermission.CreateTaskPermissionDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateTaskDTO {
    private String name;
    private String description;
    private Date dueDate;
    private List<Long> assigneeIds; // List of user IDs assigned to the task
    private List<CreateTaskPermissionDTO> permissions; // Permissions for each user
}

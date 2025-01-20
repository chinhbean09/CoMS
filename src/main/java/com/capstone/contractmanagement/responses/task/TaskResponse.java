package com.capstone.contractmanagement.responses.task;

import com.capstone.contractmanagement.responses.permission.PermissionResponse;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class TaskResponse {
    private Long taskId;
    private String name;
    private String description;
    private Date createdAt;
    private Date dueDate;
    private String status;
    private List<TaskUserResponse> assignees;
    private List<PermissionResponse> permissions;

}

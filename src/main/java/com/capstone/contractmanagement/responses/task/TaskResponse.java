package com.capstone.contractmanagement.responses.task;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
@Builder
public class TaskResponse {
//    private Long taskId;
//    private String name;
//    private String description;
//    private Date createdAt;
//    private Date dueDate;
//    private String status;
//    private List<TaskUserResponse> assignees;
//    private List<PermissionResponse> permissions;

    private Long id;
    private String taskName;
    private String description;
    private LocalDateTime createdAt;
    private String assignedTo;
    private List<String> supervisors;
    private Date dueDate;
    private String status;
    private LocalDateTime lastViewedAt;
    private LocalDateTime updatedAt;

}

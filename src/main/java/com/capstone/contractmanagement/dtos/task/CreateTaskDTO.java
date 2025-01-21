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
public class CreateTaskDTO {

    private String taskName;
    private String description;
    private Long assignedToId;
    private List<Long> supervisorIds;
    private Date dueDate;
}

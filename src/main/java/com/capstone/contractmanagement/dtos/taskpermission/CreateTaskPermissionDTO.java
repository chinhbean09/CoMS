package com.capstone.contractmanagement.dtos.taskpermission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateTaskPermissionDTO {
    private Long userId;
    private boolean canEdit;
    private boolean canView;
}

package com.capstone.contractmanagement.responses.permission;

import lombok.Data;

@Data
public class PermissionResponse {

    private Long userId;
    private boolean canEdit;
    private boolean canView;
}

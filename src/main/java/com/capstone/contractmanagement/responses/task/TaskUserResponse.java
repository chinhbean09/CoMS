package com.capstone.contractmanagement.responses.task;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskUserResponse {
    private Long userId;
    private String name;
    private LocalDateTime lastViewedAt;
}

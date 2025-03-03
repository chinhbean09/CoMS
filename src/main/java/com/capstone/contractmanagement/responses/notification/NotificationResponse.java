package com.capstone.contractmanagement.responses.notification;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private String message;
    private Boolean isRead;
    private Long contractId;
    private LocalDateTime createdAt;
    private Long userId;
}

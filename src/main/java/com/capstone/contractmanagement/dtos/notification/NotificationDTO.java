package com.capstone.contractmanagement.dtos.notification;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationDTO {
    private String message;
}

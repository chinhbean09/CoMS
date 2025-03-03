package com.capstone.contractmanagement.services.notification;

import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.notification.NotificationResponse;
import org.springframework.data.domain.Page;

public interface INotificationService {
    void saveNotification(User user, String message, Long contractId);
    Page<NotificationResponse> getAllNotifications(int page, int size);
    NotificationResponse getNotification(Long notificationId) throws DataNotFoundException;
    void markNotificationAsRead(Long notificationId) throws DataNotFoundException;
}

package com.capstone.contractmanagement.services.notification;

import com.capstone.contractmanagement.entities.Notification;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.INotificationRepository;
import com.capstone.contractmanagement.responses.notification.NotificationResponse;
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService implements INotificationService {

    private final INotificationRepository notificationRepository;
    @Override
    @Transactional
    public void saveNotification(User user, String message, Contract contract) {
        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .isRead(false)
                .contract(contract)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
    }

    @Override
    public Page<NotificationResponse> getAllNotifications(int page, int size) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Notification> notificationsPage = notificationRepository.findByUser(currentUser, pageable);

        return notificationsPage.map(notification -> NotificationResponse.builder()
                .id(notification.getId())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .userId(notification.getUser().getId())
                .contractId(notification.getContract().getId())
                .build());
    }

    @Override
    public NotificationResponse getNotification(Long notificationId) throws DataNotFoundException {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.NOTIFICATION_NOT_FOUND));{
            return NotificationResponse.builder()
                    .id(notification.getId())
                    .message(notification.getMessage())
                    .isRead(notification.getIsRead())
                    .createdAt(notification.getCreatedAt())
                    .userId(notification.getUser().getId())
                    .contractId(notification.getContract().getId())
                    .build();
        }
    }

    @Override
    public void markNotificationAsRead(Long notificationId) throws DataNotFoundException {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.NOTIFICATION_NOT_FOUND));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }
}

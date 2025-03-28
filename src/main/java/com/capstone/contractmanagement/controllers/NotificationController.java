package com.capstone.contractmanagement.controllers;


import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.responses.notification.NotificationResponse;
import com.capstone.contractmanagement.services.notification.INotificationService;
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationService notificationService;

    @GetMapping("/get-all-by-user")
    public ResponseEntity<ResponseObject> getAllNotification(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<NotificationResponse> notificationsPage = notificationService.getAllNotifications(page, size);

        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message(MessageKeys.GET_ALL_NOTIFICATION_SUCCESSFULLY)
                .data(notificationsPage)
                .build());
    }

    @GetMapping("/get-by-id/{notificationId}")
    public ResponseEntity<ResponseObject> getNotificationById(@PathVariable Long notificationId) throws DataNotFoundException {
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message(MessageKeys.GET_NOTIFICATION_SUCCESSFULLY)
                .data(notificationService.getNotification(notificationId))
                .build());
    }

    // api mark all notification as read
    @PutMapping("/mark-as-read/{notificationId}")
    public ResponseEntity<ResponseObject> markAllNotificationAsRead(@PathVariable Long notificationId) throws DataNotFoundException {
        notificationService.markNotificationAsRead(notificationId);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message(MessageKeys.MARK_NOTIFICATION_AS_READ_SUCCESSFULLY)
                .build());
    }
}

package com.capstone.contractmanagement.responses.notification;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

@Data
@Builder
public class NotificationPageResponse {
    private List<NotificationResponse> content;
    private long unreadCount;
    private Pageable pageable;
    private boolean last;
    private int totalPages;
    private long totalElements;
    private boolean first;
    private int size;
    private int number;
    private Sort sort;
    private int numberOfElements;
    private boolean empty;

       // thêm trường này
}

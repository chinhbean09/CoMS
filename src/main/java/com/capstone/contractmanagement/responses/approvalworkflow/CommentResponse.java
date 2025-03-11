package com.capstone.contractmanagement.responses.approvalworkflow;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CommentResponse {
    private String comment;
    private String commenter; // Tên người comment (ví dụ: fullName)
    private LocalDateTime commentedAt; // Ngày giờ phút giây họ comment
}

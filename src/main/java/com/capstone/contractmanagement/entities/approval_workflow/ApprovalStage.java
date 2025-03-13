package com.capstone.contractmanagement.entities.approval_workflow;

import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.enums.ApprovalStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_stages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalStage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Số thứ tự của bước duyệt (ví dụ: 1 cho Manager A, 2 cho Manager B,...)
    @Column(name = "stage_order")
    private int stageOrder;

    // Tên người duyệt, hoặc nếu cần tham chiếu đến bảng User, bạn có thể dùng ManyToOne

    @ManyToOne
    @JoinColumn(name = "approver_id", nullable = false)
    @JsonIgnore
    private User approver;

    // Trạng thái duyệt của bước (có thể là enum: PENDING, APPROVED, REJECTED)
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ApprovalStatus status;

    // Thời gian duyệt (nếu có)
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // Nội dung comment (được lưu khi bước duyệt bị từ chối)
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    // Liên kết với quy trình phê duyệt
    @ManyToOne
    @JoinColumn(name = "workflow_id", nullable = false)
    @JsonIgnore
    private ApprovalWorkflow approvalWorkflow;

    @Column(name = "due_date")
    private LocalDateTime dueDate;
}

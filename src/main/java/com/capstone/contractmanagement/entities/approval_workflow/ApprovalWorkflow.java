package com.capstone.contractmanagement.entities.approval_workflow;


import com.capstone.contractmanagement.entities.contract.Contract;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "approval_workflows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalWorkflow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tên quy trình duyệt
    @Column(name = "name", length = 100)
    private String name;

    // Số bước duyệt tùy chỉnh (không tính bước cuối là CEO nếu đó là quy định)
    @Column(name = "custom_stages_count")
    private int customStagesCount;

    // Thời gian tạo quy trình
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Liên kết với hợp đồng (Contract)
    @OneToMany(mappedBy = "approvalWorkflow")
    private List<Contract> contracts = new ArrayList<>();

    // Danh sách các bước duyệt
    @OneToMany(mappedBy = "approvalWorkflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stageOrder ASC")
    @Builder.Default
    private List<ApprovalStage> stages = new ArrayList<>();
}

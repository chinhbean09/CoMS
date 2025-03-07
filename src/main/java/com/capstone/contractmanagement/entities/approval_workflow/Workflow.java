package com.capstone.contractmanagement.entities.approval_workflow;

import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.entities.contract.Contract;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "workflows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "workflow_id")
    private Long id;

    // Many-to-One với Contract
    @ManyToOne
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    // Many-to-One với User
    @ManyToOne
    @JoinColumn(name = "updated_by", nullable = false)
    private User updatedBy;

//    @Column(name = "status", nullable = false, length = 50)
//    private String status; // Đang xử lý, Đã phê duyệt, Hủy bỏ

    @Column(name = "comment", length = 500)
    private String comment;

    @Column(name = "create_at", nullable = false)
    private LocalDateTime createdAt;
}

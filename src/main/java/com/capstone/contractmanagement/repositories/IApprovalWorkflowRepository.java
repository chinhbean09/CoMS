package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.approval_workflow.ApprovalWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IApprovalWorkflowRepository extends JpaRepository<ApprovalWorkflow, Long> {
    List<ApprovalWorkflow> findByContractType_Id(Long contractTypeId);
    // Lấy 3 workflow mới nhất theo loại hợp đồng và theo người tạo
    List<ApprovalWorkflow> findTop3ByContractType_IdAndUser_IdOrderByCreatedAtDesc(
            Long contractTypeId,
            Long userId
    );
    List<ApprovalWorkflow> findByContractType_IdAndUser_IdOrderByCreatedAtDesc(
            Long contractTypeId,
            Long userId
    );
    List<ApprovalWorkflow> findTop3ByOrderByCreatedAtDesc();
    List<ApprovalWorkflow> findTop3ByUser_IdAndAddendumNotNullOrderByCreatedAtDesc(Long userId);
}

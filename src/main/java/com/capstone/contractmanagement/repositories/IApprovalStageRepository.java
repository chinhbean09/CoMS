package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.approval_workflow.ApprovalStage;
import com.capstone.contractmanagement.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface IApprovalStageRepository extends JpaRepository<ApprovalStage, Long> {
    List<ApprovalStage> findByStatusAndDueDateBefore(ApprovalStatus status, LocalDateTime dateTime);

}

package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.approval_workflow.ApprovalStage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IApprovalStageRepository extends JpaRepository<ApprovalStage, Long> {
}

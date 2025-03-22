package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.approval_workflow.ApprovalWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IApprovalWorkflowRepository extends JpaRepository<ApprovalWorkflow, Long> {
    List<ApprovalWorkflow> findByContractType_Id(Long contractTypeId);
    List<ApprovalWorkflow> findTop3ByContractType_IdOrderByCreatedAtDesc(Long contractTypeId);
    List<ApprovalWorkflow> findTop3ByAddendumType_IdOrderByCreatedAtDesc(Long addendumTypeId);
}

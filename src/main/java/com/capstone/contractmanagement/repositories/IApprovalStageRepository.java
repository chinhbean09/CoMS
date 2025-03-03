package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.ApprovalStage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IApprovalStageRepository extends JpaRepository<ApprovalStage, Long> {
}

package com.capstone.contractmanagement.services.workflow;

import com.capstone.contractmanagement.dtos.approvalworkflow.WorkflowDTO;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.entities.approval_workflow.Workflow;
import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.IContractRepository;
import com.capstone.contractmanagement.repositories.IWorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WorkflowService implements IWorkflowService{
    private final IWorkflowRepository workflowRepository;
    private final IContractRepository contractRepository;

    @Override
    public void createWorkflow(Contract contract, WorkflowDTO workflowDTO, User user) throws DataNotFoundException {

        Workflow workflow = Workflow.builder()
                .contract(contract)
                .comment(workflowDTO.getComment())
                .createdAt(LocalDateTime.now())
                .updatedBy(user)
                .build();

        workflowRepository.save(workflow);
    }
}

package com.capstone.contractmanagement.services.addendum;

import com.capstone.contractmanagement.dtos.addendum.AddendumDTO;
import com.capstone.contractmanagement.dtos.approvalworkflow.AddendumApprovalWorkflowDTO;
import com.capstone.contractmanagement.dtos.approvalworkflow.WorkflowDTO;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.enums.AddendumStatus;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.addendum.AddendumResponse;
import com.capstone.contractmanagement.responses.approvalworkflow.ApprovalWorkflowResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IAddendumService {

    AddendumResponse createAddendum(AddendumDTO addendumDTO) throws DataNotFoundException;

    List<AddendumResponse> getAllByContractId(Long contractId) throws DataNotFoundException;

    List<AddendumResponse> getAllByAddendumType(Long addendumTypeId) throws DataNotFoundException;

    String updateAddendum(Long addendumId, AddendumDTO addendumDTO) throws DataNotFoundException;

    void deleteAddendum(Long addendumId) throws DataNotFoundException;

    AddendumResponse getAddendumById(Long addendumId) throws DataNotFoundException;
    void assignApprovalWorkflowOfContractToAddendum(Long addendumId) throws DataNotFoundException;

    void assignWorkflowToAddendum(Long addendumId, Long workflowId) throws DataNotFoundException;
    void approvedStageForAddendum(Long addendumId, Long stageId) throws DataNotFoundException;
    void rejectStageForAddendum(Long addendumId, Long stageId, WorkflowDTO workflowDTO) throws DataNotFoundException;
    void resubmitAddendumForApproval(Long addendumId) throws DataNotFoundException;
    Page<AddendumResponse> getAddendaByUserWithFilters(
            Long userId,
            String keyword,
            List<AddendumStatus> statuses,
            List<Long> addendumTypeIds,
            int page,
            int size,
            User currentUser);
    List<AddendumResponse> getAddendaForManager(Long managerId);

    List<AddendumResponse> getAddendaForApprover(Long approverId);

    ApprovalWorkflowResponse getWorkflowByAddendumId(Long addendumId) throws DataNotFoundException;

    ApprovalWorkflowResponse createWorkflowForAddendum(AddendumApprovalWorkflowDTO approvalWorkflowDTO);

    List<ApprovalWorkflowResponse> getWorkflowByAddendumTypeId(Long addendumTypeId);
}

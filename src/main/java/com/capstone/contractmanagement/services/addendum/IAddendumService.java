package com.capstone.contractmanagement.services.addendum;

import com.capstone.contractmanagement.dtos.FileBase64DTO;
import com.capstone.contractmanagement.dtos.addendum.AddendumDTO;
import com.capstone.contractmanagement.dtos.approvalworkflow.AddendumApprovalWorkflowDTO;
import com.capstone.contractmanagement.dtos.approvalworkflow.WorkflowDTO;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.enums.AddendumStatus;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.addendum.AddendumResponse;
import com.capstone.contractmanagement.responses.approvalworkflow.ApprovalWorkflowResponse;
import com.capstone.contractmanagement.responses.approvalworkflow.CommentResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface IAddendumService {

    AddendumResponse createAddendum(AddendumDTO addendumDTO) throws DataNotFoundException;

    List<AddendumResponse> getAllByContractId(Long contractId) throws DataNotFoundException;


    String updateAddendum(Long addendumId, AddendumDTO addendumDTO) throws DataNotFoundException;

    void deleteAddendum(Long addendumId) throws DataNotFoundException;

    Optional<AddendumResponse> getAddendumById(Long addendumId) throws DataNotFoundException;
    void assignApprovalWorkflowOfContractToAddendum(Long addendumId) throws DataNotFoundException;

    void assignWorkflowToAddendum(Long addendumId, Long workflowId) throws DataNotFoundException;
    void approvedStageForAddendum(Long addendumId, Long stageId) throws DataNotFoundException;
    void rejectStageForAddendum(Long addendumId, Long stageId, WorkflowDTO workflowDTO) throws DataNotFoundException;
    void resubmitAddendumForApproval(Long addendumId) throws DataNotFoundException;
    Page<AddendumResponse> getAddendaByUserWithFilters(
            Long userId,
            String keyword,
            List<AddendumStatus> statuses,
            int page,
            int size,
            User currentUser);
    Page<AddendumResponse> getAddendaForManager(Long approverId, String keyword, int page, int size);

    Page<AddendumResponse> getAddendaForApprover(Long approverId, String keyword, int page, int size);

    ApprovalWorkflowResponse getWorkflowByAddendumId(Long addendumId) throws DataNotFoundException;

    ApprovalWorkflowResponse createWorkflowForAddendum(AddendumApprovalWorkflowDTO approvalWorkflowDTO);

    List<ApprovalWorkflowResponse> getWorkflowByAddendumTypeId();

    List<CommentResponse> getApprovalStageCommentDetailsByAddendumId(Long addendumId) throws DataNotFoundException;

    AddendumResponse duplicateAddendum(Long addendumId, Long contractId) throws DataNotFoundException;

    void uploadSignedAddendum(Long addendumId, List<MultipartFile> files) throws DataNotFoundException;

    List<String> getSignedAddendumUrl(Long addendumId) throws DataNotFoundException;

    void uploadFileBase64(Long addendumId, FileBase64DTO fileBase64DTO, String fileName) throws DataNotFoundException, IOException;
    void uploadPaymentBillUrls(Long paymentScheduleId, List<MultipartFile> files) throws DataNotFoundException;
    List<String> getBillUrlsByAddendumPaymentId(Long paymentId) throws DataNotFoundException;
}

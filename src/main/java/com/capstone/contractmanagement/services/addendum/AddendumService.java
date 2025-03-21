package com.capstone.contractmanagement.services.addendum;

import com.capstone.contractmanagement.dtos.addendum.AddendumDTO;
import com.capstone.contractmanagement.dtos.approvalworkflow.WorkflowDTO;
import com.capstone.contractmanagement.entities.Addendum;
import com.capstone.contractmanagement.entities.AddendumType;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.entities.approval_workflow.ApprovalStage;
import com.capstone.contractmanagement.entities.approval_workflow.ApprovalWorkflow;
import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.enums.AddendumStatus;
import com.capstone.contractmanagement.enums.ApprovalStatus;
import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.*;
import com.capstone.contractmanagement.responses.addendum.AddendumResponse;
import com.capstone.contractmanagement.responses.addendum.AddendumTypeResponse;
import com.capstone.contractmanagement.services.notification.INotificationService;
import com.capstone.contractmanagement.services.sendmails.IMailService;
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddendumService implements IAddendumService{
    private final IAddendumRepository addendumRepository;
    private final IContractRepository contractRepository;
    private final IAddendumTypeRepository addendumTypeRepository;
    private final IApprovalWorkflowRepository approvalWorkflowRepository;
    private final IMailService mailService;
    private final SimpMessagingTemplate messagingTemplate;
    private final INotificationService notificationService;
    private final IApprovalStageRepository approvalStageRepository;

    @Override
    @Transactional
    public AddendumResponse createAddendum(AddendumDTO addendumDTO) throws DataNotFoundException {
        Contract contract = contractRepository.findById(addendumDTO.getContractId())
                .orElseThrow(() -> new DataNotFoundException("Contract not found"));

        AddendumType addendumType = addendumTypeRepository.findById(addendumDTO.getAddendumTypeId())
                .orElseThrow(() -> new DataNotFoundException("Loại phụ lục không tìm thấy với id : " + addendumDTO.getAddendumTypeId()));
        if (contract.getStatus() == ContractStatus.ACTIVE
                || contract.getStatus() == ContractStatus.EXPIRED) {
            Addendum addendum = Addendum.builder()
                    .title(addendumDTO.getTitle())
                    .content(addendumDTO.getContent())
                    .effectiveDate(addendumDTO.getEffectiveDate())
                    .contractNumber(contract.getContractNumber())
                    .status(AddendumStatus.CREATED)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(null)
                    .addendumType(addendumType)
                    .contract(contract)
                    .build();
            addendumRepository.save(addendum);

            return AddendumResponse.builder()
                    .addendumId(addendum.getId())
                    .title(addendum.getTitle())
                    .content(addendum.getContent())
                    .contractNumber(addendum.getContractNumber())
                    .status(addendum.getStatus())
                    .contractId(addendum.getContract().getId())
                    .addendumType(AddendumTypeResponse.builder()
                            .addendumTypeId(addendum.getAddendumType().getId())
                            .name(addendum.getAddendumType().getName())
                            .build())
                    .effectiveDate(addendum.getEffectiveDate())
                    .createdAt(addendum.getCreatedAt())
                    .updatedAt(addendum.getUpdatedAt())
                    .build();
        }
        throw new DataNotFoundException("Cannot create addendum: Contract is not ACTIVE");
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddendumResponse> getAllByContractId(Long contractId) throws DataNotFoundException {
        // Kiểm tra hợp đồng có tồn tại không
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new DataNotFoundException("Contract not found with id: " + contractId));

        // Lấy danh sách phụ lục theo contract id (giả sử repository có method: findByContract_Id)
        List<Addendum> addenda = addendumRepository.findByContract(contract);

        // Nếu không có phụ lục, có thể trả về danh sách rỗng hoặc ném ngoại lệ
        if (addenda.isEmpty()) {
            throw new DataNotFoundException("No addendum found for contract id: " + contractId);
        }

        // Map entity thành DTO
        return addenda.stream()
                .map(addendum -> AddendumResponse.builder()
                        .addendumId(addendum.getId())
                        .title(addendum.getTitle())
                        .content(addendum.getContent())
                        .effectiveDate(addendum.getEffectiveDate())
                        .contractNumber(addendum.getContractNumber())
                        .addendumType(AddendumTypeResponse.builder()
                                .addendumTypeId(addendum.getAddendumType().getId())
                                .name(addendum.getAddendumType().getName())
                                .build())
                        .status(addendum.getStatus())
                        .contractId(addendum.getContract().getId())
                        .createdAt(addendum.getCreatedAt())
                        .updatedAt(addendum.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<AddendumResponse> getAllByAddendumType(Long addendumTypeId) throws DataNotFoundException {
        AddendumType addendumType = addendumTypeRepository.findById(addendumTypeId)
                .orElseThrow(() -> new DataNotFoundException("Addendum type not found with id: " + addendumTypeId));
        List<Addendum> addenda = addendumRepository.findByAddendumType(addendumType);
        return addenda.stream()
                .map(addendum -> AddendumResponse.builder()
                        .addendumId(addendum.getId())
                        .title(addendum.getTitle())
                        .content(addendum.getContent())
                        .effectiveDate(addendum.getEffectiveDate())
                        .contractNumber(addendum.getContractNumber())
                        .status(addendum.getStatus())
                        .contractId(addendum.getContract().getId())
                        .addendumType(AddendumTypeResponse.builder()
                                .addendumTypeId(addendum.getAddendumType().getId())
                                .name(addendum.getAddendumType().getName())
                                .build())
                        .createdAt(addendum.getCreatedAt())
                        .updatedAt(addendum.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public String updateAddendum(Long addendumId, AddendumDTO addendumDTO) throws DataNotFoundException {
        // Tìm phụ lục theo id
        Addendum addendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Addendum not found with id: " + addendumId));
        if (addendumDTO.getAddendumTypeId() != null) {
            AddendumType addendumType = addendumTypeRepository.findById(addendumDTO.getAddendumTypeId())
                    .orElseThrow(() -> new DataNotFoundException("Loại phụ lục không tìm thấy với id : " + addendumDTO.getAddendumTypeId()));
            addendum.setAddendumType(addendumType);
        }


        // Cập nhật thông tin phụ lục (không cập nhật createdAt)
        addendum.setTitle(addendumDTO.getTitle());
        addendum.setContent(addendumDTO.getContent());
        addendum.setEffectiveDate(addendumDTO.getEffectiveDate());
        addendum.setStatus(AddendumStatus.UPDATED);
        addendum.setUpdatedAt(LocalDateTime.now());

        addendumRepository.save(addendum);
        return "Addendum updated successfully.";
    }

    @Override
    @Transactional
    public void deleteAddendum(Long addendumId) throws DataNotFoundException {
        // Tìm phụ lục theo id
        Addendum addendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Addendum not found with id: " + addendumId));
        // Xóa phụ lục
        addendumRepository.delete(addendum);
    }

    @Override
    public AddendumResponse getAddendumById(Long addendumId) throws DataNotFoundException {
        Addendum addendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Addendum not found with id: " + addendumId));
        return AddendumResponse.builder()
                .addendumId(addendum.getId())
                .title(addendum.getTitle())
                .content(addendum.getContent())
                .contractNumber(addendum.getContractNumber())
                .status(addendum.getStatus())
                .contractId(addendum.getContract().getId())
                .addendumType(AddendumTypeResponse.builder()
                        .addendumTypeId(addendum.getAddendumType().getId())
                        .name(addendum.getAddendumType().getName())
                        .build())
                .effectiveDate(addendum.getEffectiveDate())
                .createdAt(addendum.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void assignApprovalWorkflowOfContractToAddendum(Long addendumId) throws DataNotFoundException {
        // Lấy phụ lục hợp đồng và hợp đồng gốc
        Addendum addendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Phụ lục hợp đồng không tìm thấy với id: " + addendumId));

        Contract contract = contractRepository.findById(addendum.getContract().getId())
                .orElseThrow(() -> new DataNotFoundException("Hợp đồng không tìm thấy"));

        ApprovalWorkflow contractApprovalWorkflow = contract.getApprovalWorkflow();
        if (contractApprovalWorkflow == null) {
            throw new DataNotFoundException("Contract approval workflow not found");
        }

        // Tạo một quy trình duyệt cho phụ lục hợp đồng bằng cách sao chép thông tin từ hợp đồng
        ApprovalWorkflow addendumApprovalWorkflow = new ApprovalWorkflow();
        addendumApprovalWorkflow.setName(contractApprovalWorkflow.getName());
        addendumApprovalWorkflow.setCreatedAt(LocalDateTime.now());
        addendumApprovalWorkflow.setContractType(contractApprovalWorkflow.getContractType());

        // Sao chép các bước duyệt từ quy trình duyệt của hợp đồng vào quy trình của phụ lục hợp đồng
        for (ApprovalStage stage : contractApprovalWorkflow.getStages()) {
            ApprovalStage addendumStage = new ApprovalStage();
            addendumStage.setStageOrder(stage.getStageOrder());
            addendumStage.setApprover(stage.getApprover());
            addendumStage.setStatus(ApprovalStatus.NOT_STARTED);
            addendumStage.setApprovalWorkflow(addendumApprovalWorkflow);
            addendumApprovalWorkflow.getStages().add(addendumStage);
        }

        approvalWorkflowRepository.save(addendumApprovalWorkflow);
        addendum.setApprovalWorkflow(addendumApprovalWorkflow);
        addendum.setStatus(AddendumStatus.APPROVAL_PENDING);
        addendumRepository.save(addendum);

        addendumApprovalWorkflow.getStages().stream()
                .min(Comparator.comparingInt(ApprovalStage::getStageOrder))
                .ifPresent(firstStage -> {
                    Map<String, Object> payload = new HashMap<>();
                    String notificationMessage = "Bạn có phụ lục hợp đồng cần phê duyệt đợt "
                            + firstStage.getStageOrder() + ": Phụ lục " + addendum.getTitle() + " của hợp đồng số " + addendum.getContract().getContractNumber();
                    payload.put("message", notificationMessage);
                    payload.put("addendumId", addendumId);
                    User firstApprover = firstStage.getApprover();
                    mailService.sendEmailAddendumReminder(addendum, firstApprover, firstStage);
                    notificationService.saveNotification(firstApprover, notificationMessage, addendum.getContract());
                    messagingTemplate.convertAndSendToUser(firstApprover.getFullName(), "/queue/notifications", payload);

                    // Đặt trạng thái duyệt cho bước này (nếu cần)
                    firstStage.setStatus(ApprovalStatus.APPROVING);
                    approvalStageRepository.save(firstStage);
                });
    }

    @Override
    @Transactional
    public void assignWorkflowToAddendum(Long addendumId, Long workflowId) throws DataNotFoundException {
        // Tìm phụ lục cần gán workflow
        Addendum addendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Phụ lục hợp đồng không tìm thấy với id " + addendumId));

        // Tìm workflow gốc theo workflowId
        ApprovalWorkflow originalWorkflow = approvalWorkflowRepository.findById(workflowId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.WORKFLOW_NOT_FOUND));

        ApprovalWorkflow workflowToAssign = originalWorkflow;

        // Nếu workflow đã được gán cho một phụ lục khác, thực hiện clone
        if (originalWorkflow.getContract() != null || originalWorkflow.getAddendum() != null) {
            // Clone thông tin cơ bản của workflow
            ApprovalWorkflow clonedWorkflow = ApprovalWorkflow.builder()
                    .name(originalWorkflow.getName())
                    .customStagesCount(originalWorkflow.getCustomStagesCount())
                    .createdAt(LocalDateTime.now())
                    .contractType(originalWorkflow.getContractType())  // Optional, nếu muốn
                    .build();

            // Clone các bước duyệt (stages) và đặt trạng thái về PENDING
            originalWorkflow.getStages().forEach(stage -> {
                ApprovalStage clonedStage = ApprovalStage.builder()
                        .stageOrder(stage.getStageOrder())
                        .approver(stage.getApprover())
                        .status(ApprovalStatus.NOT_STARTED)
                        .approvalWorkflow(clonedWorkflow)
                        .build();
                clonedWorkflow.getStages().add(clonedStage);
            });

            // Lưu workflow mới (với các stage tương ứng)
            approvalWorkflowRepository.save(clonedWorkflow);
            workflowToAssign = clonedWorkflow;
        }

        // Gán workflow (mới hoặc gốc nếu chưa gán) cho phụ lục
        addendum.setApprovalWorkflow(workflowToAssign);
        addendum.setStatus(AddendumStatus.APPROVAL_PENDING);
        addendumRepository.save(addendum);

        // Lưu lại lịch sử thay đổi (AuditTrail)
//        String changedBy = SecurityContextHolder.getContext().getAuthentication().getName();
//        logAuditTrailForAddendum(addendum, "UPDATE", "approvalWorkflow", oldStatus, workflowToAssign.getStatus().name(), changedBy);

        // Lấy stage có stageOrder nhỏ nhất để gửi thông báo
        workflowToAssign.getStages().stream()
                .min(Comparator.comparingInt(ApprovalStage::getStageOrder))
                .ifPresent(firstStage -> {
                    Map<String, Object> payload = new HashMap<>();
                    String notificationMessage = "Bạn có phụ lục hợp đồng cần phê duyệt đợt "
                            + firstStage.getStageOrder() + ": Phụ lục " + addendum.getTitle() + " của hợp đồng số " + addendum.getContract().getContractNumber();
                    payload.put("message", notificationMessage);
                    payload.put("addendumId", addendumId);
                    User firstApprover = firstStage.getApprover();
                    mailService.sendEmailAddendumReminder(addendum, firstApprover, firstStage);
                    notificationService.saveNotification(firstApprover, notificationMessage, addendum.getContract());
                    messagingTemplate.convertAndSendToUser(firstApprover.getFullName(), "/queue/notifications", payload);

                    // Đặt trạng thái duyệt cho bước này (nếu cần)
                    firstStage.setStatus(ApprovalStatus.APPROVING);
                    approvalStageRepository.save(firstStage);
                });
    }

    @Override
    @Transactional
    public void approvedStageForAddendum(Long addendumId, Long stageId) throws DataNotFoundException {
        // Lấy phụ lục theo addendumId
        Addendum addendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Addendum not found"));

        // Tìm ApprovalStage theo stageId
        ApprovalStage stage = approvalStageRepository.findById(stageId)
                .orElseThrow(() -> new DataNotFoundException("Approval stage not found"));

        // Lấy người dùng hiện tại từ SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        // Kiểm tra: chỉ cho phép người được giao duyệt thao tác
        if (!stage.getApprover().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền duyệt bước này.");
        }

        // Kiểm tra: nếu người duyệt đã xử lý (approve hoặc reject) ở bất kỳ bước nào của phụ lục, từ chối thao tác tiếp
        boolean alreadyProcessed = addendum.getApprovalWorkflow().getStages().stream()
                .filter(s -> s.getApprover().getId().equals(currentUser.getId()))
                .anyMatch(s -> s.getStatus() == ApprovalStatus.APPROVED || s.getStatus() == ApprovalStatus.REJECTED);
        if (alreadyProcessed) {
            throw new RuntimeException("Bạn đã xử lý phụ lục này rồi.");
        }

        // Kiểm tra nếu bước đã được xử lý rồi thì không cho duyệt lại
        if (stage.getStatus() == ApprovalStatus.APPROVED || stage.getStatus() == ApprovalStatus.REJECTED) {
            throw new RuntimeException("Bước này đã được xử lý.");
        }

        // Cập nhật trạng thái duyệt cho bước hiện tại
        stage.setStatus(ApprovalStatus.APPROVED);
        stage.setApprovedAt(LocalDateTime.now());
        approvalStageRepository.save(stage);

        // Nếu duyệt thành công, chuyển sang bước tiếp theo (nếu có)
        if (stage.getStatus() == ApprovalStatus.APPROVED) {
            ApprovalWorkflow workflow = stage.getApprovalWorkflow();
            Optional<ApprovalStage> nextStageOptional = workflow.getStages().stream()
                    .filter(s -> s.getStageOrder() > stage.getStageOrder())
                    .min(Comparator.comparingInt(ApprovalStage::getStageOrder));

            if (nextStageOptional.isPresent()) {
                ApprovalStage nextStage = nextStageOptional.get();
                nextStage.setStatus(ApprovalStatus.APPROVING);
                approvalStageRepository.save(nextStage);
                User nextApprover = nextStage.getApprover();

                // Tạo payload thông báo
                Map<String, Object> payload = new HashMap<>();
                String notificationMessage = "Bạn có phụ lục hợp đồng cần phê duyệt đợt " + nextStage.getStageOrder() +
                        ": Phụ lục " + addendum.getTitle();
                payload.put("message", notificationMessage);
                payload.put("addendumId", addendumId);

                // Gửi thông báo cho người duyệt tiếp theo
                mailService.sendEmailAddendumReminder(addendum, nextApprover, nextStage);
                notificationService.saveNotification(nextApprover, notificationMessage, addendum.getContract());
                messagingTemplate.convertAndSendToUser(nextApprover.getFullName(), "/queue/notifications", payload);
            } else {
                // Nếu không còn bước tiếp theo, cập nhật trạng thái phụ lục thành APPROVED
                addendum.setStatus(AddendumStatus.APPROVED);
                addendumRepository.save(addendum);
            }
        }
    }

    @Override
    @Transactional
    public void rejectStageForAddendum(Long addendumId, Long stageId, WorkflowDTO workflowDTO) throws DataNotFoundException {
        // Lấy phụ lục theo addendumId
        Addendum addendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Addendum not found"));

        // Tìm ApprovalStage theo stageId
        ApprovalStage stage = approvalStageRepository.findById(stageId)
                .orElseThrow(() -> new DataNotFoundException("Approval stage not found"));

        // Lấy người dùng hiện tại từ SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        // Kiểm tra: chỉ cho phép người được giao duyệt thao tác
        if (!stage.getApprover().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền từ chối bước này.");
        }

        // Kiểm tra: nếu người duyệt đã xử lý (approve hoặc reject) ở bất kỳ bước nào của phụ lục, từ chối thao tác tiếp
        boolean alreadyProcessed = addendum.getApprovalWorkflow().getStages().stream()
                .filter(s -> s.getApprover().getId().equals(currentUser.getId()))
                .anyMatch(s -> s.getStatus() == ApprovalStatus.APPROVED || s.getStatus() == ApprovalStatus.REJECTED);
        if (alreadyProcessed) {
            throw new RuntimeException("Bạn đã xử lý phụ lục này rồi.");
        }

        // Kiểm tra nếu bước đã được xử lý rồi thì không cho thao tác lại
        if (stage.getStatus() == ApprovalStatus.APPROVED || stage.getStatus() == ApprovalStatus.REJECTED) {
            throw new RuntimeException("Bước này đã được xử lý.");
        }

        // Cập nhật trạng thái bước là REJECTED, lưu comment và thời gian xử lý
        stage.setStatus(ApprovalStatus.REJECTED);
        stage.setApprovedAt(LocalDateTime.now());
        stage.setComment(workflowDTO.getComment());
        approvalStageRepository.save(stage);

        // Cập nhật trạng thái phụ lục thành REJECTED
        addendum.setStatus(AddendumStatus.REJECTED);
        addendumRepository.save(addendum);

        // Ghi audit trail
        String changedBy = currentUser.getUsername();

        // Gửi thông báo cho người tạo phụ lục để yêu cầu chỉnh sửa
        Map<String, Object> payload = new HashMap<>();
        String notificationMessage = "Bạn có phụ lục " + addendum.getTitle() + " của hợp đồng số " + addendum.getContractNumber() + " cần được chỉnh sửa";
        payload.put("message", notificationMessage);
        payload.put("addendumId", addendumId);
        mailService.sendUpdateAddendumReminder(addendum, addendum.getContract().getUser());
        notificationService.saveNotification(addendum.getContract().getUser(), notificationMessage, addendum.getContract());
        messagingTemplate.convertAndSendToUser(addendum.getContract().getUser().getFullName(), "/queue/notifications", payload);
    }

    @Override
    @Transactional
    public void resubmitAddendumForApproval(Long addendumId) throws DataNotFoundException {
        // Tìm phụ lục theo addendumId
        Addendum addendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phụ lục với id : " + addendumId));

        // Lấy workflow của phụ lục
        ApprovalWorkflow workflow = addendum.getApprovalWorkflow();
        if (workflow == null || workflow.getStages().isEmpty()) {
            throw new DataNotFoundException("Không tìm thấy quy trình phê duyệt cho phụ lục với id : " + addendumId);
        }

        // Reset lại tất cả các bước duyệt: đặt trạng thái về PENDING, xóa approvedAt và comment
        workflow.getStages().forEach(stage -> {
            stage.setStatus(ApprovalStatus.NOT_STARTED);
            stage.setApprovedAt(null);
            stage.setComment(null);
            approvalStageRepository.save(stage);
        });

        // Cập nhật lại trạng thái của phụ lục về APPROVAL_PENDING (để báo hiệu đang chờ duyệt lại)
        addendum.setStatus(AddendumStatus.APPROVAL_PENDING);
        addendumRepository.save(addendum);

        // Tìm bước duyệt đầu tiên (stage có stageOrder nhỏ nhất)
        Optional<ApprovalStage> firstStageOpt = workflow.getStages().stream()
                .min(Comparator.comparingInt(ApprovalStage::getStageOrder));

        if (firstStageOpt.isPresent()) {
            ApprovalStage firstStage = firstStageOpt.get();
            // Đặt trạng thái duyệt cho bước đầu tiên
            firstStage.setStatus(ApprovalStatus.APPROVING);
            approvalStageRepository.save(firstStage);
            User firstApprover = firstStage.getApprover();

            // Tạo payload thông báo cho người duyệt ở bước đầu tiên
            Map<String, Object> payload = new HashMap<>();
            String notificationMessage = "Phụ lục hợp đồng '" + addendum.getTitle() + "' đã được chỉnh sửa và nộp lại để phê duyệt. Bạn có phụ lục cần phê duyệt đợt "
                    + firstStage.getStageOrder();
            payload.put("message", notificationMessage);
            payload.put("addendumId", addendumId);

            // Gửi email nhắc nhở nếu cần
            mailService.sendEmailAddendumReminder(addendum, firstApprover, firstStage);
            // Lưu thông báo vào hệ thống thông báo
            notificationService.saveNotification(firstApprover, notificationMessage, addendum.getContract());
            // Gửi thông báo qua WebSocket
            messagingTemplate.convertAndSendToUser(firstApprover.getFullName(), "/queue/notifications", payload);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AddendumResponse> getAddendaByUserWithFilters(
            Long userId,
            String keyword,
            List<AddendumStatus> statuses,
            List<Long> addendumTypeIds,
            int page,
            int size,
            User currentUser) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        boolean hasSearch = keyword != null && !keyword.trim().isEmpty();
        boolean hasStatusFilter = statuses != null && !statuses.isEmpty();
        boolean hasTypeFilter = addendumTypeIds != null && !addendumTypeIds.isEmpty();

        Page<Addendum> addenda;

        boolean isCeo = Boolean.TRUE.equals(currentUser.getIsCeo());
        boolean isStaff = currentUser.getRole() != null &&
                "STAFF".equalsIgnoreCase(currentUser.getRole().getRoleName());

        if (isStaff && !isCeo) {
            if (hasStatusFilter) {
                if (hasTypeFilter) {
                    if (hasSearch) {
                        addenda = addendumRepository.findByContractUserIdAndKeywordAndStatusInAndAddendumTypeIdIn(
                                userId, keyword.trim(), statuses, addendumTypeIds, pageable);
                    } else {
                        addenda = addendumRepository.findByContractUserIdAndStatusInAndAddendumTypeIdIn(
                                userId, statuses, addendumTypeIds, pageable);
                    }
                } else {
                    if (hasSearch) {
                        addenda = addendumRepository.findByContractUserIdAndKeywordAndStatusIn(
                                userId, keyword.trim(), statuses, pageable);
                    } else {
                        addenda = addendumRepository.findByContractUserIdAndStatusIn(
                                userId, statuses, pageable);
                    }
                }
            } else {
                if (hasTypeFilter) {
                    if (hasSearch) {
                        addenda = addendumRepository.findByContractUserIdAndKeywordAndAddendumTypeIdIn(
                                userId, keyword.trim(), addendumTypeIds, pageable);
                    } else {
                        addenda = addendumRepository.findByContractUserIdAndAddendumTypeIdIn(
                                userId, addendumTypeIds, pageable);
                    }
                } else {
                    if (hasSearch) {
                        addenda = addendumRepository.findByContractUserIdAndKeyword(
                                userId, keyword.trim(), pageable);
                    } else {
                        addenda = addendumRepository.findByContractUserId(userId, pageable);
                    }
                }
            }
        } else if (isCeo) {
            if (hasStatusFilter) {
                if (hasTypeFilter) {
                    if (hasSearch) {
                        addenda = addendumRepository.findByKeywordAndStatusInAndAddendumTypeIdIn(
                                keyword.trim(), statuses, addendumTypeIds, pageable);
                    } else {
                        addenda = addendumRepository.findByStatusInAndAddendumTypeIdIn(
                                statuses, addendumTypeIds, pageable);
                    }
                } else {
                    if (hasSearch) {
                        addenda = addendumRepository.findByKeywordAndStatusIn(
                                keyword.trim(), statuses, pageable);
                    } else {
                        addenda = addendumRepository.findByStatusIn(statuses, pageable);
                    }
                }
            } else {
                if (hasTypeFilter) {
                    if (hasSearch) {
                        addenda = addendumRepository.findByKeywordAndAddendumTypeIdIn(
                                keyword.trim(), addendumTypeIds, pageable);
                    } else {
                        addenda = addendumRepository.findByAddendumTypeIdIn(addendumTypeIds, pageable);
                    }
                } else {
                    if (hasSearch) {
                        addenda = addendumRepository.findByKeyword(keyword.trim(), pageable);
                    } else {
                        addenda = addendumRepository.findAll(pageable);
                    }
                }
            }
        } else {
            addenda = Page.empty(pageable);
        }

        return addenda.map(this::mapToAddendumResponse);
    }



    private AddendumResponse mapToAddendumResponse(Addendum addendum) {
        return AddendumResponse.builder()
                .addendumId(addendum.getId())
                .title(addendum.getTitle())
                .content(addendum.getContent())
                .contractNumber(addendum.getContractNumber())
                .effectiveDate(addendum.getEffectiveDate())
                .status(addendum.getStatus())
                .createdAt(addendum.getCreatedAt())
                .updatedAt(addendum.getUpdatedAt())
                .contractId(addendum.getContract() != null ? addendum.getContract().getId() : null)
                .addendumType(
                        addendum.getAddendumType() != null ?
                                AddendumTypeResponse.builder()
                                        .addendumTypeId(addendum.getAddendumType().getId())
                                        .name(addendum.getAddendumType().getName())
                                        .build()
                                : null
                )
                .build();
    }
}

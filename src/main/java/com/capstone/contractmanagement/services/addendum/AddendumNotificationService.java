package com.capstone.contractmanagement.services.addendum;

import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.entities.addendum.Addendum;
import com.capstone.contractmanagement.entities.AuditTrail;
import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.enums.AddendumStatus;
import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.repositories.IAddendumRepository;
import com.capstone.contractmanagement.repositories.IContractRepository;
import com.capstone.contractmanagement.repositories.IAuditTrailRepository;
import com.capstone.contractmanagement.services.notification.INotificationService;
import com.capstone.contractmanagement.services.sendmails.IMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddendumNotificationService implements IAddendumNotificationService {
    private final IAddendumRepository addendumRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final INotificationService notificationService;
    private final IContractRepository contractRepository;
    private final IMailService mailService;
    private final IAuditTrailRepository auditTrailRepository;

    @Override
    @Scheduled(fixedDelay = 60000)
    public void checkAddendumDates() {
        LocalDateTime now = LocalDateTime.now();
        // Lọc ra các phụ lục đã được phê duyệt và chưa gửi thông báo
        List<Addendum> addendaToNotifyExtendContract = addendumRepository.findAll().stream()
                .filter(addendum -> addendum.getStatus() == AddendumStatus.APPROVED || addendum.getStatus() == AddendumStatus.SIGNED)
                .filter(addendum -> !addendum.getIsEffectiveNotified())
                .filter(addendum -> addendum.getExtendContractDate() != null)
                .filter(addendum -> addendum.getContractExpirationDate() != null)
                .filter(addendum -> addendum.getContract() != null)
                .filter(addendum -> !now.isBefore(addendum.getExtendContractDate())) // Kiểm tra hiệu lực của phụ lục
                .collect(Collectors.toList());

        for (Addendum addendum : addendaToNotifyExtendContract) {
            Contract contract = addendum.getContract();
            String message = "Hợp đồng số '" + addendum.getContractNumber() + "' đã được gia hạn thêm từ ngày '" + addendum.getEffectiveDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "' đến ngày '" + addendum.getExtendContractDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "'";
            sendNotification(addendum, message, true);
            mailService.sendEmailAddendumExtendedDate(addendum);
            addendum.setIsEffectiveNotified(true);
            addendumRepository.save(addendum);

            // Ghi audit trail cho thay đổi trạng thái hợp đồng
            ContractStatus oldStatus = contract.getStatus();
            contract.setStatus(ContractStatus.ACTIVE);
            contract.setIsExpiryNotified(true);
            contract.setIsEffectiveOverdueNotified(true);
            contractRepository.save(contract);
            logAuditTrailForContract(contract, "UPDATE", "status", oldStatus != null ? oldStatus.name() : null, ContractStatus.ACTIVE.name(), "System");
        }

        List<Addendum> addendaToNotifyExpiryContract = addendumRepository.findAll().stream()
                .filter(addendum -> addendum.getStatus() == AddendumStatus.APPROVED || addendum.getStatus() == AddendumStatus.SIGNED)
                .filter(addendum -> !addendum.getIsExpiryNotified())
                .filter(addendum -> addendum.getExtendContractDate() != null)
                .filter(addendum -> addendum.getContractExpirationDate() != null)
                .filter(addendum -> addendum.getContract() != null)
                .filter(addendum -> !now.isBefore(addendum.getContractExpirationDate())) // Kiểm tra hiệu lực của phụ lục
                .collect(Collectors.toList());

        for (Addendum addendum : addendaToNotifyExpiryContract) {
            Contract contract = addendum.getContract();
            String message = "Hợp đồng số '" + addendum.getContractNumber() + "' đã hết hạn ngày '" + addendum.getContractExpirationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "'";
            sendNotification(addendum, message, false);
            mailService.sendEmailAddendumEndExtendedDate(addendum);
            addendum.setIsExpiryNotified(true);
            addendumRepository.save(addendum);

            // Ghi audit trail cho thay đổi trạng thái hợp đồng
            ContractStatus oldStatus = contract.getStatus();
            contract.setStatus(ContractStatus.EXPIRED);
            contract.setIsExpiryNotified(true);
            contract.setIsEffectiveOverdueNotified(true);
            contractRepository.save(contract);
            logAuditTrailForContract(contract, "UPDATE", "status", oldStatus != null ? oldStatus.name() : null, ContractStatus.EXPIRED.name(), "System");
        }
    }

    private void sendNotification(Addendum addendum, String message, boolean isEffective) {
        User user = addendum.getUser();
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        payload.put("contractId", addendum.getId());
        messagingTemplate.convertAndSendToUser(user.getFullName(), "/queue/notifications", payload);
        notificationService.saveNotification(user, message, addendum.getContract());
        if (isEffective) {
            addendum.setIsEffectiveNotified(true);
        } else {
            addendum.setIsExpiryNotified(true);
        }
        addendumRepository.save(addendum);
    }

    private void logAuditTrailForContract(Contract contract, String action, String fieldName, String oldValue, String newValue, String changedBy) {
        String oldStatusVi = oldValue != null ? translateContractStatusToVietnamese(oldValue) : null;
        String newStatusVi = newValue != null ? translateContractStatusToVietnamese(newValue) : null;

        String changeSummary;
        if ("CREATED".equalsIgnoreCase(newValue)) {
            changeSummary = "Đã tạo mới hợp đồng với trạng thái '" + (newStatusVi != null ? newStatusVi : "Không có") + "'";
        } else {
            changeSummary = String.format("Đã cập nhật trạng thái hợp đồng từ '%s' sang '%s'",
                    oldStatusVi != null ? oldStatusVi : "Không có",
                    newStatusVi != null ? newStatusVi : "Không có");
        }

        AuditTrail auditTrail = AuditTrail.builder()
                .contract(contract)
                .entityName("Contract")
                .entityId(contract.getId())
                .action(action)
                .fieldName(fieldName)
                .oldValue(oldStatusVi)
                .newValue(newStatusVi)
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .changeSummary(changeSummary)
                .build();
        auditTrailRepository.save(auditTrail);
    }

    // Giả định phương thức này tồn tại để dịch ContractStatus sang tiếng Việt
    private String translateContractStatusToVietnamese(String status) {
        switch (status) {
            case "CREATED":
                return "Tạo mới";
            case "ACTIVE":
                return "Đang hoạt động";
            case "EXPIRED":
                return "Hết hạn";
            default:
                return status;
        }
    }
}
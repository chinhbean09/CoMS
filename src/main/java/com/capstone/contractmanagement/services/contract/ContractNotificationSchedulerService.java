package com.capstone.contractmanagement.services.contract;

import com.capstone.contractmanagement.entities.Role;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.entities.addendum.Addendum;
import com.capstone.contractmanagement.entities.AuditTrail;
import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.enums.AddendumStatus;
import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.repositories.IAppConfigRepository;
import com.capstone.contractmanagement.repositories.IContractRepository;
import com.capstone.contractmanagement.repositories.IAuditTrailRepository;
import com.capstone.contractmanagement.repositories.IUserRepository;
import com.capstone.contractmanagement.services.app_config.IAppConfigService;
import com.capstone.contractmanagement.services.notification.INotificationService;
import com.capstone.contractmanagement.services.sendmails.IMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractNotificationSchedulerService implements IContractNotificationSchedulerService {
    private final IContractRepository contractRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final INotificationService notificationService;
    private final IMailService mailService;
    private final IAuditTrailRepository auditTrailRepository;
    private final IUserRepository userRepository;
    private final IAppConfigService appConfigService;

    @Override
    @Transactional
    @Scheduled(fixedDelay = 60000)
    public void checkContractDates() {
        LocalDateTime now = LocalDateTime.now();

        // Bước 1: Thông báo sắp hiệu lực
        List<Contract> contractsToEffectiveNotify = contractRepository.findAll().stream()
                .filter(contract -> contract.getEffectiveDate() != null)
                .filter(contract -> contract.getNotifyEffectiveDate() != null)
                .filter(contract -> contract.getStatus() == ContractStatus.SIGNED || contract.getStatus() == ContractStatus.APPROVED || contract.getStatus() == ContractStatus.ACTIVE)
                .filter(contract -> Boolean.FALSE.equals(contract.getIsEffectiveNotified()))
                .filter(Contract::getIsLatestVersion)
                .filter(contract -> !now.isBefore(contract.getNotifyEffectiveDate()))
                .collect(Collectors.toList());

        for (Contract contract : contractsToEffectiveNotify) {
            String message = "Hợp đồng '" + contract.getTitle() + "' sẽ có hiệu lực vào ngày " + contract.getEffectiveDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            sendNotification(contract, message, true);
            mailService.sendEmailContractEffectiveDate(contract);
            contractRepository.save(contract);
        }

        List<Contract> contractsEffectiveNotify = contractRepository.findAll().stream()
                .filter(contract -> contract.getEffectiveDate() != null)
                .filter(contract -> contract.getNotifyEffectiveDate() != null)
                .filter(contract -> contract.getStatus() == ContractStatus.SIGNED || contract.getStatus() == ContractStatus.APPROVED)
                //.filter(contract -> Boolean.FALSE.equals(contract.getIsEffectiveNotified()))
                .filter(Contract::getIsLatestVersion)
                .filter(contract -> !now.isBefore(contract.getEffectiveDate()))
                .collect(Collectors.toList());

        for (Contract contract : contractsToEffectiveNotify) {
            String message = "Hợp đồng '" + contract.getTitle() + "'bắt đầu có hiệu lực vào ngày " + contract.getEffectiveDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            sendNotification(contract, message, true);
            //mailService.sendEmailContractEffectiveDate(contract);
            contract.setStatus(ContractStatus.ACTIVE);
            contractRepository.save(contract);
        }

        // Bước 2: Thông báo sắp hết hạn
        List<Contract> contractsToExpiryNotify = contractRepository.findAll().stream()
                .filter(contract -> contract.getExpiryDate() != null)
                .filter(contract -> contract.getNotifyExpiryDate() != null)
                .filter(contract -> contract.getStatus() == ContractStatus.ACTIVE)
                .filter(contract -> Boolean.FALSE.equals(contract.getIsExpiryNotified()))
                .filter(Contract::getIsLatestVersion)
                .filter(contract -> !now.isBefore(contract.getNotifyExpiryDate()))
                .collect(Collectors.toList());

        for (Contract contract : contractsToExpiryNotify) {
            String message = "Hợp đồng '" + contract.getTitle() + "' sắp hết hạn vào ngày " + contract.getExpiryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            sendNotification(contract, message, false);
            mailService.sendEmailContractExpiryDate(contract);
        }

        // Bước 3: Thông báo quá hạn hiệu lực
        List<Contract> contractsEffectiveOverdue = contractRepository.findAll().stream()
                .filter(contract -> contract.getExpiryDate() != null)
                .filter(contract -> contract.getStatus() == ContractStatus.ACTIVE)
                .filter(contract -> Boolean.FALSE.equals(contract.getIsEffectiveOverdueNotified()))
                .filter(contract -> now.isAfter(contract.getExpiryDate()))
                .filter(Contract::getIsLatestVersion)
                .collect(Collectors.toList());

        for (Contract contract : contractsEffectiveOverdue) {
            String message = "Hợp đồng '" + contract.getTitle() + "' đã quá hạn hiệu lực từ ngày " + contract.getExpiryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            sendOverdueNotification(contract, message);
            mailService.sendEmailContractOverdue(contract);

            // Ghi audit trail cho thay đổi trạng thái hợp đồng
            ContractStatus oldStatus = contract.getStatus();
            contract.setStatus(ContractStatus.EXPIRED);
            contractRepository.save(contract);
            logAuditTrailForContract(contract, "UPDATE", "status", oldStatus != null ? oldStatus.name() : null, ContractStatus.EXPIRED.name(), "System");
        }
    }

    /**
     * Gửi thông báo cho người dùng
     */
    private void sendNotification(Contract contract, String message, boolean isEffective) {
        User user = contract.getUser();
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        payload.put("contractId", contract.getId());
        messagingTemplate.convertAndSendToUser(user.getFullName(), "/queue/notifications", payload);
        notificationService.saveNotification(user, message, contract);
        if (isEffective) {
            contract.setIsEffectiveNotified(true);
        } else {
            contract.setIsExpiryNotified(true);
        }
        contractRepository.save(contract);
    }

    /**
     * Gửi thông báo tả
     */
    private void sendOverdueNotification(Contract contract, String message) {
        User user = contract.getUser();
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        payload.put("contractId", contract.getId());
        messagingTemplate.convertAndSendToUser(user.getFullName(), "/queue/notifications", payload);
        notificationService.saveNotification(user, message, contract);
        contract.setIsEffectiveOverdueNotified(true);
        contractRepository.save(contract);
    }

    /**
     * Ghi audit trail cho thay đổi trạng thái hợp đồng
     */
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

    //@Scheduled(fixedDelay = 30 * 60 * 1000)
    @Scheduled(cron = "0 0 0,12 * * *")
    protected void notificationForContractExpired() {
        LocalDateTime now = LocalDateTime.now();
        List<Contract> contracts = contractRepository.findAll().stream()
                .filter(contract -> contract.getStatus() == ContractStatus.APPROVAL_PENDING
                        || contract.getStatus() == ContractStatus.CREATED
                        || contract.getStatus() == ContractStatus.UPDATED)
                .filter(contract -> contract.getIsLatestVersion() == true)
                .filter(c -> {
                    long days = ChronoUnit.DAYS.between(
                            now.toLocalDate(),
                            c.getSigningDate().toLocalDate()
                    );
                    if (appConfigService.getApprovalDeadlineValue() == 0) {
                        return days <= 2;
                    }
                    return days <= appConfigService.getApprovalDeadlineValue();
                })
                .collect(Collectors.toList());

        if (contracts.isEmpty()) return;
        User director = userRepository.findAll().stream()
                .filter(user -> user.getRole() != null && Role.DIRECTOR.equalsIgnoreCase(user.getRole().getRoleName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người có vai trò giám đốc"));
        for (Contract notiContract: contracts) {
            String dateStr = notiContract.getSigningDate()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String message = String.format(
                    "Hợp đồng số \"%s\" dự kiến ký vào ngày %s. Vui lòng nhắc nhở duyệt kịp thời!",
                    notiContract.getContractNumber(), dateStr
            );

            // gửi in‑app + lưu notification
            Map<String,Object> payload = new HashMap<>();
            payload.put("message", message);
            payload.put("contractId", notiContract.getId());

                // giả sử bạn dùng fullName làm destination
                messagingTemplate
                        .convertAndSendToUser(director.getFullName(),
                                "/queue/notifications",
                                payload);
                notificationService.saveNotification(director, message, notiContract);
                // (tuỳ chọn) gửi email cho director
                // mailService.sendEmailContractSigningReminder(user, contract);

        }
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    protected void markContractsEnded() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);

        // Lấy tất cả hợp đồng latest version đã EXPIRED
        List<Contract> all = contractRepository.findByStatusAndIsLatestVersion(
                ContractStatus.EXPIRED, true
        );

        List<Contract> toEnd = new ArrayList<>();
        for (Contract c : all) {
            LocalDateTime actualExpiry = getActualExpiryDateFromAddendum(c);
            if (actualExpiry.isBefore(cutoff)) {
                toEnd.add(c);
            }
        }

        if (toEnd.isEmpty()) return;

        // Đánh dấu, gửi thông báo và ghi audit trail
        toEnd.forEach(c -> {
            ContractStatus oldStatus = c.getStatus(); // Lưu trạng thái cũ
            c.setStatus(ContractStatus.ENDED);

            // --- Ghi audit trail cho thay đổi trạng thái ---
            logAuditTrailForContract(
                    c,
                    "UPDATE",
                    "status",
                    oldStatus != null ? oldStatus.name() : null,
                    ContractStatus.ENDED.name(),
                    "System"
            );

            // --- Gửi thông báo ---
            User owner = c.getUser();
            String message = String.format(
                    "Hợp đồng \"%s\" đã kết thúc sau 30 ngày hết hạn.",
                    c.getTitle()
            );
            Map<String, Object> payload = Map.of(
                    "message", message,
                    "contractId", c.getId()
            );

            // 1) Gửi WebSocket
            messagingTemplate.convertAndSendToUser(
                    owner.getFullName(),
                    "/queue/notifications",
                    payload
            );
            // 2) Lưu vào hệ thống notification
            notificationService.saveNotification(owner, message, c);
            // 3) (Tùy chọn) Gửi email
            // mailService.sendEmailContractEnded(owner, c);
        });

        // Lưu tất cả thay đổi
        contractRepository.saveAll(toEnd);
    }

    /**
     * Lấy ngày hết hạn thực tế (xét phụ lục nếu có)
     */
    private LocalDateTime getActualExpiryDateFromAddendum(Contract contract) {
        return contract.getAddenda().stream()
                .filter(a -> a.getStatus() == AddendumStatus.SIGNED)
                .map(Addendum::getContractExpirationDate)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(contract.getExpiryDate());
    }
    /**
     * Dịch trạng thái hợp đồng sang tiếng Việt
     */
    private String translateContractStatusToVietnamese(String status) {
        switch (status) {
            case "CREATED":
                return "Tạo mới";
            case "SIGNED":
                return "Đã ký";
            case "APPROVED":
                return "Đã phê duyệt";
            case "ACTIVE":
                return "Đang hoạt động";
            case "EXPIRED":
                return "Hết hạn";
            case "ENDED":
                return "Kết thúc";
            default:
                return status;
        }
    }

    /**
     * Lấy ngày hiệu lực thực tế (từ phụ lục nếu có, nếu không thì từ hợp đồng gốc)
     */
    private LocalDateTime getActualEffectiveDate(Contract contract) {
        return contract.getAddenda().stream()
                .filter(addendum -> addendum.getStatus() == AddendumStatus.APPROVED || addendum.getStatus() == AddendumStatus.SIGNED)
                .map(Addendum::getExtendContractDate)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(contract.getEffectiveDate());
    }

    /**
     * Lấy ngày hết hạn thực tế (từ phụ lục nếu có, nếu không thì từ hợp đồng gốc)
     */
    private LocalDateTime getActualExpiryDate(Contract contract) {
        return contract.getAddenda().stream()
                .filter(addendum -> addendum.getStatus() == AddendumStatus.APPROVED || addendum.getStatus() == AddendumStatus.SIGNED)
                .map(Addendum::getContractExpirationDate)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(contract.getExpiryDate());
    }

    /**
     * Lấy ngày thông báo hiệu lực từ phụ lục (nếu có)
     */
    private LocalDateTime getNotifyEffectiveDateFromAddendum(Contract contract) {
        return contract.getAddenda().stream()
                .filter(addendum -> addendum.getStatus() == AddendumStatus.APPROVED || addendum.getStatus() == AddendumStatus.SIGNED)
                .map(Addendum::getExtendContractDate)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(contract.getNotifyEffectiveDate());
    }

    /**
     * Lấy ngày thông báo hết hạn từ phụ lục (nếu có)
     */
    private LocalDateTime getNotifyExpiryDateFromAddendum(Contract contract) {
        return contract.getAddenda().stream()
                .filter(addendum -> addendum.getStatus() == AddendumStatus.APPROVED || addendum.getStatus() == AddendumStatus.SIGNED)
                .map(Addendum::getContractExpirationDate)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(contract.getNotifyExpiryDate());
    }
}
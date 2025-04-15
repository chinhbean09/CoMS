package com.capstone.contractmanagement.services.contract;

import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.entities.addendum.Addendum;
import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.enums.AddendumStatus;
import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.repositories.IContractRepository;
import com.capstone.contractmanagement.services.notification.INotificationService;
import com.capstone.contractmanagement.services.sendmails.IMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractNotificationSchedulerService implements IContractNotificationSchedulerService {
    private final IContractRepository contractRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final INotificationService notificationService;
    private final IMailService mailService;

    // Số ngày trước khi hiệu lực hoặc hết hạn để gửi thông báo
    private static final int EFFECTIVE_NOTIFY_DAYS = 5;
    private static final int EXPIRY_NOTIFY_DAYS = 5;

    @Override
    @Transactional
    //@Scheduled(fixedDelay = 60000)
    @Scheduled(cron = "0 0 8 * * ?") // Chạy hàng ngày lúc 8h sáng
    public void checkContractDates() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Thông báo sắp hiệu lực
        List<Contract> contractsToEffectiveNotify = contractRepository.findAll().stream()
                .filter(contract -> getActualEffectiveDate(contract) != null) // Kiểm tra ngày hiệu lực thực tế từ phụ lục
                .filter(contract -> contract.getNotifyEffectiveDate() != null)
                .filter(contract -> contract.getStatus() == ContractStatus.SIGNED || contract.getStatus() == ContractStatus.APPROVED)
                //.filter(contract -> Boolean.FALSE.equals(contract.getIsEffectiveNotified()))
                .filter(Contract::getIsLatestVersion)
                .filter(contract -> !now.isBefore(getNotifyEffectiveDateFromAddendum(contract)))
                .collect(Collectors.toList());

        for (Contract contract : contractsToEffectiveNotify) {
            String message = contract.getNotifyEffectiveContent() != null
                    ? contract.getNotifyEffectiveContent()
                    : "Hợp đồng '" + contract.getTitle() + "' sẽ có hiệu lực vào ngày " + contract.getEffectiveDate();
            sendNotification(contract, message, true);
            mailService.sendEmailContractEffectiveDate(contract);
            contract.setStatus(ContractStatus.ACTIVE);
            contractRepository.save(contract);
        }

        // 2. Thông báo sắp hết hạn
        List<Contract> contractsToExpiryNotify = contractRepository.findAll().stream()
                .filter(contract -> getActualExpiryDate(contract) != null)
                .filter(contract -> contract.getNotifyExpiryDate() != null)
                .filter(contract -> contract.getStatus() == ContractStatus.ACTIVE)
                .filter(contract -> Boolean.FALSE.equals(contract.getIsExpiryNotified()))
                .filter(Contract::getIsLatestVersion)
                .filter(contract -> !now.isBefore(getNotifyExpiryDateFromAddendum(contract)))
                .collect(Collectors.toList());

        for (Contract contract : contractsToExpiryNotify) {
            String message = contract.getNotifyExpiryContent() != null
                    ? contract.getNotifyExpiryContent()
                    : "Hợp đồng '" + contract.getTitle() + "' sắp hết hạn vào ngày " + contract.getExpiryDate();
            sendNotification(contract, message, false);
            mailService.sendEmailContractExpiryDate(contract);
        }

        // 3. Thông báo quá hạn hiệu lực
        List<Contract> contractsEffectiveOverdue = contractRepository.findAll().stream()
                .filter(contract -> getActualExpiryDate(contract) != null)
                .filter(contract -> contract.getStatus() == ContractStatus.ACTIVE)
                //.filter(contract -> Boolean.FALSE.equals(contract.getIsEffectiveOverdueNotified()))
                .filter(contract -> !now.isBefore(getActualExpiryDate(contract)))
                .filter(Contract::getIsLatestVersion)
                .collect(Collectors.toList());

        for (Contract contract : contractsEffectiveOverdue) {
            String message = "Hợp đồng '" + contract.getTitle() + "' đã quá hạn hiệu lực từ ngày " + contract.getExpiryDate();
            sendOverdueNotification(contract, message);
            mailService.sendEmailContractOverdue(contract);
            contract.setStatus(ContractStatus.EXPIRED);
            contractRepository.save(contract);
        }
    }

    private void sendNotification(Contract contract, String message, boolean isEffective) {
        User user = contract.getUser();

        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        payload.put("contractId", contract.getId());

        messagingTemplate.convertAndSendToUser(user.getFullName(), "/queue/notifications", payload);
        notificationService.saveNotification(user, message, contract);

        // Cập nhật trạng thái thông báo
        if (isEffective) {
            contract.setIsEffectiveNotified(true);
        } else {
            contract.setIsExpiryNotified(true);
        }
        // Lưu lại hợp đồng sau khi cập nhật thông báo đã gửi
        contractRepository.save(contract);
    }

    private void sendOverdueNotification(Contract contract, String message) {
        User user = contract.getUser();

        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        payload.put("contractId", contract.getId());

        messagingTemplate.convertAndSendToUser(user.getFullName(), "/queue/notifications", payload);
        notificationService.saveNotification(user, message, contract);

        // Cập nhật trạng thái thông báo
        contract.setIsEffectiveOverdueNotified(true);
        // Lưu lại hợp đồng sau khi cập nhật thông báo đã gửi
        contractRepository.save(contract);
    }

    private LocalDateTime getActualEffectiveDate(Contract contract) {
        // Kiểm tra nếu có phụ lục gia hạn
        return contract.getAddenda().stream()
                .filter(addendum -> addendum.getStatus() == AddendumStatus.APPROVED)
                .map(Addendum::getExtendContractDate) // Ngày gia hạn từ phụ lục
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo) // Lấy ngày gia hạn sớm nhất
                .orElse(contract.getEffectiveDate()); // Nếu không có phụ lục, dùng ngày hiệu lực ban đầu
    }

    private LocalDateTime getActualExpiryDate(Contract contract) {
        // Kiểm tra nếu có phụ lục thay đổi ngày hết hạn
        return contract.getAddenda().stream()
                .filter(addendum -> addendum.getStatus() == AddendumStatus.APPROVED)
                .map(Addendum::getContractExpirationDate) // Ngày hết hạn từ phụ lục
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo) // Lấy ngày hết hạn muộn nhất
                .orElse(contract.getExpiryDate()); // Nếu không có phụ lục, dùng ngày hết hạn ban đầu
    }

    private LocalDateTime getNotifyEffectiveDateFromAddendum(Contract contract) {
        // Kiểm tra nếu có phụ lục đã duyệt, lấy ngày gia hạn hợp đồng từ phụ lục
        return contract.getAddenda().stream()
                .filter(addendum -> addendum.getStatus() == AddendumStatus.APPROVED)
                .map(Addendum::getExtendContractDate)  // Ngày gia hạn hợp đồng từ phụ lục
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)  // Lấy ngày gia hạn sớm nhất trong các phụ lục
                .orElse(contract.getNotifyEffectiveDate());  // Nếu không có phụ lục, dùng ngày thông báo hiệu lực trong hợp đồng gốc
    }

    private LocalDateTime getNotifyExpiryDateFromAddendum(Contract contract) {
        // Kiểm tra nếu có phụ lức thay đổi ngày hết hazole, lấy ngày hết hazole hợp đồng từ phụ lức
        return contract.getAddenda().stream()
                .filter(addendum -> addendum.getStatus() == AddendumStatus.APPROVED)
                .map(Addendum::getContractExpirationDate)  // Ngày hết hazole hợp đồng từ phụ lức
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)  // Lấy ngày hết hazole muộn nhất trong các phụ lức
                .orElse(contract.getNotifyExpiryDate());  // Nếu không có phụ lức, dùng ngày hết hazole trong hợp đồng gốc
    }
}

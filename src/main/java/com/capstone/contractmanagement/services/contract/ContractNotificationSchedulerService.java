package com.capstone.contractmanagement.services.contract;

import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.repositories.IContractRepository;
import com.capstone.contractmanagement.services.notification.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractNotificationSchedulerService implements IContractNotificationSchedulerService {
    private final IContractRepository contractRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final INotificationService notificationService;

    // Số ngày trước khi hiệu lực hoặc hết hạn để gửi thông báo
    private static final int EFFECTIVE_NOTIFY_DAYS = 5;
    private static final int EXPIRY_NOTIFY_DAYS = 5;

    @Override
    @Scheduled(cron = "0 0 8 * * ?") // Chạy định kỳ hàng ngày lúc 8 giờ sáng
    public void checkContractDates() {
        LocalDateTime now = LocalDateTime.now();

        // Tìm hợp đồng sắp có hiệu lực: Ngày hiệu lực trừ EFFECTIVE_NOTIFY_DAYS <= hiện tại < ngày hiệu lực
        List<Contract> contractsToEffectiveNotify = contractRepository.findAll().stream()
                .filter(contract -> contract.getEffectiveDate() != null)
                .filter(contract -> !contract.getIsEffectiveNotified())  // Chưa gửi thông báo hiệu lực
                .filter(contract -> {
                    LocalDateTime notifyDate = contract.getEffectiveDate().minusDays(EFFECTIVE_NOTIFY_DAYS);
                    return now.isAfter(notifyDate) && now.isBefore(contract.getEffectiveDate());
                })
                .collect(Collectors.toList());

        for (Contract contract : contractsToEffectiveNotify) {
            String message = "Hợp đồng '" + contract.getTitle() + "' sẽ có hiệu lực vào ngày "
                    + contract.getEffectiveDate();
            sendNotification(contract, message, true);
        }

        // Tìm hợp đồng sắp hết hạn: Ngày hết hạn trừ EXPIRY_NOTIFY_DAYS <= hiện tại < ngày hết hạn
        List<Contract> contractsToExpiryNotify = contractRepository.findAll().stream()
                .filter(contract -> contract.getExpiryDate() != null)
                .filter(contract -> !contract.getIsExpiryNotified())  // Chưa gửi thông báo hết hạn
                .filter(contract -> {
                    LocalDateTime notifyDate = contract.getExpiryDate().minusDays(EXPIRY_NOTIFY_DAYS);
                    return now.isAfter(notifyDate) && now.isBefore(contract.getExpiryDate());
                })
                .collect(Collectors.toList());

        for (Contract contract : contractsToExpiryNotify) {
            String message = "Hợp đồng '" + contract.getTitle() + "' sắp hết hạn vào ngày "
                    + contract.getExpiryDate();
            sendNotification(contract, message, false);
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
}

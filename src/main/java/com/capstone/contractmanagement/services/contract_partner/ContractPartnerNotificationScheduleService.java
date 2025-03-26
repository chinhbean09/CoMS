package com.capstone.contractmanagement.services.contract_partner;

import com.capstone.contractmanagement.entities.ContractPartner;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.repositories.IContractPartnerRepository;
import com.capstone.contractmanagement.repositories.IPaymentScheduleRepository;
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
public class ContractPartnerNotificationScheduleService implements IContractPartnerNotificationScheduleService {
    private final IPaymentScheduleRepository paymentScheduleRepository;
    private final IContractPartnerRepository contractPartnerRepository;

    private final SimpMessagingTemplate messagingTemplate;
    private final INotificationService notificationService;

    // Số ngày trước khi hiệu lực hoặc hết hạn để gửi thông báo
    private static final int EFFECTIVE_NOTIFY_DAYS = 5;
    private static final int EXPIRY_NOTIFY_DAYS = 5;

    @Scheduled(cron = "0 0 8 * * ?") // Chạy định kỳ hàng ngày lúc 8 giờ sáng
    public void checkContractPartnerDates() {
        LocalDateTime now = LocalDateTime.now();

        // Tìm đối tác hợp đồng sắp có hiệu lực
        List<ContractPartner> partnersToEffectiveNotify = contractPartnerRepository.findAll().stream()
                .filter(cp -> cp.getEffectiveDate() != null)
                .filter(cp -> Boolean.FALSE.equals(cp.getIsEffectiveNotified())) // Chưa gửi thông báo hiệu lực
                .filter(cp -> {
                    LocalDateTime notifyDate = cp.getEffectiveDate().minusDays(EFFECTIVE_NOTIFY_DAYS);
                    return now.isAfter(notifyDate) && now.isBefore(cp.getEffectiveDate());
                })
                .collect(Collectors.toList());

        for (ContractPartner cp : partnersToEffectiveNotify) {
            String message = "Hợp đồng đối tác '" + cp.getTitle() + "' sẽ có hiệu lực vào ngày " + cp.getEffectiveDate();
            sendNotification(cp, message, true);
        }

        // Tìm đối tác hợp đồng sắp hết hạn
        List<ContractPartner> partnersToExpiryNotify = contractPartnerRepository.findAll().stream()
                .filter(cp -> cp.getExpiryDate() != null)
                .filter(cp -> Boolean.FALSE.equals(cp.getIsExpiryNotified())) // Chưa gửi thông báo hết hạn
                .filter(cp -> {
                    LocalDateTime notifyDate = cp.getExpiryDate().minusDays(EXPIRY_NOTIFY_DAYS);
                    return now.isAfter(notifyDate) && now.isBefore(cp.getExpiryDate());
                })
                .collect(Collectors.toList());

        for (ContractPartner cp : partnersToExpiryNotify) {
            String message = "Hợp đồng đối tác '" + cp.getTitle() + "' sắp hết hạn vào ngày " + cp.getExpiryDate();
            sendNotification(cp, message, false);
        }
    }

    /**
     * Gửi thông báo đến người dùng và cập nhật trạng thái thông báo của ContractPartner.
     *
     * @param cp         đối tượng ContractPartner
     * @param message    nội dung thông báo
     * @param isEffective nếu true: thông báo hiệu lực, nếu false: thông báo hết hạn
     */
    private void sendNotification(ContractPartner cp, String message, boolean isEffective) {
        // Lấy thông tin người dùng từ ContractPartner
        User user = cp.getUser();

        // Tạo payload thông báo
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        payload.put("contractPartnerId", cp.getId());

        // Gửi thông báo qua WebSocket
        messagingTemplate.convertAndSendToUser(user.getFullName(), "/queue/notifications", payload);

        // Lưu thông báo vào hệ thống (Notification entity)
        //notificationService.saveNotification(user, message, cp);

        // Cập nhật trạng thái thông báo đã được gửi
        if (isEffective) {
            cp.setIsEffectiveNotified(true);
        } else {
            cp.setIsExpiryNotified(true);
        }

        // Lưu lại thay đổi trong cơ sở dữ liệu
        contractPartnerRepository.save(cp);
    }
}

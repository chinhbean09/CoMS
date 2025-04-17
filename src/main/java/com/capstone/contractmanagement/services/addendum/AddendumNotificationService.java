package com.capstone.contractmanagement.services.addendum;

import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.entities.addendum.Addendum;
import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.enums.AddendumStatus;
import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.repositories.IAddendumRepository;
import com.capstone.contractmanagement.repositories.IContractRepository;
import com.capstone.contractmanagement.services.notification.INotificationService;
import com.capstone.contractmanagement.services.sendmails.IMailService;
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
public class AddendumNotificationService implements IAddendumNotificationService {
    private final IAddendumRepository addendumRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final INotificationService notificationService;
    private final IContractRepository contractRepository;
    private final IMailService mailService;

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
            String message = "Hợp đồng số '" + addendum.getContractNumber() + "' đã được gia hạn thêm từ ngày '" + addendum.getEffectiveDate() + "' đến ngày '" + addendum.getExtendContractDate();
            sendNotification(addendum, message, true);
            mailService.sendEmailAddendumExtendedDate(addendum);
            addendum.setIsEffectiveNotified(true);
            addendumRepository.save(addendum);
            contract.setStatus(ContractStatus.ACTIVE);
            contract.setIsExpiryNotified(true);
            contract.setIsEffectiveOverdueNotified(true);
            contractRepository.save(contract);
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
            String message = "Hợp đồng số '" + addendum.getContractNumber() + "' đã hết hạn ngày '" + addendum.getContractExpirationDate() + "'";
            sendNotification(addendum, message, false);
            mailService.sendEmailAddendumEndExtendedDate(addendum);
            addendum.setIsExpiryNotified(true);
            addendumRepository.save(addendum);
            contract.setStatus(ContractStatus.EXPIRED);
            contract.setIsExpiryNotified(true);
            contract.setIsEffectiveOverdueNotified(true);
            contractRepository.save(contract);
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
}

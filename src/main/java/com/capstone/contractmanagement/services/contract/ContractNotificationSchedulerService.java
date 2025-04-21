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
    import java.time.format.DateTimeFormatter;
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
    //    private static final int EFFECTIVE_NOTIFY_DAYS = 5;
    //    private static final int EXPIRY_NOTIFY_DAYS = 5;

        @Override
        @Transactional
        @Scheduled(fixedDelay = 60000)
        //@Scheduled(cron = "0 0 8 * * ?") // Chạy hàng ngày lúc 8h sáng
        public void checkContractDates() {
            LocalDateTime now = LocalDateTime.now();

            // Bước 1: Cập nhật trạng thái hợp đồng nếu có phụ lục gia hạn được duyệt
            //updateContractStatusBasedOnAddendum();

            // Bước 2: Thông báo sắp hiệu lực
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
                contract.setStatus(ContractStatus.ACTIVE);
                contractRepository.save(contract);
            }

            // Bước 3: Thông báo sắp hết hạn
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

            // Bước 4: Thông báo quá hạn hiệu lực
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
                contract.setStatus(ContractStatus.EXPIRED);
                contractRepository.save(contract);
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
         * Gửi thông báo quá hạn
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

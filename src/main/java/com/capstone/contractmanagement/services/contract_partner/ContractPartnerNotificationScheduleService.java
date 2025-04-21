package com.capstone.contractmanagement.services.contract_partner;

import com.capstone.contractmanagement.entities.PartnerContract;
import com.capstone.contractmanagement.entities.PaymentSchedule;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.enums.PaymentStatus;
import com.capstone.contractmanagement.repositories.IPartnerContractRepository;
import com.capstone.contractmanagement.repositories.IPaymentScheduleRepository;
import com.capstone.contractmanagement.services.notification.INotificationService;
import com.capstone.contractmanagement.services.sendmails.IMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractPartnerNotificationScheduleService implements IContractPartnerNotificationScheduleService {
    private final IPaymentScheduleRepository paymentScheduleRepository;
    private final IPartnerContractRepository contractPartnerRepository;

    private final SimpMessagingTemplate messagingTemplate;
    private final INotificationService notificationService;
    private final IMailService mailService;

    // Số ngày trước khi hiệu lực hoặc hết hạn để gửi thông báo
    private static final int EFFECTIVE_NOTIFY_DAYS = 5;
    private static final int EXPIRY_NOTIFY_DAYS = 5;

    @Scheduled(cron = "0 0 8 * * ?") // Chạy định kỳ hàng ngày lúc 8 giờ sáng
    @Override
    public void checkContractPartnerDates() {
        LocalDateTime now = LocalDateTime.now();

        // Tìm đối tác hợp đồng sắp có hiệu lực
        List<PartnerContract> partnersToEffectiveNotify = contractPartnerRepository.findAll().stream()
                .filter(cp -> cp.getEffectiveDate() != null)
                .filter(cp -> Boolean.FALSE.equals(cp.getIsEffectiveNotified())) // Chưa gửi thông báo hiệu lực
                .filter(cp -> {
                    LocalDateTime notifyDate = cp.getEffectiveDate().minusDays(EFFECTIVE_NOTIFY_DAYS);
                    return now.isAfter(notifyDate) && now.isBefore(cp.getEffectiveDate());
                })
                .collect(Collectors.toList());

        for (PartnerContract cp : partnersToEffectiveNotify) {
            String message = "Hợp đồng đối tác '" + cp.getTitle() + "' sẽ có hiệu lực vào ngày " + cp.getEffectiveDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            sendNotification(cp, message, true);
            // send mail here
            mailService.sendEmailPartnerContractEffectiveReminder(cp);
        }

        // Tìm đối tác hợp đồng sắp hết hạn
        List<PartnerContract> partnersToExpiryNotify = contractPartnerRepository.findAll().stream()
                .filter(cp -> cp.getExpiryDate() != null)
                .filter(cp -> Boolean.FALSE.equals(cp.getIsExpiryNotified())) // Chưa gửi thông báo hết hạn
                .filter(cp -> {
                    LocalDateTime notifyDate = cp.getExpiryDate().minusDays(EXPIRY_NOTIFY_DAYS);
                    return now.isAfter(notifyDate) && now.isBefore(cp.getExpiryDate());
                })
                .collect(Collectors.toList());

        for (PartnerContract cp : partnersToExpiryNotify) {
            String message = "Hợp đồng đối tác '" + cp.getTitle() + "' sắp hết hạn vào ngày " + cp.getExpiryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            sendNotification(cp, message, false);
            // send mail here
            mailService.sendEmailPartnerContractExpiryReminder(cp);
        }
    }

    /**
     * Gửi thông báo đến người dùng và cập nhật trạng thái thông báo của ContractPartner.
     *
     * @param cp         đối tượng ContractPartner
     * @param message    nội dung thông báo
     * @param isEffective nếu true: thông báo hiệu lực, nếu false: thông báo hết hạn
     */
    private void sendNotification(PartnerContract cp, String message, boolean isEffective) {
        // Lấy thông tin người dùng từ ContractPartner
        User user = cp.getUser();

        // Tạo payload thông báo
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        payload.put("contractPartnerId", cp.getId());

        // Gửi thông báo qua WebSocket
        messagingTemplate.convertAndSendToUser(user.getFullName(), "/queue/notifications", payload);

        // Lưu thông báo vào hệ thống (Notification entity)
        notificationService.saveNotification(user, message, null);

        // Cập nhật trạng thái thông báo đã được gửi
        if (isEffective) {
            cp.setIsEffectiveNotified(true);
        } else {
            cp.setIsExpiryNotified(true);
        }

        // Lưu lại thay đổi trong cơ sở dữ liệu
        contractPartnerRepository.save(cp);
    }

    @Scheduled(cron = "0 0 8 * * ?")
    private void checkPaymentSchedule() {
        LocalDateTime now = LocalDateTime.now();
        // Chúng ta chỉ so sánh phần ngày để tránh sai lệch giờ
        LocalDate today = now.toLocalDate();
        LocalDate targetDate = today.plusDays(5);

        List<PaymentSchedule> reminderNotifiedPaymentSchedules =
                paymentScheduleRepository.findAll().stream()
                        // Chưa gửi reminder
                        .filter(ps -> !ps.isReminderEmailSent())
                        // Chỉ cho PartnerContract (bỏ qua những schedule gắn vào Contract chính)
                        .filter(ps -> ps.getContract() == null && ps.getPartnerContract() != null)
                        // Chưa thanh toán
                        .filter(ps -> ps.getStatus() == PaymentStatus.UNPAID)
                        // paymentDate cách ngày hôm nay đúng 5 ngày
                        .filter(ps -> {
                            LocalDate paymentDate = ps.getPaymentDate().toLocalDate();
                            return paymentDate.equals(targetDate);
                        })
                        .collect(Collectors.toList());

        for (PaymentSchedule ps : reminderNotifiedPaymentSchedules) {
            // Đánh dấu đã gửi
            ps.setReminderEmailSent(true);
            paymentScheduleRepository.save(ps);

            // Tạo message
            String title = ps.getPartnerContract().getTitle();
            LocalDateTime due = ps.getPaymentDate();
            String reminderMessage = String.format(
                    "Nhắc nhở: Hợp đồng đối tác '%s' sẽ đến hạn thanh toán vào ngày %s. Vui lòng chuẩn bị thanh toán.",
                    title,
                    due.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            );

            // Gửi notification real‐time qua WebSocket/STOMP
            String username = ps.getPartnerContract().getUser().getFullName();
            Map<String, Object> payload = Map.of("message", reminderMessage);
            messagingTemplate.convertAndSendToUser(username, "/queue/payment", payload);

            // Lưu notification vào DB
            notificationService.saveNotification(
                    ps.getPartnerContract().getUser(),
                    reminderMessage,
                    null
            );

            // (Tuỳ chọn) gửi email
            mailService.sendEmailPartnerContractPaymentReminder(ps);
        }

        List<PaymentSchedule> overDueNotifiedPaymentSchedules =
                paymentScheduleRepository.findAll().stream()
                        // Chưa gửi reminder
                        .filter(ps -> !ps.isOverdueEmailSent())
                        // Chỉ cho PartnerContract (bỏ qua những schedule gắn vào Contract chính)
                        .filter(ps -> ps.getContract() == null && ps.getPartnerContract() != null)
                        // Chưa thanh toán
                        .filter(ps -> ps.getStatus() == PaymentStatus.UNPAID)
                        // paymentDate cách ngày hôm nay đúng 5 ngày
                        .filter(ps -> {
                            LocalDate paymentDate = ps.getPaymentDate().toLocalDate();
                            return paymentDate.equals(targetDate);
                        })
                        .collect(Collectors.toList());
        for (PaymentSchedule ps : overDueNotifiedPaymentSchedules) {
            // Đánh dấu đã gửi
            ps.setOverdueEmailSent(true);
            ps.setStatus(PaymentStatus.OVERDUE);
            paymentScheduleRepository.save(ps);

            // Tạo message
            String title = ps.getPartnerContract().getTitle();
            LocalDateTime due = ps.getPaymentDate();
            String reminderMessage = String.format(
                    "Quá hạn: Hợp đồng đối tác '%s' đã quá hạn thanh toán vào ngày %s.",
                    title,
                    due.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            );

            // Gửi notification real‐time qua WebSocket/STOMP
            String username = ps.getPartnerContract().getUser().getFullName();
            Map<String, Object> payload = Map.of("message", reminderMessage);
            messagingTemplate.convertAndSendToUser(username, "/queue/payment", payload);

            // Lưu notification vào DB
            notificationService.saveNotification(
                    ps.getPartnerContract().getUser(),
                    reminderMessage,
                    null
            );

            // (Tuỳ chọn) gửi email
            mailService.sendEmailPartnerContractPaymentExpired(ps);
        }
    }
}

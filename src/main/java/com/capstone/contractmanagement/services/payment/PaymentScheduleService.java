package com.capstone.contractmanagement.services.payment;

import com.capstone.contractmanagement.dtos.DataMailDTO;
import com.capstone.contractmanagement.dtos.payment.CreatePaymentScheduleDTO;
import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.entities.PaymentSchedule;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.enums.PaymentStatus;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.IContractRepository;
import com.capstone.contractmanagement.repositories.IPaymentScheduleRepository;
import com.capstone.contractmanagement.services.notification.INotificationService;
import com.capstone.contractmanagement.services.sendmails.IMailService;
import com.capstone.contractmanagement.utils.MailTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentScheduleService implements IPaymentScheduleService {

    private final IPaymentScheduleRepository paymentScheduleRepository;
    private final IContractRepository contractRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final IMailService mailService;
    private final INotificationService notificationService;

    @Override
    public String createPaymentSchedule(Long contractId, CreatePaymentScheduleDTO createPaymentScheduleDTO) throws DataNotFoundException {
        // check if contract exists
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new DataNotFoundException("Contract not found with id: " + contractId));
        // create payment schedule

        PaymentSchedule paymentSchedule = PaymentSchedule.builder()
                .paymentOrder(createPaymentScheduleDTO.getPaymentOrder())
                .amount(createPaymentScheduleDTO.getAmount())
                .paymentDate(createPaymentScheduleDTO.getPaymentDate())
                .status(PaymentStatus.UNPAID)
                .overdueEmailSent(false)
                .reminderEmailSent(false)
                .paymentMethod(createPaymentScheduleDTO.getPaymentMethod())
                .notifyPaymentContent(createPaymentScheduleDTO.getNotifyPaymentContent())
                .contract(contract)
                .build();

        paymentScheduleRepository.save(paymentSchedule);

        return "Create payment schedule successfully";
    }

    @Override
    @Scheduled(fixedRate = 60000)
    public void checkPaymentDue() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Gửi thông báo nhắc nhở 5 phút trước thời hạn
        LocalDateTime reminderWindowEnd = now.plusMinutes(5);
        List<PaymentSchedule> reminderPayments = paymentScheduleRepository.findByPaymentDateBetweenAndStatus(now, reminderWindowEnd, PaymentStatus.UNPAID);
        for (PaymentSchedule payment : reminderPayments) {
            if (!payment.isReminderEmailSent()) {
                // Tạo payload dạng JSON
                Map<String, Object> payload = new HashMap<>();
                String reminderMessage = "Nhắc nhở: Hợp đồng '" + payment.getContract().getTitle() +
                        "' sẽ đến hạn thanh toán lúc " + payment.getPaymentDate() +
                        ". Vui lòng chuẩn bị thanh toán.";
                Long contractId = payment.getContract().getId();
                payload.put("message", reminderMessage);
                payload.put("contractId", contractId);
                // Bạn có thể thêm các trường khác nếu cần, ví dụ "url"
                //payload.put("url", ""); // Hoặc bỏ qua nếu không cần

                // Lấy username của người dùng
                String username = payment.getContract().getUser().getFullName();
                User user = payment.getContract().getUser();
                // Gửi thông báo dưới dạng JSON
                messagingTemplate.convertAndSendToUser(username, "/queue/payment", payload);
                notificationService.saveNotification(user, reminderMessage, payment.getContract());
                sendEmailReminder(payment);
                // Đánh dấu đã gửi email nhắc nhở
                payment.setReminderEmailSent(true);
                paymentScheduleRepository.save(payment);
            }
        }

        // 2. Gửi thông báo quá hạn nếu đã vượt qua dueDate
        List<PaymentSchedule> overduePayments = paymentScheduleRepository.findByPaymentDateBeforeAndStatus(now, PaymentStatus.UNPAID);
        for (PaymentSchedule payment : overduePayments) {
            if (now.isAfter(payment.getPaymentDate()) && !payment.isOverdueEmailSent()) {
                payment.setStatus(PaymentStatus.OVERDUE);
                paymentScheduleRepository.save(payment);
                // Tạo payload dạng JSON cho thông báo quá hạn
                Map<String, Object> payload = new HashMap<>();
                String overdueMessage = "Quá hạn: Hợp đồng '" + payment.getContract().getTitle() +
                        "' đã quá hạn thanh toán lúc " + payment.getPaymentDate() + ".";
                Long contractId = payment.getContract().getId();
                payload.put("message", overdueMessage);
                payload.put("contractId", contractId);
                //payload.put("url", ""); // Nếu có URL cụ thể thì thêm vào

                String username = payment.getContract().getUser().getFullName();
                User user = payment.getContract().getUser();
                messagingTemplate.convertAndSendToUser(username, "/queue/payment", payload);
                notificationService.saveNotification(user, overdueMessage, payment.getContract());
                sendEmailExpired(payment);
                payment.setOverdueEmailSent(true);
                paymentScheduleRepository.save(payment);
            }
        }
    }

    private void sendEmailReminder(PaymentSchedule payment) {
        // Gửi email nhắc nhỏ
        try {
            DataMailDTO dataMailDTO = new DataMailDTO();
            dataMailDTO.setTo(payment.getContract().getParty().getEmail());
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.CONTRACT_PAYMENT_NOTIFICATION);

            Map<String, Object> props = new HashMap<>();
            props.put("contractTitle", payment.getContract().getTitle());
            props.put("dueDate", payment.getPaymentDate());
            dataMailDTO.setProps(props);
            mailService.sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.CONTRACT_PAYMENT_NOTIFICATION);
        } catch (Exception e) {
            // Xu ly loi
            e.printStackTrace();
        }
    }

    // gửi mail đã hết hạn thanh toán
    private void sendEmailExpired(PaymentSchedule payment) {
        // Gửi email nhắc nhỏ
        try {
            DataMailDTO dataMailDTO = new DataMailDTO();
            dataMailDTO.setTo(payment.getContract().getParty().getEmail());
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.CONTRACT_PAYMENT_EXPIRED);

            Map<String, Object> props = new HashMap<>();
            props.put("contractTitle", payment.getContract().getTitle());
            props.put("dueDate", payment.getPaymentDate());
            dataMailDTO.setProps(props);
            mailService.sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.CONTRACT_PAYMENT_EXPIRED);
        } catch (Exception e) {
            // Xu ly loi
            e.printStackTrace();
        }
    }

//    @Scheduled(fixedRate = 60000)
//    public void checkPaymentDue() {
//        LocalDateTime now = LocalDateTime.now();
//        // Số ngày trước hạn cần nhắc nhở, ví dụ 3 ngày
//        int reminderDays = 3;
//        LocalDateTime reminderThreshold = now.plusDays(reminderDays);
//
//        // Tìm các khoản thanh toán sắp đến hạn (trong khoảng từ bây giờ đến 3 ngày sau)
//        List<PaymentSchedule> upcomingPayments = paymentScheduleRepository
//                .findByDueDateBetweenAndStatus(now, reminderThreshold, "Chưa thanh toán");
//        for (PaymentSchedule payment : upcomingPayments) {
//            String message = "Hợp đồng '" + payment.getContract().getTitle() + "' sẽ đến hạn thanh toán vào: "
//                    + payment.getDueDate() + ". Vui lòng thanh toán sớm.";
//            messagingTemplate.convertAndSend("/topic/payment", message);
//        }
//
//        // Kiểm tra các khoản thanh toán đã quá hạn để gửi email nhắc nhở
//        List<PaymentSchedule> overduePayments = paymentScheduleRepository
//                .findByDueDateBeforeAndStatus(now, "Chưa thanh toán");
//        for (PaymentSchedule payment : overduePayments) {
//            if (now.isAfter(payment.getDueDate())) {
//                sendEmailReminder(payment);
//            }
//        }
//    }
}
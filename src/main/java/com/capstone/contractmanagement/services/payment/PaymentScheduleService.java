package com.capstone.contractmanagement.services.payment;

import com.capstone.contractmanagement.dtos.DataMailDTO;
import com.capstone.contractmanagement.dtos.payment.CreatePaymentScheduleDTO;
import com.capstone.contractmanagement.entities.addendum.AddendumPaymentSchedule;
import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.entities.PaymentSchedule;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.enums.AddendumStatus;
import com.capstone.contractmanagement.enums.PaymentStatus;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.IContractRepository;
import com.capstone.contractmanagement.repositories.IPaymentScheduleRepository;
import com.capstone.contractmanagement.services.app_config.IAppConfigService;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentScheduleService implements IPaymentScheduleService {

    private final IPaymentScheduleRepository paymentScheduleRepository;
    private final IContractRepository contractRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final IMailService mailService;
    private final INotificationService notificationService;
    private final IAppConfigService appConfigService;

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
    @Scheduled(cron = "0 0 8 * * ?")
    public void checkPaymentDue() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Thông báo nhắc thanh toán dựa vào notifyPaymentDate
        List<PaymentSchedule> reminderPayments = paymentScheduleRepository.findByStatus(PaymentStatus.UNPAID);
        for (PaymentSchedule payment : reminderPayments) {
            Contract contract = payment.getContract();

            // Lấy các đợt thanh toán từ phụ lục (nếu có)
            List<AddendumPaymentSchedule> addendumPayments = getAddendumPaymentSchedules(contract);

            // Nếu có phụ lục thanh toán, kiểm tra các đợt thanh toán từ phụ lục
            if (!addendumPayments.isEmpty()) {
                for (AddendumPaymentSchedule addendumPayment : addendumPayments) {
                    if (addendumPayment.getNotifyPaymentDate() != null &&
                            !addendumPayment.isReminderEmailSent() &&
                            !now.isBefore(addendumPayment.getNotifyPaymentDate())) {

                        String message = addendumPayment.getNotifyPaymentContent() != null
                                ? addendumPayment.getNotifyPaymentContent()
                                : "Nhắc nhở: Hợp đồng '" + contract.getTitle() +
                                "' sẽ đến hạn thanh toán vào " + addendumPayment.getPaymentDate();

                        sendPaymentNotification(contract, message);
                        mailService.sendEmailReminder(null, addendumPayment); // Gửi email nhắc nhở cho phụ lục
                        addendumPayment.setReminderEmailSent(true);
                        paymentScheduleRepository.save(payment);
                    }
                }
            } else {
                // Không có phụ lục, xử lý thanh toán bình thường từ PaymentSchedule
                if (contract != null && Boolean.TRUE.equals(contract.getIsLatestVersion())) {
                    if (payment.getNotifyPaymentDate() != null &&
                            !payment.isReminderEmailSent() &&
                            !now.isBefore(payment.getNotifyPaymentDate())) {

                        String message = payment.getNotifyPaymentContent() != null
                                ? payment.getNotifyPaymentContent()
                                : "Nhắc nhở: Hợp đồng '" + contract.getTitle() +
                                "' sẽ đến hạn thanh toán vào " + payment.getPaymentDate();

                        sendPaymentNotification(contract, message);
                        mailService.sendEmailReminder(payment, null);
                        payment.setReminderEmailSent(true);
                        paymentScheduleRepository.save(payment);
                    }
                }
            }
        }

        // 2. Thông báo quá hạn nếu vượt quá paymentDate
        List<PaymentSchedule> overduePayments = paymentScheduleRepository.findByStatus(PaymentStatus.UNPAID);
        for (PaymentSchedule payment : overduePayments) {
            Contract contract = payment.getContract();

            // Lấy các đợt thanh toán từ phụ lục (nếu có)
            List<AddendumPaymentSchedule> addendumPayments = getAddendumPaymentSchedules(contract);

            // Nếu có phụ lục thanh toán, kiểm tra các đợt thanh toán từ phụ lục
            if (!addendumPayments.isEmpty()) {
                for (AddendumPaymentSchedule addendumPayment : addendumPayments) {
                    if (addendumPayment.getPaymentDate() != null &&
                            now.isAfter(addendumPayment.getPaymentDate()) &&
                            !addendumPayment.isOverdueEmailSent()) {

                        String overdueMessage = "Quá hạn: Hợp đồng '" + contract.getTitle() +
                                "' đã quá hạn thanh toán vào ngày " + addendumPayment.getPaymentDate();

                        addendumPayment.setStatus(PaymentStatus.OVERDUE);
                        addendumPayment.setOverdueEmailSent(true);
                        paymentScheduleRepository.save(payment);

                        sendPaymentNotification(contract, overdueMessage);
                        mailService.sendEmailExpired(null, addendumPayment); // Gửi email quá hạn cho phụ lục
                    }
                }
            } else {
                // Không có phụ lục, xử lý thanh toán bình thường từ PaymentSchedule
                if (contract != null && Boolean.TRUE.equals(contract.getIsLatestVersion())) {
                    if (payment.getPaymentDate() != null &&
                            now.isAfter(payment.getPaymentDate()) &&
                            !payment.isOverdueEmailSent()) {

                        String overdueMessage = "Quá hạn: Hợp đồng '" + contract.getTitle() +
                                "' đã quá hạn thanh toán vào ngày " + payment.getPaymentDate();

                        payment.setStatus(PaymentStatus.OVERDUE);
                        payment.setOverdueEmailSent(true);
                        paymentScheduleRepository.save(payment);

                        sendPaymentNotification(contract, overdueMessage);
                        mailService.sendEmailExpired(payment, null);
                    }
                }
            }
        }
    }

    private List<AddendumPaymentSchedule> getAddendumPaymentSchedules(Contract contract) {
        // Lấy tất cả các đợt thanh toán từ phụ lục đã duyệt
        return contract.getAddenda().stream()
                .filter(addendum -> addendum.getStatus() == AddendumStatus.APPROVED)
                .flatMap(addendum -> addendum.getPaymentSchedules().stream()) // Lấy các đợt thanh toán từ phụ lục
                .collect(Collectors.toList());
    }

    private void sendPaymentNotification(Contract contract, String message) {
        User user = contract.getUser();
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        payload.put("contractId", contract.getId());

        messagingTemplate.convertAndSendToUser(user.getFullName(), "/queue/payment", payload);
        notificationService.saveNotification(user, message, contract);
    }

    @Override
    public List<String> getBillUrlsByPaymentId(Long paymentId) throws DataNotFoundException {
        // Lấy danh sách billUrls từ repository
        List<String> billUrls = paymentScheduleRepository.findBillUrlsByPaymentId(paymentId);

        if (billUrls == null || billUrls.isEmpty()) {
            throw new DataNotFoundException("No bill URLs found for payment with ID: " + paymentId);
        }

        return billUrls;
    }

//    private void sendEmailReminder(PaymentSchedule payment) {
//        // Gửi email nhắc nhỏ
//        try {
//            DataMailDTO dataMailDTO = new DataMailDTO();
//            dataMailDTO.setTo(payment.getContract().getPartner().getEmail());
//            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.CONTRACT_PAYMENT_NOTIFICATION);
//
//            Map<String, Object> props = new HashMap<>();
//            props.put("contractTitle", payment.getContract().getTitle());
//            props.put("dueDate", payment.getPaymentDate());
//            dataMailDTO.setProps(props);
//            mailService.sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.CONTRACT_PAYMENT_NOTIFICATION);
//        } catch (Exception e) {
//            // Xu ly loi
//            e.printStackTrace();
//        }
//    }

    // gửi mail đã hết hạn thanh toán
//    private void sendEmailExpired(PaymentSchedule payment) {
//        // Gửi email nhắc nhỏ
//        try {
//            DataMailDTO dataMailDTO = new DataMailDTO();
//            dataMailDTO.setTo(payment.getContract().getPartner().getEmail());
//            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.CONTRACT_PAYMENT_EXPIRED);
//
//            Map<String, Object> props = new HashMap<>();
//            props.put("contractTitle", payment.getContract().getTitle());
//            props.put("dueDate", payment.getPaymentDate());
//            dataMailDTO.setProps(props);
//            mailService.sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.CONTRACT_PAYMENT_EXPIRED);
//        } catch (Exception e) {
//            // Xu ly loi
//            e.printStackTrace();
//        }
//    }

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
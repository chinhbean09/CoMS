package com.capstone.contractmanagement.services.payment;

import com.capstone.contractmanagement.dtos.payment.CreatePaymentScheduleDTO;
import com.capstone.contractmanagement.entities.addendum.AddendumPaymentSchedule;
import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.entities.PaymentSchedule;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.enums.AddendumStatus;
import com.capstone.contractmanagement.enums.PaymentStatus;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.IAddendumPaymentScheduleRepository;
import com.capstone.contractmanagement.repositories.IContractRepository;
import com.capstone.contractmanagement.repositories.IPaymentScheduleRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final IAddendumPaymentScheduleRepository addendumPaymentScheduleRepository;

    @Override
    public String createPaymentSchedule(Long contractId, CreatePaymentScheduleDTO createPaymentScheduleDTO) throws DataNotFoundException {
        // check if contract exists
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy hợp đồng"));
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

        return "Tạo đợt thanh toán thành công";
    }

    @Override
    //@Scheduled(cron = "0 0 8 * * ?")
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void checkPaymentDue() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Thông báo nhắc thanh toán dựa vào notifyPaymentDate
        List<PaymentSchedule> reminderPayments = paymentScheduleRepository
                .findByStatus(PaymentStatus.UNPAID)
                .stream()
                // chỉ lấy những paymentSchedules liên kết với Contract
                .filter(ps -> ps.getContract() != null)
                // bỏ qua những record gắn partnerContract
                .filter(ps -> ps.getPartnerContract() == null)
                .collect(Collectors.toList());

        for (PaymentSchedule payment : reminderPayments) {
            Contract contract = payment.getContract();

            // Lấy các đợt thanh toán từ phụ lục (nếu có)
            List<AddendumPaymentSchedule> addendumPayments = getAddendumPaymentSchedules(contract);

            // Nếu có phụ lục thanh toán, kiểm tra các đợt thanh toán từ phụ lục
            if (!addendumPayments.isEmpty()) {
                for (AddendumPaymentSchedule addendumPayment : addendumPayments) {
                    LocalDateTime fiveMinutesBefore = addendumPayment.getPaymentDate().minusMinutes(5);
                    if (//addendumPayment.getNotifyPaymentDate() != null &&
                            now.isAfter(fiveMinutesBefore) &&
                            !addendumPayment.isReminderEmailSent() &&
                            now.isBefore(addendumPayment.getPaymentDate())) {

                        String message = "Nhắc nhở: Hợp đồng '" + contract.getTitle() +
                                "' sẽ đến hạn thanh toán đợt " + addendumPayment.getPaymentOrder() +
                                " vào ngày " + addendumPayment.getPaymentDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

                        sendPaymentNotification(contract, message);
                        mailService.sendEmailPaymentReminder(null, addendumPayment); // Gửi email nhắc nhở cho phụ lục
                        addendumPayment.setReminderEmailSent(true);
                        addendumPaymentScheduleRepository.save(addendumPayment);
                    }
                }
            } else {
                // Không có phụ lục, xử lý thanh toán bình thường từ PaymentSchedule
                if (contract != null && Boolean.TRUE.equals(contract.getIsLatestVersion())) {
                    if (payment.getNotifyPaymentDate() != null &&
                            !payment.isReminderEmailSent() &&
                            !now.isBefore(payment.getNotifyPaymentDate())) {

                        String message = "Nhắc nhở: Hợp đồng '" + contract.getTitle() +
                                "' sẽ đến hạn thanh toán đợt " + payment.getPaymentOrder() +
                                " vào ngày " + payment.getPaymentDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

                        sendPaymentNotification(contract, message);
                        mailService.sendEmailPaymentReminder(payment, null);
                        payment.setReminderEmailSent(true);
                        paymentScheduleRepository.save(payment);
                    }
                }
            }
        }

        // 2. Thông báo quá hạn nếu vượt quá paymentDate
        List<PaymentSchedule> overduePayments = paymentScheduleRepository
                .findByStatus(PaymentStatus.UNPAID)
                .stream()
                .filter(ps -> ps.getContract() != null)
                .filter(ps -> ps.getPartnerContract() == null)
                .collect(Collectors.toList());
        for (PaymentSchedule payment : overduePayments) {
            Contract contract = payment.getContract();

            // Lấy các đợt thanh toán từ phụ lục (nếu có)
            List<AddendumPaymentSchedule> addendumPayments = getAddendumPaymentSchedules(contract);

            // Nếu có phụ lục thanh toán, kiểm tra các đợt thanh toán từ phụ lục
            if (!addendumPayments.isEmpty()) {
                for (AddendumPaymentSchedule addendumPayment : addendumPayments) {
                    if (addendumPayment.getPaymentDate() != null &&
                            !now.isBefore(addendumPayment.getPaymentDate()) &&
                            !addendumPayment.isOverdueEmailSent()) {

                        String overdueMessage = "Quá hạn: Hợp đồng '" + contract.getTitle() +
                                "' đã quá hạn thanh toán đợt " + addendumPayment.getPaymentOrder() +
                                " vào ngày " + addendumPayment.getPaymentDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

                        addendumPayment.setStatus(PaymentStatus.OVERDUE);
                        addendumPayment.setOverdueEmailSent(true);
                        addendumPaymentScheduleRepository.save(addendumPayment);

                        sendPaymentNotification(contract, overdueMessage);
                        mailService.sendEmailPaymentExpired(null, addendumPayment); // Gửi email quá hạn cho phụ lục
                    }
                }
            } else {
                // Không có phụ lục, xử lý thanh toán bình thường từ PaymentSchedule
                if (contract != null && Boolean.TRUE.equals(contract.getIsLatestVersion())) {
                    if (payment.getPaymentDate() != null &&
                            !now.isBefore(payment.getPaymentDate()) &&
                            !payment.isOverdueEmailSent()) {

                        String overdueMessage = "Quá hạn: Hợp đồng '" + contract.getTitle() +
                                "' đã quá hạn thanh toán đợt " + payment.getPaymentOrder() +
                                " vào ngày " + payment.getPaymentDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

                        payment.setStatus(PaymentStatus.OVERDUE);
                        payment.setOverdueEmailSent(true);
                        paymentScheduleRepository.save(payment);

                        sendPaymentNotification(contract, overdueMessage);
                        mailService.sendEmailPaymentExpired(payment, null);
                    }
                }
            }
        }
    }

    private List<AddendumPaymentSchedule> getAddendumPaymentSchedules(Contract contract) {
        // Lấy tất cả các đợt thanh toán từ phụ lục đã duyệt
        return contract.getAddenda().stream()
                .filter(addendum -> addendum.getStatus() == AddendumStatus.APPROVED || addendum.getStatus() == AddendumStatus.SIGNED)
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
            throw new DataNotFoundException("Không tìm thấy bill url với đợt thanh toán");
        }

        return billUrls;
    }
}
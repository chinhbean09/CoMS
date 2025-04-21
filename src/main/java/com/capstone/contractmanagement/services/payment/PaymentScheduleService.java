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
        @Transactional
        @Scheduled(fixedDelay = 60000)
        public void checkPaymentDue() {
            LocalDateTime now = LocalDateTime.now();

            // Lấy tất cả các hợp đồng có phiên bản mới nhất
            List<Contract> contracts = contractRepository.findAll().stream()
                    .filter(Contract::getIsLatestVersion)
                    .collect(Collectors.toList());

            for (Contract contract : contracts) {
                // Kiểm tra xem hợp đồng có phụ lục được duyệt không
                boolean hasApprovedAddendum = contract.getAddenda().stream()
                        .anyMatch(addendum -> addendum.getStatus() == AddendumStatus.APPROVED || addendum.getStatus() == AddendumStatus.SIGNED);

                if (hasApprovedAddendum) {
                    // Nếu có phụ lục được duyệt, chỉ xử lý các đợt thanh toán từ phụ lục
                    List<AddendumPaymentSchedule> addendumPayments = getAddendumPaymentSchedules(contract);
                    processPaymentSchedules(addendumPayments, contract, now);
                } else {
                    // Nếu không có phụ lục được duyệt, xử lý các đợt thanh toán của hợp đồng chính
                    List<PaymentSchedule> paymentSchedules = paymentScheduleRepository.findByContractAndStatus(contract, PaymentStatus.UNPAID);
                    processPaymentSchedules(paymentSchedules, contract, now);
                }
            }
        }

        // Phương thức xử lý chung cho cả PaymentSchedule và AddendumPaymentSchedule
        private void processPaymentSchedules(List<?> paymentSchedules, Contract contract, LocalDateTime now) {
            for (Object payment : paymentSchedules) {
                if (payment instanceof AddendumPaymentSchedule) {
                    AddendumPaymentSchedule aps = (AddendumPaymentSchedule) payment;
                    handleAddendumPaymentNotification(aps, contract, now);
                } else if (payment instanceof PaymentSchedule) {
                    PaymentSchedule ps = (PaymentSchedule) payment;
                    handlePaymentNotification(ps, contract, now);
                }
            }
        }

        // Phương thức xử lý thông báo cho PaymentSchedule
        private void handlePaymentNotification(PaymentSchedule ps, Contract contract, LocalDateTime now) {
            if (ps.getNotifyPaymentDate() != null && !ps.isReminderEmailSent() && now.isAfter(ps.getNotifyPaymentDate())) {
                sendReminderNotification(ps, contract);
                ps.setReminderEmailSent(true);
                paymentScheduleRepository.save(ps);
            }
            if (ps.getPaymentDate() != null && now.isAfter(ps.getPaymentDate()) && !ps.isOverdueEmailSent()) {
                sendOverdueNotification(ps, contract);
                ps.setOverdueEmailSent(true);
                ps.setStatus(PaymentStatus.OVERDUE);
                paymentScheduleRepository.save(ps);
            }
        }

        // Phương thức xử lý thông báo cho AddendumPaymentSchedule
        private void handleAddendumPaymentNotification(AddendumPaymentSchedule aps, Contract contract, LocalDateTime now) {
            if (aps.getNotifyPaymentDate() != null && !aps.isReminderEmailSent() && now.isAfter(aps.getNotifyPaymentDate())) {
                sendReminderNotification(aps, contract);
                aps.setReminderEmailSent(true);
                addendumPaymentScheduleRepository.save(aps);
            }
            if (aps.getPaymentDate() != null && now.isAfter(aps.getPaymentDate()) && !aps.isOverdueEmailSent()) {
                sendOverdueNotification(aps, contract);
                aps.setOverdueEmailSent(true);
                aps.setStatus(PaymentStatus.OVERDUE);
                addendumPaymentScheduleRepository.save(aps);
            }
        }

        // Các phương thức gửi thông báo
        private void sendReminderNotification(Object payment, Contract contract) {
            String message = "Nhắc nhở thanh toán cho hợp đồng " + contract.getTitle();
            // Gửi thông báo qua WebSocket, email, v.v.
        }

        private void sendOverdueNotification(Object payment, Contract contract) {
            String message = "Thanh toán quá hạn cho hợp đồng " + contract.getTitle();
            // Gửi thông báo qua WebSocket, email, v.v.
        }

        // Giả định phương thức lấy danh sách đợt thanh toán từ phụ lục
        private List<AddendumPaymentSchedule> getAddendumPaymentSchedules(Contract contract) {
            return addendumPaymentScheduleRepository.findByContractAndStatus(contract, PaymentStatus.UNPAID);
        }

    //        private List<AddendumPaymentSchedule> getAddendumPaymentSchedules(Contract contract) {
    //            // Lấy tất cả các đợt thanh toán từ phụ lục đã duyệt
    //            return contract.getAddenda().stream()
    //                    .filter(addendum -> addendum.getStatus() == AddendumStatus.APPROVED || addendum.getStatus() == AddendumStatus.SIGNED)
    //                    .flatMap(addendum -> addendum.getPaymentSchedules().stream()) // Lấy các đợt thanh toán từ phụ lục
    //                    .collect(Collectors.toList());
    //        }

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
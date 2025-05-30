package com.capstone.contractmanagement.services.sendmails;

import com.capstone.contractmanagement.dtos.DataMailDTO;
import com.capstone.contractmanagement.entities.PartnerContract;
import com.capstone.contractmanagement.entities.Role;
import com.capstone.contractmanagement.entities.addendum.Addendum;
import com.capstone.contractmanagement.entities.PaymentSchedule;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.entities.addendum.AddendumPaymentSchedule;
import com.capstone.contractmanagement.entities.approval_workflow.ApprovalStage;
import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.repositories.IUserRepository;
import com.capstone.contractmanagement.utils.MailTemplate;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class MailService implements IMailService{
    private final JavaMailSender mailSender;

    private final IUserRepository userRepository;
    private final SpringTemplateEngine templateEngine;
    @Override
    public void sendHtmlMail(DataMailDTO dataMail, String templateName) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(message, true, "utf-8");

        Context context = new Context();
        context.setVariables(dataMail.getProps());

        String html = templateEngine.process(templateName, context);

        helper.setTo(dataMail.getTo());
        helper.setSubject(dataMail.getSubject());
        helper.setText(html, true);

        mailSender.send(message);
    }

    @Override
    @Async("taskExecutor")
    public void sendEmailReminder(Contract contract, User user, ApprovalStage stage) {
        try {
            DataMailDTO dataMailDTO = new DataMailDTO();
            dataMailDTO.setTo(new String[]{user.getEmail()});
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.CONTRACT_APPROVAL_NOTIFICATION);

            // Thiết lập các thuộc tính cho email
            Map<String, Object> props = new HashMap<>();
            props.put("contractTitle", contract.getTitle());
            props.put("stage", stage.getStageOrder());
            dataMailDTO.setProps(props);

            // Gửi email HTML theo template đã định nghĩa
            sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.CONTRACT_APPROVAL_NOTIFICATION);

            // Log thông báo gửi email thành công
            System.out.println("Đã gửi email nhắc nhở cho: " + user.getEmail());
        } catch (Exception e) {
            // Xử lý lỗi, có thể dùng framework logging như Log4j hoặc SLF4J thay vì printStackTrace
            e.printStackTrace();
        }
    }

    @Override
    @Async("taskExecutor")
    public void sendUpdateContractReminder(Contract contract, User user) {
        try {
            DataMailDTO dataMailDTO = new DataMailDTO();
            dataMailDTO.setTo(new String[]{user.getEmail()});
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.UPDATE_CONTRACT_REQUEST);
            Map<String, Object> props = new HashMap<>();
            props.put("contractNumber", contract.getContractNumber());
            dataMailDTO.setProps(props); // Set props to dataMailDTO
            sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.UPDATE_CONTRACT_REQUEST);
        } catch (Exception e) {
            // Xu ly loi
            e.printStackTrace();
        }
    }

    @Override
    @Async("taskExecutor")
    public void sendAccountPassword(String email, String password) {
        try {
            DataMailDTO dataMailDTO = new DataMailDTO();
            dataMailDTO.setTo(new String[]{email}); // Set email to dataMailDTO (email);
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.USER_REGISTER);

            Map<String, Object> props = new HashMap<>();
            props.put("password", password);
            props.put("email", email);
            dataMailDTO.setProps(props); // Set props to dataMailDTO

            sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.USER_REGISTER);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    @Override
    @Async("taskExecutor")
    @Transactional
    public void sendEmailPaymentReminder(PaymentSchedule payment) {
        // Gửi email nhắc nhỏ
        try {
            DataMailDTO dataMailDTO = new DataMailDTO();
            dataMailDTO.setTo(new String[]{payment.getContract().getPartner().getEmail()});
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.CONTRACT_PAYMENT_NOTIFICATION);

            Map<String, Object> props = new HashMap<>();
            if (payment != null){
                props.put("contractTitle", payment.getContract().getTitle());
                props.put("dueDate", payment.getPaymentDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                props.put("stage", payment.getPaymentOrder());
                props.put("amount", payment.getAmount());
            }

            dataMailDTO.setProps(props);
            sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.CONTRACT_PAYMENT_NOTIFICATION);
        } catch (Exception e) {
            // Xu ly loi
            e.printStackTrace();
        }
    }

    @Override
    public void sendEmailPaymentReminderForAddendum(AddendumPaymentSchedule addendumPaymentSchedule) {
        try {
            DataMailDTO dataMailDTO = new DataMailDTO();
            dataMailDTO.setTo(new String[] {addendumPaymentSchedule.getAddendum().getContract().getPartner().getEmail()});
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.CONTRACT_PAYMENT_NOTIFICATION);

            Map<String, Object> props = new HashMap<>();
            if (addendumPaymentSchedule != null){
                props.put("contractTitle", addendumPaymentSchedule.getAddendum().getContract().getTitle());
                props.put("dueDate", addendumPaymentSchedule.getPaymentDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                props.put("stage", addendumPaymentSchedule.getPaymentOrder());
                props.put("amount", addendumPaymentSchedule.getAmount());
            }

            dataMailDTO.setProps(props);
            sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.CONTRACT_PAYMENT_NOTIFICATION);
        } catch (Exception e) {
            // Xu ly loi
            e.printStackTrace();
        }
    }

    @Override
    @Async("taskExecutor")
    public void sendEmailPaymentExpired(PaymentSchedule payment) {
        try {
            User director = userRepository.findAll().stream()
                    .filter(user1 -> user1.getRole() != null && Role.DIRECTOR.equalsIgnoreCase(user1.getRole().getRoleName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người duyệt có vai trò giám đốc"));
            DataMailDTO dataMailDTO = new DataMailDTO();
            assert director != null;
            dataMailDTO.setTo(new String[]{payment.getContract().getPartner().getEmail(), director.getEmail()});
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.CONTRACT_PAYMENT_EXPIRED);

            Map<String, Object> props = new HashMap<>();

            if (payment != null){
                props.put("contractTitle", payment.getContract().getTitle());
                props.put("dueDate", payment.getPaymentDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                props.put("stage", payment.getPaymentOrder());
                props.put("amount", payment.getAmount());
            }

            dataMailDTO.setProps(props);
            sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.CONTRACT_PAYMENT_EXPIRED);
        } catch (Exception e) {
            // Xu ly loi
            e.printStackTrace();
        }
    }

    @Override
    public void sendEmailPaymentExpiredForAddendum(AddendumPaymentSchedule addendumPayment) {
        try {
            User director = userRepository.findAll().stream()
                    .filter(user1 -> user1.getRole() != null && Role.DIRECTOR.equalsIgnoreCase(user1.getRole().getRoleName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người duyệt có vai trò giám đốc"));
            DataMailDTO dataMailDTO = new DataMailDTO();
            dataMailDTO.setTo(new String[]{addendumPayment.getAddendum().getContract().getPartner().getEmail(), director.getEmail()});
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.CONTRACT_PAYMENT_EXPIRED);

            Map<String, Object> props = new HashMap<>();

            if (addendumPayment != null){
                props.put("contractTitle", addendumPayment.getAddendum().getContract().getTitle());
                props.put("dueDate", addendumPayment.getPaymentDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                props.put("stage", addendumPayment.getPaymentOrder());
            }

            dataMailDTO.setProps(props);
            sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.CONTRACT_PAYMENT_EXPIRED);
        } catch (Exception e) {
            // Xu ly loi
            e.printStackTrace();
        }
    }

    @Override
    @Async("taskExecutor")
    public void sendUpdateAddendumReminder(Addendum addendum, User user) {
        try {
            DataMailDTO dataMailDTO = new DataMailDTO();
            dataMailDTO.setTo(new String[]{user.getEmail()});
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.UPDATE_ADDENDUM_REQUEST);
            Map<String, Object> props = new HashMap<>();
            props.put("contractNumber", addendum.getContractNumber());
            dataMailDTO.setProps(props); // Set props to dataMailDTO
            sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.UPDATE_ADDENDUM_REQUEST);
        } catch (Exception e) {
            // Xu ly loi
            e.printStackTrace();
        }
    }

    @Override
    @Async("taskExecutor")
    public void sendEmailAddendumReminder(Addendum addendum, User user, ApprovalStage approvalStage) {
        try {
            DataMailDTO dataMailDTO = new DataMailDTO();
            dataMailDTO.setTo(new String[]{user.getEmail()}); // Gửi email cho người dùng user.getEmail());
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.ADDENDUM_APPROVAL_NOTIFICATION);

            // Thiết lập các thuộc tính cho email
            Map<String, Object> props = new HashMap<>();
            props.put("contractNumber", addendum.getContractNumber());
            props.put("stage", approvalStage.getStageOrder());
            dataMailDTO.setProps(props);

            // Gửi email HTML theo template đã định nghĩa
            sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.ADDENDUM_APPROVAL_NOTIFICATION);

            // Log thông báo gửi email thành công
            System.out.println("Đã gửi email nhắc nhở cho: " + user.getEmail());
        } catch (Exception e) {
            // Xử lý lỗi, có thể dùng framework logging như Log4j hoặc SLF4J thay vì printStackTrace
            e.printStackTrace();
        }
    }

    @Override
    @Async("taskExecutor")
    public void sendEmailApprovalSuccessForContract(Contract contract, User user) {
        try {
            DataMailDTO dataMailDTO = new DataMailDTO();
            dataMailDTO.setTo(new String[]{user.getEmail()});
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.APPROVAL_CONTRACT_SUCCESS);

            // Thiết lập các thuộc tính cho email
            Map<String, Object> props = new HashMap<>();
            props.put("contractNumber", contract.getContractNumber());
            props.put("contractTitle", contract.getTitle());
            dataMailDTO.setProps(props);

            // Gửi email HTML theo template đã định nghĩa
            sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.APPROVAL_CONTRACT_SUCCESS);

            // Log thông báo gửi email thành công
            System.out.println("Đã gửi email nhắc nhở cho: " + user.getEmail());
        } catch (Exception e) {
            // Xử lý lỗi, có thể dùng framework logging như Log4j hoặc SLF4J thay vì printStackTrace
            e.printStackTrace();
        }
    }

    @Override
    @Async("taskExecutor")
    public void sendEmailApprovalSuccessForAddendum(Addendum addendum, User user) {
        try {
            DataMailDTO dataMailDTO = new DataMailDTO();
            dataMailDTO.setTo(new String[]{user.getEmail()});
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.ADDENDUM_APPROVAL_SUCCESS);

            // Thiết lập các thuộc tính cho email
            Map<String, Object> props = new HashMap<>();
            props.put("contractNumber", addendum.getContractNumber());
            props.put("addendumTitle", addendum.getTitle());
            dataMailDTO.setProps(props);

            // Gửi email HTML theo template đã định nghĩa
            sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.ADDENDUM_APPROVAL_SUCCESS);

            // Log thông báo gửi email thành công
            System.out.println("Đã gửi email nhắc nhở cho: " + user.getEmail());
        } catch (Exception e) {
            // Xử lý lỗi, có thể dùng framework logging như Log4j hoặc SLF4J thay vì printStackTrace
            e.printStackTrace();
        }
    }

    @Override
    public void sendEmailContractOverdue(Contract contract) {
        try {
            User director = userRepository.findAll().stream()
                    .filter(user1 -> user1.getRole() != null && Role.DIRECTOR.equalsIgnoreCase(user1.getRole().getRoleName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người duyệt có vai trò giám đốc"));
            DataMailDTO dataMailDTO = new DataMailDTO();
            assert director != null;
            dataMailDTO.setTo(new String[]{contract.getPartner().getEmail(), director.getEmail()});
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.CONTRACT_OVERDUE_NOTIFICATION);

            // Thiết lập các thuộc tính cho email
            Map<String, Object> props = new HashMap<>();
            props.put("contractNumber", contract.getContractNumber());
            props.put("contractTitle", contract.getTitle());
            props.put("contractExpiryDate", contract.getExpiryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            dataMailDTO.setProps(props);

            // Gửi email HTML theo template đã định nghĩa
            sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.CONTRACT_OVERDUE_NOTIFICATION);

            // Log thông báo gửi email thành công
            System.out.println("Đã gửi email nhắc nhở cho: " + contract.getUser().getEmail());
        } catch (Exception e) {
            // Xử lý lỗi, có thể dùng framework logging như Log4j hoặc SLF4J thay vì printStackTrace
            e.printStackTrace();
        }
    }

    @Override
    public void sendEmailContractEffectiveDate(Contract contract) {
        try {
            User director = userRepository.findAll().stream()
                    .filter(user1 -> user1.getRole() != null && Role.DIRECTOR.equalsIgnoreCase(user1.getRole().getRoleName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người duyệt có vai trò giám đốc"));
            DataMailDTO dataMailDTO = new DataMailDTO();
            dataMailDTO.setTo(new String[]{contract.getPartner().getEmail(),
                    director.getEmail(),
                    contract.getUser().getEmail()});
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.CONTRACT_EFFECTIVE_DATE_REMINDER);

            // Thiết lập các thuộc tính cho email
            Map<String, Object> props = new HashMap<>();
            props.put("contractNumber", contract.getContractNumber());
            props.put("contractTitle", contract.getTitle());
            props.put("contractEffectiveDate", contract.getEffectiveDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            dataMailDTO.setProps(props);

            // Gửi email HTML theo template đã định nghĩa
            sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.CONTRACT_EFFECTIVE_DATE_REMINDER);

            // Log thông báo gửi email thành công
            System.out.println("Đã gửi email nhắc nhở cho: " + contract.getUser().getEmail());
        } catch (Exception e) {
            // Xử lý lỗi, có thể dùng framework logging như Log4j hoặc SLF4J thay vì printStackTrace
            e.printStackTrace();
        }
    }

    @Override
    public void sendEmailContractExpiryDate(Contract contract) {
        try {
            User director = userRepository.findAll().stream()
                    .filter(user1 -> user1.getRole() != null && Role.DIRECTOR.equalsIgnoreCase(user1.getRole().getRoleName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người duyệt có vai trò giám đốc"));
            DataMailDTO dataMailDTO = new DataMailDTO();

            dataMailDTO.setTo(new String[]{contract.getPartner().getEmail(), director.getEmail()});
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.CONTRACT_EXPIRY_DATE_REMINDER);

            // Thiết lập các thuộc tính cho email
            Map<String, Object> props = new HashMap<>();
            props.put("contractNumber", contract.getContractNumber());
            props.put("contractTitle", contract.getTitle());
            props.put("contractExpiryDate", contract.getExpiryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            dataMailDTO.setProps(props);

            // Gửi email HTML theo template już định nghĩa
            sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.CONTRACT_EXPIRY_DATE_REMINDER);

            // Log thông báo gửi email thành công
            System.out.println("Đã gửi email nhắc nhở cho: " + contract.getUser().getEmail());
        } catch (Exception e) {
            // Xử lý lỗi, có thể dùng framework logging như Log4j hoặc SLF4J thay vì printStackTrace
            e.printStackTrace();
        }
    }

    @Override
    @Async("taskExecutor")
    public void sendEmailContractSignedSuccess(Contract contract) {
        try {
            DataMailDTO dataMailDTO = new DataMailDTO();
            dataMailDTO.setTo(new String[]{contract.getUser().getEmail()});
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.CONTRACT_SIGNED_SUCCESS);

            // Thiết lập các thuộc tính cho email
            Map<String, Object> props = new HashMap<>();
            props.put("contractNumber", contract.getContractNumber());
            props.put("contractTitle", contract.getTitle());
            props.put("signedBy", contract.getSignedBy());
            dataMailDTO.setProps(props);

            // Gửi email HTML theo template już định nghĩa
            sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.CONTRACT_SIGNED_SUCCESS);

            // Log thông báo gửi email thành công
            System.out.println("Đã gửi email nhắc nhở cho: " + contract.getUser().getEmail());
        } catch (Exception e) {
            // Xử lý lỗi, có thể dùng framework logging như Log4j hoặc SLF4J thay vì printStackTrace
            e.printStackTrace();
        }
    }

    @Override
    @Async("taskExecutor")
    public void sendEmailAddendumSignedSuccess(Addendum addendum) {
        try {
            DataMailDTO dataMailDTO = new DataMailDTO();
            dataMailDTO.setTo(new String[]{addendum.getUser().getEmail()});
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.ADDENDUM_SIGNED_SUCCESS);

            // Thiết lập các thuộc tính cho email
            Map<String, Object> props = new HashMap<>();
            props.put("contractNumber", addendum.getContractNumber());
            props.put("addendumTitle", addendum.getTitle());
            props.put("signedBy", addendum.getSignedBy());
            dataMailDTO.setProps(props);

            // Gửi email HTML theo template już định nghĩa
            sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.ADDENDUM_SIGNED_SUCCESS);

            // Log thông báo gửi email thành công
            System.out.println("Đã gửi email nhắc nhở cho: " + addendum.getUser().getEmail());
        } catch (Exception e) {
            // Xử lý lỗi, có thể dùng framework logging như Log4j hoặc SLF4J thay vì printStackTrace
            e.printStackTrace();
        }
    }

    @Override
    @Async("taskExecutor")
    public void sendEmailAddendumExtendedDate(Addendum addendum) {
        try {
            User director = userRepository.findAll().stream()
                    .filter(user1 -> user1.getRole() != null && Role.DIRECTOR.equalsIgnoreCase(user1.getRole().getRoleName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người duyệt có vai trò giám đốc"));
            DataMailDTO dataMailDTO = new DataMailDTO();
            assert director != null;
            dataMailDTO.setTo(new String[]{addendum.getContract().getPartner().getEmail(),director.getEmail(),
                    addendum.getUser().getEmail()});
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.CONTRACT_EXTENDED_REMINDER);

            // Thiết lập các thuộc tính cho email
            Map<String, Object> props = new HashMap<>();
            props.put("contractNumber", addendum.getContractNumber());
            props.put("addendumTitle", addendum.getTitle());
            props.put("addendumExtendContractDate", addendum.getExtendContractDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            props.put("addendumContractExpirationDate", addendum.getContractExpirationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            dataMailDTO.setProps(props);

            // Gửi email HTML theo template już định nghĩa
            sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.CONTRACT_EXTENDED_REMINDER);

            // Log thông báo gửi email thành công
            System.out.println("Đã gửi email nhắc nhở cho: " + addendum.getUser().getEmail());
        } catch (Exception e) {
            // Xử lý lỗi, có thể dùng framework logging như Log4j hoặc SLF4J thay vì printStackTrace
            e.printStackTrace();
        }
    }

    @Override
    @Async("taskExecutor")
    public void sendEmailAddendumEndExtendedDate(Addendum addendum) {
        User director = userRepository.findAll().stream()
                .filter(user1 -> user1.getRole() != null && Role.DIRECTOR.equalsIgnoreCase(user1.getRole().getRoleName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người duyệt có vai trò giám đốc"));
        try {
            DataMailDTO dataMailDTO = new DataMailDTO();
            dataMailDTO.setTo(new String[]{
                    addendum.getContract().getPartner().getEmail(),
                    director.getEmail()
            }); // Gửi email cho người dùng addendum.getUser().getEmail());
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.CONTRACT_EXTENDED_END_REMINDER);

            // Thiết lập các thuộc tính cho email
            Map<String, Object> props = new HashMap<>();
            props.put("contractNumber", addendum.getContractNumber());
            props.put("addendumTitle", addendum.getTitle());
            props.put("addendumExtendContractDate", addendum.getExtendContractDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            props.put("addendumContractExpirationDate", addendum.getContractExpirationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            dataMailDTO.setProps(props);

            // Gửi email HTML theo template już định nghĩa
            sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.CONTRACT_EXTENDED_END_REMINDER);

            // Log thông báo gửi email thành công
            System.out.println("Đã gửi email nhắc nhở cho: " + addendum.getUser().getEmail());
        } catch (Exception e) {
            // Xử lý lỗi, có thể dùng framework logging như Log4j hoặc SLF4J thay vì printStackTrace
            e.printStackTrace();
        }
    }

    @Override
    public void sendEmailPartnerContractEffectiveReminder(PartnerContract contract) {
        try {
            User director = userRepository.findAll().stream()
                    .filter(user1 -> user1.getRole() != null && Role.DIRECTOR.equalsIgnoreCase(user1.getRole().getRoleName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người duyệt có vai trò giám đốc"));
            DataMailDTO dataMailDTO = new DataMailDTO();
            dataMailDTO.setTo(new String[]{contract.getUser().getEmail(),
                    director.getEmail()});
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.PARTNER_CONTRACT_EFFECTIVE_DATE_REMINDER);

            // Thiết lập các thuộc tính cho email
            Map<String, Object> props = new HashMap<>();
            props.put("contractNumber", contract.getContractNumber());
            props.put("contractTitle", contract.getTitle());
            props.put("effectiveDate", contract.getEffectiveDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            props.put("expiryDate", contract.getExpiryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            dataMailDTO.setProps(props);

            // Gửi email HTML theo template już định nghĩa
            sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.PARTNER_CONTRACT_EFFECTIVE_DATE_REMINDER);

            // Log thông báo gửi email thành công
            System.out.println("Đã gửi email nhắc nhở cho: " + contract.getUser().getEmail());
        } catch (Exception e) {
            // Xử lý lỗi, có thể dùng framework logging như Log4j hoặc SLF4J thay vì printStackTrace
            e.printStackTrace();
        }
    }

    @Override
    public void sendEmailPartnerContractExpiryReminder(PartnerContract contract) {
        try {
            User director = userRepository.findAll().stream()
                    .filter(user1 -> user1.getRole() != null && Role.DIRECTOR.equalsIgnoreCase(user1.getRole().getRoleName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người duyệt có vai trò giám đốc"));
            DataMailDTO dataMailDTO = new DataMailDTO();
            dataMailDTO.setTo(new String[]{contract.getUser().getEmail(), director.getEmail(),
                    contract.getPartner().getEmail()});
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.PARTNER_CONTRACT_EXPIRY_DATE_REMINDER);

            // Thiết lập các thuộc tính cho email
            Map<String, Object> props = new HashMap<>();
            props.put("contractNumber", contract.getContractNumber());
            props.put("contractTitle", contract.getTitle());
            props.put("expiryDate", contract.getExpiryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            dataMailDTO.setProps(props);

            // Gửi email HTML theo template już định nghĩa
            sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.PARTNER_CONTRACT_EXPIRY_DATE_REMINDER);

            // Log thông báo gửi email thành công
            System.out.println("Đã gửi email nhắc nhở cho: " + contract.getUser().getEmail());
        } catch (Exception e) {
            // Xử lý lỗi, có thể dùng framework logging như Log4j hoặc SLF4J thay vì printStackTrace
            e.printStackTrace();
        }
    }

    @Override
    public void sendEmailPartnerContractPaymentReminder(PaymentSchedule payment) {
        try {
            DataMailDTO dataMailDTO = new DataMailDTO();
            dataMailDTO.setTo(new String[]{payment.getPartnerContract().getUser().getEmail(),
                    payment.getPartnerContract().getPartner().getEmail()});
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.PARTNER_CONTRACT_PAYMENT_REMINDER);

            // Thiết lập các thuộc tính cho email
            Map<String, Object> props = new HashMap<>();
            props.put("contractNumber", payment.getPartnerContract().getContractNumber());
            props.put("contractTitle", payment.getPartnerContract().getTitle());
            props.put("stage", payment.getPaymentOrder());
            props.put("paymentDate", payment.getPaymentDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            dataMailDTO.setProps(props);

            // Gửi email HTML theo template już định nghĩa
            sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.PARTNER_CONTRACT_PAYMENT_REMINDER);

            // Log thông báo gửi email thành công
            System.out.println("Đã gửi email nhắc nhở cho: " + payment.getPartnerContract().getUser().getEmail());
        } catch (Exception e) {
            // Xử lý lỗi, có thể dùng framework logging như Log4j hoặc SLF4J thay vì printStackTrace
            e.printStackTrace();
        }
    }

    @Override
    public void sendEmailPartnerContractPaymentExpired(PaymentSchedule payment) {
        try {
            User director = userRepository.findAll().stream()
                    .filter(user1 -> user1.getRole() != null && Role.DIRECTOR.equalsIgnoreCase(user1.getRole().getRoleName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người duyệt có vai trò giám đốc"));
            DataMailDTO dataMailDTO = new DataMailDTO();
            assert director != null;
            dataMailDTO.setTo(new String[]{payment.getPartnerContract().getUser().getEmail(), director.getEmail(),
                    payment.getPartnerContract().getPartner().getEmail()});
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.PARTNER_CONTRACT_PAYMENT_EXPIRED);

            // Thiết lập các thuộc tính cho email
            Map<String, Object> props = new HashMap<>();
            props.put("contractNumber", payment.getPartnerContract().getContractNumber());
            props.put("contractTitle", payment.getPartnerContract().getTitle());
            props.put("stage", payment.getPaymentOrder());
            props.put("paymentDate", payment.getPaymentDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            dataMailDTO.setProps(props);

            // Gửi email HTML theo template już định nghĩa
            sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.PARTNER_CONTRACT_PAYMENT_EXPIRED);

            // Log thông báo gửi email thành công
            System.out.println("Đã gửi email nhắc nhở cho: " + payment.getPartnerContract().getUser().getEmail());
        } catch (Exception e) {
            // Xử lý lỗi, có thể dùng framework logging như Log4j hoặc SLF4J thay vì printStackTrace
            e.printStackTrace();
        }
    }

    @Override
    public void sendEmailContractSignedOverdue(Contract contract, User director, User approver) {
        try {
            DataMailDTO dataMailDTO = new DataMailDTO();
            assert director != null;
            dataMailDTO.setTo(new String[]{director.getEmail()});
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.CONTRACT_SIGN_OVERDUE);

            // Thiết lập các thuộc tính cho email
            Map<String, Object> props = new HashMap<>();
            props.put("contractNumber", contract.getContractNumber());
            props.put("contractTitle", contract.getTitle());
            props.put("signingDate", contract.getSigningDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            props.put("approver", approver.getFullName());
            dataMailDTO.setProps(props);

            // Gửi email HTML theo template już định nghĩa
            sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.CONTRACT_SIGN_OVERDUE);

            // Log thông báo gửi email thành công
            System.out.println("Đã gửi email nhắc nhở cho: " + director.getEmail());
        } catch (Exception e) {
            // Xử lý lỗi, có thể dùng framework logging như Log4j hoặc SLF4J thay vì printStackTrace
            e.printStackTrace();
        }
    }
}

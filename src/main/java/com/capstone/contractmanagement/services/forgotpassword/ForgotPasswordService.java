package com.capstone.contractmanagement.services.forgotpassword;

import com.capstone.contractmanagement.dtos.DataMailDTO;
import com.capstone.contractmanagement.dtos.forgotpassword.ChangePasswordDTO;
import com.capstone.contractmanagement.entities.ForgotPassword;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.IForgotPasswordRepository;
import com.capstone.contractmanagement.repositories.IUserRepository;
import com.capstone.contractmanagement.services.sendmails.MailService;
import com.capstone.contractmanagement.services.user.IUserService;
import com.capstone.contractmanagement.utils.MailTemplate;
import com.capstone.contractmanagement.utils.MessageKeys;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ForgotPasswordService implements IForgotPasswordService {

    private final IForgotPasswordRepository forgotPasswordRepository;
    private final MailService mailService;
    private final IUserRepository userRepository;
    private final IUserService userService;
    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordService.class);
    private static final int MAX_OTP_ATTEMPTS = 5;

    // Sử dụng SecureRandom để tạo OTP an toàn
    private Integer otpGenerator() {
        SecureRandom secureRandom = new SecureRandom();
        return secureRandom.nextInt(900000) + 100000; // OTP 6 chữ số
    }

    private void sendOtpMail(ForgotPassword forgotPassword) {
        try {
            DataMailDTO dataMailDTO = new DataMailDTO();
            dataMailDTO.setTo(new String[]{forgotPassword.getUser().getEmail()});
            dataMailDTO.setSubject(MailTemplate.SEND_MAIL_SUBJECT.OTP_SEND);

            Map<String, Object> props = new HashMap<>();
            props.put("otp", forgotPassword.getOtp());
            dataMailDTO.setProps(props);

            mailService.sendHtmlMail(dataMailDTO, MailTemplate.SEND_MAIL_TEMPLATE.OTP_SEND_TEMPLATE);
        } catch (MessagingException e) {
            logger.error("Failed to send OTP email", e);
        }
    }

    @Override
    public void verifyEmailAndSendOtp(String email) throws DataNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.USER_NOT_FOUND));

        // Kiểm tra nếu đã có OTP chưa xác thực và chưa hết hạn thì không gửi OTP mới
        Optional<ForgotPassword> existingOtpOpt = forgotPasswordRepository.findLatestOtpSent(email);
        if(existingOtpOpt.isPresent()){
            ForgotPassword existingOtp = existingOtpOpt.get();
            if(existingOtp.getExpirationTime().after(new Date())){
                throw new DataNotFoundException("OTP đã được gửi, vui lòng kiểm tra email của bạn.");
            }
        }

        int otp = otpGenerator();
        ForgotPassword fp = ForgotPassword.builder()
                .otp(otp)
                .expirationTime(new Date(System.currentTimeMillis() + 60 * 1000)) // OTP hợp lệ trong 60 giây
                .verified(false)
                .otpAttempts(0)
                .user(user)
                .build();
        forgotPasswordRepository.save(fp);
        sendOtpMail(fp);
    }

    @Override
    public String verifyOTP(String email, Integer otp) throws DataNotFoundException {
        ForgotPassword forgotPassword = forgotPasswordRepository.findLatestOtpSent(email)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.OTP_NOT_FOUND));

        // Kiểm tra số lần thử OTP
        if (forgotPassword.getOtpAttempts() >= MAX_OTP_ATTEMPTS) {
            throw new DataNotFoundException("Số lần thử OTP đã vượt quá giới hạn. Vui lòng yêu cầu gửi lại OTP.");
        }

        if (!forgotPassword.getOtp().equals(otp)) {
            // Tăng số lần thử OTP và lưu lại
            forgotPassword.setOtpAttempts(forgotPassword.getOtpAttempts() + 1);
            forgotPasswordRepository.save(forgotPassword);
            throw new DataNotFoundException(MessageKeys.OTP_INCORRECT);
        }
        if (forgotPassword.getExpirationTime().before(new Date())) {
            throw new DataNotFoundException(MessageKeys.OTP_IS_EXPIRED);
        }
        // Xác thực thành công, tạo reset token cho quá trình thay đổi mật khẩu
        forgotPassword.setVerified(true);
        String resetToken = UUID.randomUUID().toString();
        forgotPassword.setResetToken(resetToken);
        forgotPasswordRepository.save(forgotPassword);
        return resetToken;
    }

    // Phương thức dọn dẹp các OTP hết hạn, chạy theo lịch trình (mỗi 60 giây)
    @Scheduled(fixedDelay = 60000)
    public void removeExpiredOtps() {
        Date now = new Date();
        forgotPasswordRepository.findAllExpired(now)
                .forEach(forgotPasswordRepository::delete);
    }

    // Phương thức chuyển đổi mật khẩu sau khi OTP đã được xác thực
    @Override
    public void changePassword(String email, String resetToken, ChangePasswordDTO changePasswordDTO) throws DataNotFoundException {
        // Lấy OTP đã xác thực dựa trên email và resetToken
        ForgotPassword fp = forgotPasswordRepository.findLatestVerifiedOtp(email, resetToken)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.OTP_NOT_FOUND));

        if (!changePasswordDTO.getNewPassword().equals(changePasswordDTO.getConfirmPassword())) {
            throw new DataNotFoundException(MessageKeys.PASSWORD_NOT_MATCH);
        }

        // Cập nhật mật khẩu cho người dùng
        userService.updatePassword(email, changePasswordDTO.getNewPassword());
        // Sau khi thay đổi mật khẩu thành công, xoá OTP đã sử dụng
        forgotPasswordRepository.delete(fp);
    }
}

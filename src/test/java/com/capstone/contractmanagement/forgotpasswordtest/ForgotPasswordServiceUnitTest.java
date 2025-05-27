package com.capstone.contractmanagement.forgotpasswordtest;

import com.capstone.contractmanagement.dtos.DataMailDTO;
import com.capstone.contractmanagement.dtos.forgotpassword.ChangePasswordDTO;
import com.capstone.contractmanagement.entities.ForgotPassword;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.IForgotPasswordRepository;
import com.capstone.contractmanagement.repositories.IUserRepository;
import com.capstone.contractmanagement.services.forgotpassword.ForgotPasswordService;
import com.capstone.contractmanagement.services.sendmails.MailService;
import com.capstone.contractmanagement.services.user.IUserService;
import com.capstone.contractmanagement.utils.MailTemplate;
import com.capstone.contractmanagement.utils.MessageKeys;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ForgotPasswordServiceUnitTest {

    @Mock private IForgotPasswordRepository forgotPasswordRepository;
    @Mock private MailService mailService;
    @Mock private IUserRepository userRepository;
    @Mock private IUserService userService;

    @InjectMocks
    private ForgotPasswordService forgotPasswordService;

    private final String email = "test@example.com";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void verifyEmailAndSendOtp_ShouldSendOtp_WhenNoExistingOtp() throws Exception {
        User user = new User();
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(forgotPasswordRepository.findLatestOtpSent(email)).thenReturn(Optional.empty());

        forgotPasswordService.verifyEmailAndSendOtp(email);

        verify(forgotPasswordRepository, times(1)).save(any(ForgotPassword.class));
        verify(mailService, times(1)).sendHtmlMail(any(DataMailDTO.class), anyString());
    }

    @Test
    void verifyEmailAndSendOtp_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        Exception ex = assertThrows(DataNotFoundException.class, () ->
                forgotPasswordService.verifyEmailAndSendOtp(email));

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void verifyEmailAndSendOtp_ShouldThrowException_WhenOtpNotExpired() {
        User user = new User();
        user.setEmail(email);

        ForgotPassword existing = ForgotPassword.builder()
                .expirationTime(new Date(System.currentTimeMillis() + 60000)) // not expired
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(forgotPasswordRepository.findLatestOtpSent(email)).thenReturn(Optional.of(existing));

        Exception ex = assertThrows(DataNotFoundException.class, () ->
                forgotPasswordService.verifyEmailAndSendOtp(email));

        assertTrue(ex.getMessage().contains("OTP đã được gửi"));
    }

    @Test
    void verifyOTP_ShouldReturnToken_WhenOtpIsCorrect() throws Exception {
        ForgotPassword fp = ForgotPassword.builder()
                .otp(123456)
                .otpAttempts(0)
                .expirationTime(new Date(System.currentTimeMillis() + 60000))
                .verified(false)
                .build();

        when(forgotPasswordRepository.findLatestOtpSent(email)).thenReturn(Optional.of(fp));

        String token = forgotPasswordService.verifyOTP(email, 123456);

        assertNotNull(token);
        assertTrue(fp.getVerified());
        verify(forgotPasswordRepository).save(fp);
    }

    @Test
    void verifyOTP_ShouldThrow_WhenMaxAttemptsExceeded() {
        ForgotPassword fp = ForgotPassword.builder()
                .otpAttempts(5)
                .build();

        when(forgotPasswordRepository.findLatestOtpSent(email)).thenReturn(Optional.of(fp));

        Exception ex = assertThrows(DataNotFoundException.class, () ->
                forgotPasswordService.verifyOTP(email, 123456));

        assertTrue(ex.getMessage().contains("Số lần thử OTP đã vượt quá"));
    }

    @Test
    void verifyOTP_ShouldThrow_WhenOtpIsIncorrect() {
        ForgotPassword fp = ForgotPassword.builder()
                .otp(999999)
                .otpAttempts(0)
                .expirationTime(new Date(System.currentTimeMillis() + 60000))
                .build();

        when(forgotPasswordRepository.findLatestOtpSent(email)).thenReturn(Optional.of(fp));

        Exception ex = assertThrows(DataNotFoundException.class, () ->
                forgotPasswordService.verifyOTP(email, 123456));

        assertTrue(ex.getMessage().contains("OTP không chính xác"));
        verify(forgotPasswordRepository).save(fp);
        assertEquals(1, fp.getOtpAttempts());
    }

    @Test
    void verifyOTP_ShouldThrow_WhenOtpIsExpired() {
        ForgotPassword fp = ForgotPassword.builder()
                .otp(123456)
                .otpAttempts(0)
                .expirationTime(new Date(System.currentTimeMillis() - 1000))
                .build();

        when(forgotPasswordRepository.findLatestOtpSent(email)).thenReturn(Optional.of(fp));

        Exception ex = assertThrows(DataNotFoundException.class, () ->
                forgotPasswordService.verifyOTP(email, 123456));

        assertEquals("OTP đã hết hạn", ex.getMessage());
    }

    @Test
    void changePassword_ShouldChangePassword_WhenValidToken() throws Exception {
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setNewPassword("newpass123");
        dto.setConfirmPassword("newpass123");

        ForgotPassword fp = new ForgotPassword();
        when(forgotPasswordRepository.findLatestVerifiedOtp(email, "token123"))
                .thenReturn(Optional.of(fp));

        forgotPasswordService.changePassword(email, "token123", dto);

        verify(userService).updatePassword(email, "newpass123");
        verify(forgotPasswordRepository).delete(fp);
    }

    @Test
    void changePassword_ShouldThrow_WhenTokenInvalid() {
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setNewPassword("abc");
        dto.setConfirmPassword("abc");

        when(forgotPasswordRepository.findLatestVerifiedOtp(email, "wrongtoken"))
                .thenReturn(Optional.empty());

        Exception ex = assertThrows(DataNotFoundException.class, () ->
                forgotPasswordService.changePassword(email, "wrongtoken", dto));

        assertEquals("OTP not found", ex.getMessage());
    }

    @Test
    void changePassword_ShouldThrow_WhenPasswordsDoNotMatch() {
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setNewPassword("abc123");
        dto.setConfirmPassword("xyz123");

        ForgotPassword fp = new ForgotPassword();
        when(forgotPasswordRepository.findLatestVerifiedOtp(email, "token123"))
                .thenReturn(Optional.of(fp));

        Exception ex = assertThrows(DataNotFoundException.class, () ->
                forgotPasswordService.changePassword(email, "token123", dto));

        assertEquals("Mật khẩu xác nhận không khớp", ex.getMessage());
    }
}

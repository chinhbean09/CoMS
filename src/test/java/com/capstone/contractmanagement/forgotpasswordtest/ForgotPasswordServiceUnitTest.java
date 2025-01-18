package com.capstone.contractmanagement.forgotpasswordtest;

import com.capstone.contractmanagement.dtos.DataMailDTO;
import com.capstone.contractmanagement.entities.ForgotPassword;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.IForgotPasswordRepository;
import com.capstone.contractmanagement.repositories.IUserRepository;
import com.capstone.contractmanagement.services.forgotpassword.ForgotPasswordService;
import com.capstone.contractmanagement.services.sendmails.MailService;
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

@ExtendWith(MockitoExtension.class)
public class ForgotPasswordServiceUnitTest {

    @Mock
    private IForgotPasswordRepository forgotPasswordRepository;

    @Mock
    private IUserRepository userRepository;

    @Mock
    private MailService mailService;

    @Mock
    private ScheduledExecutorService executorService;

    @InjectMocks
    private ForgotPasswordService forgotPasswordService;

    private User testUser;
    private ForgotPassword forgotPassword;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        forgotPassword = ForgotPassword.builder()
                .otp(123456)
                .expirationTime(new Date(System.currentTimeMillis() + 60 * 1000))
                .verified(false)
                .user(testUser)
                .build();
    }

    @Test
    void verifyEmailAndSendOtp_ShouldSendOtp_WhenUserExists() throws DataNotFoundException, MessagingException {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(forgotPasswordRepository.save(any(ForgotPassword.class))).thenReturn(forgotPassword);

        forgotPasswordService.verifyEmailAndSendOtp("test@example.com");

        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(forgotPasswordRepository, times(1)).save(any(ForgotPassword.class));
        verify(mailService, times(1)).sendHtmlMail(any(DataMailDTO.class), eq(MailTemplate.SEND_MAIL_TEMPLATE.OTP_SEND_TEMPLATE));
    }

    @Test
    void verifyEmailAndSendOtp_ShouldThrowException_WhenUserDoesNotExist() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        DataNotFoundException exception = assertThrows(DataNotFoundException.class,
                () -> forgotPasswordService.verifyEmailAndSendOtp("nonexistent@example.com"));

        assertEquals(MessageKeys.USER_NOT_FOUND, exception.getMessage());
        verify(userRepository, times(1)).findByEmail("nonexistent@example.com");
        verifyNoInteractions(mailService);
    }

    @Test
    void verifyOTP_ShouldVerify_WhenOtpIsCorrectAndNotExpired() throws DataNotFoundException {
        when(forgotPasswordRepository.findLatestOtpSent("test@example.com")).thenReturn(Optional.of(forgotPassword));

        forgotPasswordService.verifyOTP("test@example.com", 123456);

        //assertTrue(forgotPassword.isVerified());
        verify(forgotPasswordRepository, times(1)).save(forgotPassword);
    }

    @Test
    void verifyOTP_ShouldThrowException_WhenOtpIsIncorrect() {
        when(forgotPasswordRepository.findLatestOtpSent("test@example.com")).thenReturn(Optional.of(forgotPassword));

        DataNotFoundException exception = assertThrows(DataNotFoundException.class,
                () -> forgotPasswordService.verifyOTP("test@example.com", 654321));

        assertEquals(MessageKeys.OTP_INCORRECT, exception.getMessage());
    }

    @Test
    void verifyOTP_ShouldThrowException_WhenOtpIsExpired() {
        forgotPassword.setExpirationTime(new Date(System.currentTimeMillis() - 1000));
        when(forgotPasswordRepository.findLatestOtpSent("test@example.com")).thenReturn(Optional.of(forgotPassword));

        DataNotFoundException exception = assertThrows(DataNotFoundException.class,
                () -> forgotPasswordService.verifyOTP("test@example.com", 123456));

        assertEquals(MessageKeys.OTP_IS_EXPIRED, exception.getMessage());
    }
}

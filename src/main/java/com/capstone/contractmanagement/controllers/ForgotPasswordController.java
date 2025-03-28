package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.dtos.forgotpassword.ChangePasswordDTO;
import com.capstone.contractmanagement.dtos.forgotpassword.ForgotPasswordDTO;
import com.capstone.contractmanagement.entities.ForgotPassword;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.repositories.IForgotPasswordRepository;
import com.capstone.contractmanagement.services.forgotpassword.IForgotPasswordService;
import com.capstone.contractmanagement.services.user.IUserService;
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/forgot-password")
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final IForgotPasswordService forgotPasswordService;
    private final IUserService userService;
    private final IForgotPasswordRepository forgotPasswordRepository;

    @PostMapping("/send-otp/{email}")
    public ResponseEntity<ResponseObject> sendOtp(@PathVariable String email) {
        try {
            forgotPasswordService.verifyEmailAndSendOtp(email);
            return ResponseEntity.status(HttpStatus.OK).body(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message(MessageKeys.OTP_SENT_SUCCESSFULLY)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseObject.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message(e.getMessage())
                    .build());
        }
    }

    @PostMapping("/verify-otp/{email}")
    public ResponseEntity<ResponseObject> verifyOtp(@PathVariable String email,
                                                    @RequestBody ForgotPasswordDTO forgotPasswordDTO) {
        try {
            // Nhận reset token ngay từ service sau khi OTP được xác thực
            String resetToken = forgotPasswordService.verifyOTP(email, forgotPasswordDTO.getOtp());
            return ResponseEntity.status(HttpStatus.OK).body(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message(MessageKeys.OTP_VERIFIED_SUCCESSFULLY)
                    .data(resetToken)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseObject.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message(e.getMessage())
                    .build());
        }
    }

    @PostMapping("/change-password/{email}")
    public ResponseEntity<ResponseObject> changePassword(@PathVariable String email,
                                                         @RequestParam String resetToken,
                                                         @RequestBody ChangePasswordDTO changePasswordDTO) {
        try {
            // Chuyển toàn bộ logic đổi mật khẩu sang lớp service
            forgotPasswordService.changePassword(email, resetToken, changePasswordDTO);
            return ResponseEntity.status(HttpStatus.OK).body(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Thay đổi mật khẩu thành công.")
                    .build());
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseObject.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message("Đã có lỗi xảy ra, vui lòng thử lại sau.")
                    .build());
        }
    }
}

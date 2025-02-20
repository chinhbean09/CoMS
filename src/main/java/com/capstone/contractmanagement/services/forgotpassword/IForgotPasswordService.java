package com.capstone.contractmanagement.services.forgotpassword;

import com.capstone.contractmanagement.dtos.forgotpassword.ChangePasswordDTO;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;

public interface IForgotPasswordService {
    void verifyEmailAndSendOtp(String email) throws DataNotFoundException;

    String verifyOTP(String email, Integer otp) throws DataNotFoundException;

    void changePassword(String email, String resetToken, ChangePasswordDTO changePasswordDTO) throws DataNotFoundException;
}

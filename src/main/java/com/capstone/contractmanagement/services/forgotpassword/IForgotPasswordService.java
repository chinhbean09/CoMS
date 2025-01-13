package com.capstone.contractmanagement.services.forgotpassword;

import com.capstone.contractmanagement.exceptions.DataNotFoundException;

public interface IForgotPasswordService {
    void verifyEmailAndSendOtp(String email) throws DataNotFoundException;

    void verifyOTP(String email, Integer otp) throws DataNotFoundException;
}

package com.capstone.contractmanagement.utils;

public class MailTemplate {
    public final static class SEND_MAIL_SUBJECT {
        public final static String USER_REGISTER = "TẠO TÀI KHOẢN THÀNH CÔNG!";

        public final static String OTP_SEND = "MÃ OTP XÁC THỰC";

        public final static String PAYMENT_SUCCESS = "THANH TOÁN THÀNH CÔNG!";

        public final static String CONTRACT_PAYMENT_EXPIRED = "ĐÃ QUÁ HẠN THANH TOÁN HỢP ĐỒNG";

        public final static String CONTRACT_PAYMENT_NOTIFICATION = "NHẮC NHỞ THANH TOÁN HỢP ĐỔNG";
//
//        public final static String BOOKING_PAYMENT_SUCCESS = "BOOKING PAYMENT SUCCESSFUL!";
//
//        public final static String PACKAGE_PAYMENT_SUCCESS = "PACKAGE PAYMENT SUCCESSFUL!";
    }

    public final static class SEND_MAIL_TEMPLATE {
        public final static String USER_REGISTER = "register";

        public final static String OTP_SEND_TEMPLATE = "otp-sent";

        public final static String PAYMENT_SUCCESS_TEMPLATE = "payment-success";

        public final static String PACKAGE_EXPIRED_TEMPLATE = "package-expired";

        public final static String CONTRACT_PAYMENT_NOTIFICATION = "contract-payment-notification";

        public final static String CONTRACT_PAYMENT_EXPIRED = "contract-payment-expired";

//        public final static String NEW_PASSWORD = "new-password";
//        public final static String BOOKING_PAYMENT_SUCCESS_TEMPLATE = "booking";
//
//        public final static String PACKAGE_PAYMENT_SUCCESS_TEMPLATE = "package-payment";
    }
}

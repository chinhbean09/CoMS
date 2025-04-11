package com.capstone.contractmanagement.utils;

public class MailTemplate {
    public final static class SEND_MAIL_SUBJECT {
        public final static String USER_REGISTER = "TẠO TÀI KHOẢN THÀNH CÔNG!";

        public final static String OTP_SEND = "MÃ OTP XÁC THỰC";

        public final static String PAYMENT_SUCCESS = "THANH TOÁN THÀNH CÔNG!";

        public final static String CONTRACT_PAYMENT_EXPIRED = "ĐÃ QUÁ HẠN THANH TOÁN HỢP ĐỒNG";

        public final static String CONTRACT_PAYMENT_NOTIFICATION = "NHẮC NHỞ THANH TOÁN HỢP ĐỔNG";

        public final static String CONTRACT_APPROVAL_NOTIFICATION = "NHẮC NHỞ DUYỆT HỢP ĐỔNG";

        public final static String UPDATE_CONTRACT_REQUEST = "NHẮC NHỞ CẠP NHẤT HỢP ĐỔNG";
        public final static String UPDATE_ADDENDUM_REQUEST = "NHẮC NHỞ CẬP NHẬT PHỤ LỤC";
        public final static String ADDENDUM_APPROVAL_NOTIFICATION = "NHẮC NHỞ PHÊ DUYỆT PHỤ LỤC";
        public final static String APPROVAL_CONTRACT_SUCCESS = "HỢP ĐỒNG ĐÃ HOÀN TẤT PHÊ DUYỆT";
        public final static String ADDENDUM_APPROVAL_SUCCESS = "PHỤ LỤC ĐÃ HOÀN TẤT PHÊ DUYỆT";
        public final static String CONTRACT_EFFECTIVE_DATE_REMINDER = "HỢP ĐỒNG CÓ HIỆU LỰC";
        public final static String CONTRACT_EXPIRY_DATE_REMINDER = "HỢP ĐỒNG SẮP HẾT HẠN";
        public final static String CONTRACT_OVERDUE_NOTIFICATION = "HỢP ĐỒNG ĐÃ HẾT HẠN";
        public final static String CONTRACT_SIGNED_SUCCESS = "HỢP ĐỒNG ĐÃ ĐƯỢC KÍ";
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
        public final static String CONTRACT_APPROVAL_NOTIFICATION = "contract-approval-notification";
        public final static String UPDATE_CONTRACT_REQUEST = "update-contract-request";
        public final static String UPDATE_ADDENDUM_REQUEST = "update-addendum-request";
        public final static String ADDENDUM_APPROVAL_NOTIFICATION = "addendum-approval-notification";

        public final static String APPROVAL_CONTRACT_SUCCESS = "approval-contract-success";
        public final static String ADDENDUM_APPROVAL_SUCCESS = "addendum-approval-success";
        public final static String CONTRACT_EFFECTIVE_DATE_REMINDER = "contract-effective-date-reminder";
        public final static String CONTRACT_EXPIRY_DATE_REMINDER = "contract-expiry-date-reminder";
        public final static String CONTRACT_OVERDUE_NOTIFICATION = "contract-overdue-notification";
        public final static String CONTRACT_SIGNED_SUCCESS = "contract-signed-success";


//        public final static String NEW_PASSWORD = "new-password";
//        public final static String BOOKING_PAYMENT_SUCCESS_TEMPLATE = "booking";
//
//        public final static String PACKAGE_PAYMENT_SUCCESS_TEMPLATE = "package-payment";
    }
}

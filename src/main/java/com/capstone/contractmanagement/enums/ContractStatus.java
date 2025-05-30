package com.capstone.contractmanagement.enums;

public enum ContractStatus {
    CREATED,     // Đã tạo
    UPDATED,
    APPROVAL_PENDING, // Chờ phê duyệt
    APPROVED,    // Đã phê duyệt
    PENDING,     // Chưa ký
    REJECTED,    // Bị từ chối
    FIXED,       // Đã chính sửa
    SIGNED,      // Đã ký
    ACTIVE,      // Đang có hiệu lực
    COMPLETED,   // Hoàn thành
    EXPIRED,     // Hết hạn
    CANCELLED,   // Đã hủy
    ENDED,        // Kết thúc
    DELETED,     // Đã xóa
    LIQUIDATED,   // Đã thanh lý
    SIGN_OVERDUE, // Đã ký quá hạn
}
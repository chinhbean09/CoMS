package com.capstone.contractmanagement.enums;

public enum ContractStatus {
    DRAFT,       // Bản nháp
    CREATED,     // Đã tạo
    APPROVAL_PENDING, // Chờ phê duyệt
    APPROVED,    // Đã phê duyệt
    PENDING,     // Chưa ký
    REJECTED,    // Bị từ chối
    SIGNED,      // Đã ký
    ACTIVE,      // Đang có hiệu lực
    COMPLETED,   // Hoàn thành
    EXPIRED,     // Hết hạn
    CANCELLED,   // Đã hủy
    ENDED        // Kết thúc
}
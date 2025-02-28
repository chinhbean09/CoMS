package com.capstone.contractmanagement.dtos.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TermSnapshotDTO {
    // Đây là ID của term gốc (để tham chiếu nếu cần)
    private Long id;
    private String label;      // Label của điều khoản
    private String value;      // Nội dung (value) của điều khoản
    // Nếu cần phân loại (ví dụ: “LEGAL_BASIS”, “GENERAL_TERMS”, “OTHER_TERMS”, “ADDITIONAL_TERMS”)
    private String termType;
    // Nếu là additional term, có thể có field thêm cho nhóm (Common, A, B)
    private String additionalGroup;
}

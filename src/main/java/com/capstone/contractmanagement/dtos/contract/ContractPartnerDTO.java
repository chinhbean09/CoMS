    package com.capstone.contractmanagement.dtos.contract;

    import lombok.AllArgsConstructor;
    import lombok.Builder;
    import lombok.Data;
    import lombok.NoArgsConstructor;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class ContractPartnerDTO {
        private Long partnerId; // ID của partner trong database (nếu có)
        private String partnerName; // Tên partner
        private String partnerAddress; // Địa chỉ
        private String partnerTaxCode; // Mã số thuế
        private String partnerPhone; // Số điện thoại
        private String partnerEmail; // Email

    }

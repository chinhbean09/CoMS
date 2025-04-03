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
        private Long partnerId;
        private String partnerName;
        private String partnerAddress;
        private String partnerTaxCode;
        private String partnerPhone;
        private String partnerEmail;
        private String spokesmanName;
        private String position;
    }

package com.capstone.contractmanagement.responses.contract_partner;


import com.capstone.contractmanagement.responses.payment_schedule.PaymentScheduleResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractPartnerResponse {


    private Long contractPartnerId;
    private String contractNumber;
    private Double totalValue;
    private String partnerName;
    private LocalDateTime signingDate;
    private LocalDateTime effectiveDate;
    private LocalDateTime expiryDate;
    private String title;
    private String fileUrl;
    private List<ContractPartnerItemResponse> items;
    private List<PaymentScheduleResponse> paymentSchedules;

}

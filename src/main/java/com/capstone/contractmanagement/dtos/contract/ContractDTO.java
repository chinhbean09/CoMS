package com.capstone.contractmanagement.dtos.contract;

import com.capstone.contractmanagement.dtos.payment.PaymentDTO;
import com.capstone.contractmanagement.enums.ContractStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ContractDTO {
    private Long templateId;
    private Long partnerId;
    private String contractNumber;
    private LocalDateTime signingDate;
    private String contractLocation;
    private Double totalValue;
    private List<PaymentDTO> payments;
    private LocalDateTime effectiveDate;
    private LocalDateTime expiryDate;
    private LocalDateTime notifyEffectiveDate;
    private LocalDateTime notifyExpiryDate;
    private String notifyEffectiveContent;
    private String notifyExpiryContent;
    private String contractTitle;

    @JsonProperty("TemplateData")
    private TemplateData templateData;

    private ContractStatus status;

    private List<ContractItemDTO> contractItems;
    private Integer contractNumberFormat;
}

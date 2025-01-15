package com.capstone.contractmanagement.dtos.contract;

import com.capstone.contractmanagement.entities.PaymentOneTime;
import com.capstone.contractmanagement.entities.PaymentSchedule;
import com.capstone.contractmanagement.enums.ContractStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
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
    @JsonProperty("")

    @NotBlank(message = "Title cannot be blank")
    private String title;

    @NotBlank(message = "Contract Number cannot be blank")
    private String contractNumber;

    private String description;

    private ContractStatus status;

    private LocalDateTime startDate;

    private String createdBy;

    private String scope;

    private String configuration;

    private String sla;

    private String confidentiality;

    private String obligations;

    private Double amount;

    private Long userId;

    private Long templateId;

    private Long partyId;

    private List<Long> termIds;

    private List<PaymentSchedule> paymentSchedules;

    private PaymentOneTime paymentOneTime;

}

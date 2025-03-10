package com.capstone.contractmanagement.responses.contract;

import com.capstone.contractmanagement.entities.Party;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.responses.User.UserContractResponse;
import com.capstone.contractmanagement.responses.payment_schedule.PaymentScheduleResponse;
import com.capstone.contractmanagement.responses.term.TermResponse;
import com.capstone.contractmanagement.responses.term.TypeTermResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractResponse {
    private Long id;
    private String title;
    private UserContractResponse user;
    private Party party;
    private String contractNumber;
    private ContractStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime signingDate;
    private String contractLocation;
    private Double amount;
    private Long contractTypeId;
    private LocalDateTime effectiveDate;
    private LocalDateTime expiryDate;
    private LocalDateTime notifyEffectiveDate;
    private LocalDateTime notifyExpiryDate;
    private String notifyEffectiveContent;
    private String notifyExpiryContent;
    private String specialTermsA;
    private String specialTermsB;
    private String contractContent;
    private Boolean appendixEnabled;
    private Boolean transferEnabled;
    private Boolean autoAddVAT;
    private Double vatPercentage;
    private Boolean isDateLateChecked;
    private Integer maxDateLate;
    private Boolean autoRenew;
    private Boolean violate;
    private Boolean suspend;
    private String suspendContent;
    private Integer version;
    private List<TermResponse> legalBasisTerms;
    private List<TermResponse> generalTerms;
    private List<TermResponse> otherTerms;
    private List<TypeTermResponse> additionalTerms;
    private Map<String, Map<String, List<TermResponse>>> additionalConfig;

    // Thông tin thanh toán
    private List<PaymentScheduleResponse> paymentSchedules;

}

package com.capstone.contractmanagement.dtos.contract;

import com.capstone.contractmanagement.dtos.payment.PaymentDTO;
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
public class ContractCreateDTO {
    private String contractTitle;
    private Long partnerId;
    private LocalDateTime signingDate;
    private String contractLocation;
    private Double totalValue;
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
    private Boolean autoRenew;
    private Boolean violate;
    private Integer maxDateLate;
    private Boolean suspend;
    private String suspendContent;
    private Long contractTypeId;
    private List<TermSnapshotDTO> legalBasisTerms;
    private List<TermSnapshotDTO> generalTerms;
    private List<TermSnapshotDTO> otherTerms;
    private Map<String, Map<String, List<TermSnapshotDTO>>> additionalConfig;
    private List<PaymentDTO> payments;
}

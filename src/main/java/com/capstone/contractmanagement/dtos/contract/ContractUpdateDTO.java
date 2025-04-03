package com.capstone.contractmanagement.dtos.contract;
import com.capstone.contractmanagement.dtos.contract.TermSnapshotDTO;
import com.capstone.contractmanagement.dtos.payment.PaymentScheduleDTO;
import com.capstone.contractmanagement.enums.ContractStatus;
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
public class ContractUpdateDTO {

    private String title;
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
    private Integer maxDateLate;
    private Boolean autoRenew;
    private Boolean violate;
    private Boolean suspend;
    private String suspendContent;

    // Danh sách TermSnapshotDTO thay vì ContractTermDTO
    private List<TermSnapshotDTO> legalBasisTerms;
    private List<TermSnapshotDTO> generalTerms;
    private List<TermSnapshotDTO> otherTerms;

    // Cấu trúc additionalConfig giống trong TemplateData
    private Map<String, Map<String, List<TermSnapshotDTO>>> additionalConfig;

    private List<PaymentScheduleDTO> payments; // Danh sách PaymentSchedule để cập nhật

    private Long contractTypeId; // ID của loại hợp đồng

    private List<ContractItemDTO> contractItems;

    private ContractPartnerDTO partnerA; // Thông tin bên A
    private ContractPartnerDTO partnerB; // Thông tin bên B

}

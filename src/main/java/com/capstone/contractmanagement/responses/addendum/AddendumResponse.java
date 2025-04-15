package com.capstone.contractmanagement.responses.addendum;

import com.capstone.contractmanagement.dtos.contract.ContractItemDTO;
import com.capstone.contractmanagement.entities.Partner;
import com.capstone.contractmanagement.entities.contract.ContractPartner;
import com.capstone.contractmanagement.enums.AddendumStatus;
import com.capstone.contractmanagement.responses.payment_schedule.PaymentScheduleResponse;
import com.capstone.contractmanagement.responses.term.TermResponse;
import com.capstone.contractmanagement.responses.term.TypeTermResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Data
@Builder
public class AddendumResponse {
    private Long addendumId;
    private String title;
    private String content;
    private String contractNumber;
    private LocalDateTime effectiveDate;
    //private AddendumTypeResponse addendumType;
    private AddendumStatus status;
    private UserAddendumResponse createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long contractId;
    private Partner partnerA;
    private Optional<ContractPartner> partnerB;

    private List<TermResponse> legalBasisTerms;
    private List<TermResponse> generalTerms;
    private List<TermResponse> otherTerms;
    private List<TypeTermResponse> additionalTerms;
    private Map<String, Map<String, List<TermResponse>>> additionalConfig;

    // Thông tin thanh toán
    private List<PaymentScheduleResponse> paymentSchedules;
    private List<ContractItemDTO> contractItems;

    private LocalDateTime extendContractDate; // Thêm trường mới
    private LocalDateTime contractExpirationDate; // Thêm trường mới
}

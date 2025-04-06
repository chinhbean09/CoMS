package com.capstone.contractmanagement.responses.addendum;

import com.capstone.contractmanagement.entities.Partner;
import com.capstone.contractmanagement.entities.contract.ContractPartner;
import com.capstone.contractmanagement.enums.AddendumStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Optional;

@Data
@Builder
public class AddendumResponse {
    private Long addendumId;
    private String title;
    private String content;
    private String contractNumber;
    private LocalDateTime effectiveDate;
    private AddendumTypeResponse addendumType;
    private AddendumStatus status;
    private UserAddendumResponse createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long contractId;
    private Partner partnerA;
    private Optional<ContractPartner> partnerB;
}

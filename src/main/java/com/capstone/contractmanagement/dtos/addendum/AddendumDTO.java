package com.capstone.contractmanagement.dtos.addendum;

import com.capstone.contractmanagement.dtos.contract.AdditionalTermDTO;
import com.capstone.contractmanagement.dtos.contract.ContractItemDTO;
import com.capstone.contractmanagement.dtos.contract.TermSnapshotDTO;
import com.capstone.contractmanagement.dtos.payment.PaymentDTO;
import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AddendumDTO {
    private String title;
    private String content;
    private LocalDateTime effectiveDate;

    private Long contractId;
    private Long addendumTypeId;
    private List<ContractItemDTO> contractItems;
    private List<PaymentDTO> payments;

    //Term legal basis, generalTerm, otherTerms
    private List<AddendumTermSnapshotDTO> legalBasisTerms;
    private List<AddendumTermSnapshotDTO> generalTerms;
    private List<AddendumTermSnapshotDTO> otherTerms;

    // additionalConfig: cấu trúc map
    private Map<String, Map<String, List<AddendumTermSnapshotDTO>>> additionalConfig;

    private LocalDateTime extendContractDate;
    private LocalDateTime contractExpirationDate;

}

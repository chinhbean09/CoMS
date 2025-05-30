package com.capstone.contractmanagement.responses.term;


import com.capstone.contractmanagement.enums.TermStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GetAllTermsResponse {

    private Long id;
    private String clauseCode;
    private String label;
    private String value;
    private String type;
    private String identifier;
    private LocalDateTime createdAt;
    private TermStatus status;
    private TermCreatorResponse createdBy;
    private Integer version;
    // Số lượng contract_template sử dụng term này
    private int contractTemplateCount;

    // Số lượng contract sử dụng term này
    private int contractCount;

}

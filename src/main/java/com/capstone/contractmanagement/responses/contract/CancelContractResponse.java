package com.capstone.contractmanagement.responses.contract;

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
public class CancelContractResponse {
    private Long contractId;
    private String cancelContent;
    private LocalDateTime cancelAt;
    private List<String> urls;
}

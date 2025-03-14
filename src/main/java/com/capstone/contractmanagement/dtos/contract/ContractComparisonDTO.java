package com.capstone.contractmanagement.dtos.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractComparisonDTO {

    private String fieldName;
    private String fieldType; // BASIC, TERM, ADDITIONAL_TERM
    private Object oldValue;
    private Object newValue;
    private String changeType; // MODIFIED, ADDED, REMOVED
    private String groupName; // For additional terms (COMMON, A, B)

}

package com.capstone.contractmanagement.dtos.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TermChange {
    private Long originalTermId;
    private String termLabel;
    private String oldValue;
    private String newValue;
    private String termType;
    private String action; // "CREATE", "UPDATE", "DELETE"
}

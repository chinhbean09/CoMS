package com.capstone.contractmanagement.dtos.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldChange {
    private String oldValue;
    private String newValue;
    private String action; // "UPDATE", "CREATE", "DELETE"
}

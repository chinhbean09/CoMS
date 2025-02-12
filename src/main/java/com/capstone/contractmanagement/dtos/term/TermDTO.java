package com.capstone.contractmanagement.dtos.term;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TermDTO {
    private String label;
    private String value;
    private String clauseCode;
}

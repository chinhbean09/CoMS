package com.capstone.contractmanagement.dtos.term;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTermDTO {
    private String label;
    private String value;
    private String clauseCode;
}

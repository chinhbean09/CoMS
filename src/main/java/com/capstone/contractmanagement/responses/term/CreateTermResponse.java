package com.capstone.contractmanagement.responses.term;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTermResponse {
    private Long id;
    private String clauseCode;
    private String label;
    private String value;
    private String type;
}

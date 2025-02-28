package com.capstone.contractmanagement.responses.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TermSimpleResponse {
    private Long id;
    private String label;
    private String value;
    private String content;
}

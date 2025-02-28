package com.capstone.contractmanagement.responses.template;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractTemplateTitleResponse {
    private Long id;
    private String contractTitle;
}


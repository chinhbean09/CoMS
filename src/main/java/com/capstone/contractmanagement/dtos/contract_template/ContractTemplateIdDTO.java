package com.capstone.contractmanagement.dtos.contract_template;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContractTemplateIdDTO {
    private Long id;

    @NotBlank(message = "Contract title is required")
    private String contractTitle;


}

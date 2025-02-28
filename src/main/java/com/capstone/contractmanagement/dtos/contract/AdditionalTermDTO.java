package com.capstone.contractmanagement.dtos.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdditionalTermDTO {
    private Long id;
    private String name;
    private String identifier;  // Ví dụ: "ADDITIONAL_TERMS"
}

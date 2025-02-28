package com.capstone.contractmanagement.responses.contract;

import com.capstone.contractmanagement.entities.TypeTerm;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdditionalConfigTermResponse {
    private Long id;
    private Long originalTermId;
    private String termContent;
    private TypeTerm typeTerm;
    private String additionalGroup;
}

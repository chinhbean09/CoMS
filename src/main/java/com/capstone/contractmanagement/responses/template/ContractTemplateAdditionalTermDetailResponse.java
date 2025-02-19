package com.capstone.contractmanagement.responses.template;

import com.capstone.contractmanagement.dtos.term.TermSimpleDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ContractTemplateAdditionalTermDetailResponse {

    private Long typeTermId;
    private List<TermSimpleDTO> commonTerms;
    private List<TermSimpleDTO> aTerms;
    private List<TermSimpleDTO> bTerms;

}

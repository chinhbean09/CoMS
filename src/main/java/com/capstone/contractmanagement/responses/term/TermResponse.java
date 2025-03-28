package com.capstone.contractmanagement.responses.term;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TermResponse {

    @JsonProperty("original_term_id")
    private Long id;

    private String label;

    private String value;

}

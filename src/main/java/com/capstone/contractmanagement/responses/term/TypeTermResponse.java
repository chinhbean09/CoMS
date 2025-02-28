package com.capstone.contractmanagement.responses.term;

import com.capstone.contractmanagement.enums.TypeTermIdentifier;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypeTermResponse {
    @JsonProperty("original_term_id")
    private Long id;
    private String name;
    private TypeTermIdentifier identifier;
}

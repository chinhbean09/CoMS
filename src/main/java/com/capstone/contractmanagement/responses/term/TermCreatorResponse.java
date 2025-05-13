package com.capstone.contractmanagement.responses.term;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TermCreatorResponse {
    private Long id;
    private String name;
}

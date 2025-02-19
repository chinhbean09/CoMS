package com.capstone.contractmanagement.responses.term;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TermResponse {
    private Long id;
    private String label;
    private String value;

}

package com.capstone.contractmanagement.dtos.term;
import lombok.Builder;
import lombok.Data;

@Data
@Builder

public class TermSimpleDTO {
    private Long id;
    private String label;
    private String value;

}

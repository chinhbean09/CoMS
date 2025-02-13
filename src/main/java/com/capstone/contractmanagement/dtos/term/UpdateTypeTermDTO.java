package com.capstone.contractmanagement.dtos.term;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateTypeTermDTO {
    private String identifier;
    private String name;
}

package com.capstone.contractmanagement.responses.section;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionResponse {
    private Long id;
    private String sectionName;
    private Integer order;
}

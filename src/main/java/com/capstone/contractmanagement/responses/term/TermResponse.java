package com.capstone.contractmanagement.responses.term;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TermResponse {
    private Long id;
    private String title;
    private String description;
    private Boolean isDefault;
    private String createdAt;
}

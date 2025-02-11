package com.capstone.contractmanagement.responses.template;

import com.capstone.contractmanagement.responses.section.SectionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateResponse {
    private Long id;
    private String templateName;
    private String title;
    private String content;
    private String createdBy;
    private String createdAt;
    private String updatedAt;
    private List<SectionResponse> sections;
}

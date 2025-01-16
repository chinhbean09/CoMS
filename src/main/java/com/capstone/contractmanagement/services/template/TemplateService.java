package com.capstone.contractmanagement.services.template;

import com.capstone.contractmanagement.entities.template.Template;
import com.capstone.contractmanagement.repositories.ITemplateRepository;
import com.capstone.contractmanagement.responses.section.SectionResponse;
import com.capstone.contractmanagement.responses.template.TemplateResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemplateService implements ITemplateService {
    private final ITemplateRepository templateRepository;

    @Override
    @Transactional
    public TemplateResponse createTemplate(Template template) {
        Template savedTemplate = templateRepository.save(template);
        return mapToTemplateResponse(savedTemplate);
    }

    @Override
    @Transactional
    public List<TemplateResponse> getAllTemplates() {
        List<Template> templates = templateRepository.findAll();
        return templates.stream()
                .map(this::mapToTemplateResponse)
                .collect(Collectors.toList());
    }

    private TemplateResponse mapToTemplateResponse(Template template) {
        // Chuyển đổi các Section liên quan sang SectionResponse
        List<SectionResponse> sectionResponses = template.getSections().stream()
                .map(section -> SectionResponse.builder()
                        .id(section.getId())
                        .sectionName(section.getSectionName())
                        .order(section.getOrder())
                        .build())
                .collect(Collectors.toList());

        return TemplateResponse.builder()
                .id(template.getId())
                .templateName(template.getTemplateName())
                .title(template.getTitle())
                .content(template.getContent())
                .createdBy(template.getCreatedBy())
                .sections(sectionResponses)
                .build();
    }
}

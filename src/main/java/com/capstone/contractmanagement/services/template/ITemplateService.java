package com.capstone.contractmanagement.services.template;

import com.capstone.contractmanagement.entities.template.Template;
import com.capstone.contractmanagement.responses.template.TemplateResponse;

import java.util.List;

public interface ITemplateService {
    TemplateResponse createTemplate(Template template);
    List<TemplateResponse> getAllTemplates();
}

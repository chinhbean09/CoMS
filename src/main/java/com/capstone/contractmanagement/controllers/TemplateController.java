package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.entities.template.Template;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.responses.template.TemplateResponse;
import com.capstone.contractmanagement.services.template.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @PostMapping("/create")
    public ResponseObject createTemplate(@RequestBody Template template) {
        TemplateResponse response = templateService.createTemplate(template);
        return ResponseObject.builder()
                .status(HttpStatus.CREATED)
                .message("Created template successfully")
                .build();
    }

    @GetMapping("/get-all")
    public ResponseEntity<List<TemplateResponse>> getAllTemplates() {
        List<TemplateResponse> responses = templateService.getAllTemplates();
        return ResponseEntity.ok(responses);
    }

}

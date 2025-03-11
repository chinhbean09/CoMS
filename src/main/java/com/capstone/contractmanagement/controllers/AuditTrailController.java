package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.repositories.IAuditTrailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/audit-trails")
@RequiredArgsConstructor
public class AuditTrailController {

    private final IAuditTrailRepository auditTrailRepository;



}

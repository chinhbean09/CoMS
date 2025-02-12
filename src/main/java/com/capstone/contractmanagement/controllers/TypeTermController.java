package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.dtos.term.CreateTypeTermDTO;
import com.capstone.contractmanagement.services.typeterm.TypeTermService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/type-terms")
@RequiredArgsConstructor
public class TypeTermController {

    private final TypeTermService typeTermService;

    // create type term
    @PostMapping("/create")
    public ResponseEntity<String> createTypeTerm(@RequestBody CreateTypeTermDTO request) {
        return ResponseEntity.ok(typeTermService.createTypeTerm(request));
    }
}

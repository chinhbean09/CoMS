package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.entities.AppConfig;
import com.capstone.contractmanagement.services.app_config.AppConfigService;
import com.capstone.contractmanagement.services.app_config.IAppConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/${api.prefix}/config")
@RequiredArgsConstructor
public class AppConfigController {
    private final IAppConfigService appConfigService;


    @GetMapping("/{key}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<String> getConfig(@PathVariable String key) {
        return ResponseEntity.ok(appConfigService.getConfigValue(key));
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<AppConfig> updateConfig(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam(required = false) String description
    ) {
        return ResponseEntity.ok(appConfigService.createOrUpdateConfig(key, value, description));
    }

    @DeleteMapping("/{key}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> deleteConfig(@PathVariable String key) {
        appConfigService.deleteConfig(key);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/get-all")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<AppConfig>> getAllConfigs() {
        return ResponseEntity.ok(appConfigService.getAllConfigs());
    }

    @GetMapping("/get-all-paging")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Page<AppConfig>> getAllConfigsPaging(
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "key",
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    ) {
        return ResponseEntity.ok(appConfigService.getAllConfigsPaging(pageable));
    }

}

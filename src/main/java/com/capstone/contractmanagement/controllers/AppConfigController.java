package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.dtos.appconfig.AppConfigDTO;
import com.capstone.contractmanagement.entities.AppConfig;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.services.app_config.AppConfigService;
import com.capstone.contractmanagement.services.app_config.IAppConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/configs")
@RequiredArgsConstructor
public class AppConfigController {
    private final IAppConfigService appConfigService;


    @GetMapping("/{key}")
    //@PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<String> getConfig(@PathVariable String key) {
        return ResponseEntity.ok(appConfigService.getConfigValue(key));
    }

    @PostMapping("/update/{configId}")
    public ResponseEntity<ResponseObject> updateAppConfig(@PathVariable Long configId, @RequestBody AppConfigDTO appConfigDTO) throws DataNotFoundException {
        appConfigService.updateConfig(configId, appConfigDTO);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Cập nhật cấu hình thành công")
                .data(null)
                .build());
    }

    @DeleteMapping("/delete/{configId}")
    //@PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ResponseObject> deleteConfig(@PathVariable Long configId) throws DataNotFoundException {
        appConfigService.deleteConfig(configId);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Xoá cấu hình thành công")
                .data(null)
                .build());
    }

    @GetMapping("/get-all")
    //@PreAuthorize("hasRole('MANAGER')")
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

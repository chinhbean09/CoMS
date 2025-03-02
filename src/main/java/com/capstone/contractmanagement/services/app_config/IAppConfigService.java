package com.capstone.contractmanagement.services.app_config;

import com.capstone.contractmanagement.entities.AppConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IAppConfigService {

    AppConfig createOrUpdateConfig(String key, String value, String description);

    void deleteConfig(String key);

    String getConfigValue(String key);

    List<AppConfig> getAllConfigs();

    Page<AppConfig> getAllConfigsPaging (Pageable pageable);
}

package com.capstone.contractmanagement.services.app_config;

import com.capstone.contractmanagement.entities.AppConfig;
import com.capstone.contractmanagement.repositories.IAppConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AppConfigService implements  IAppConfigService{
    private final IAppConfigRepository appConfigRepository;

    @Override
    public AppConfig createOrUpdateConfig(String key, String value, String description) {
        Optional<AppConfig> existingConfig = appConfigRepository.findByKey(key);
        if (existingConfig.isPresent()) {
            AppConfig config = existingConfig.get();
            config.setValue(value);
            config.setDescription(description);
            return appConfigRepository.save(config);
        }
        return appConfigRepository.save(AppConfig.builder()
                .key(key)
                .value(value)
                .description(description)
                .build());
    }

    @Override
    public void deleteConfig(String key) {
        appConfigRepository.deleteByKey(key);
    }

    @Override
    // Cho phép tất cả user đọc config nhưng chỉ Manager được sửa
    public String getConfigValue(String key) {
        return appConfigRepository.findByKey(key)
                .map(AppConfig::getValue)
                .orElse(null);
    }

    @Override
    public List<AppConfig> getAllConfigs() {
        return appConfigRepository.findAll();
    }

    @Override
    public Page<AppConfig> getAllConfigsPaging (Pageable pageable) {
        return appConfigRepository.findAll(pageable);
    }
}

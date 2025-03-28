package com.capstone.contractmanagement.services.app_config;

import com.capstone.contractmanagement.dtos.appconfig.AppConfigDTO;
import com.capstone.contractmanagement.entities.AppConfig;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.IAppConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppConfigService implements  IAppConfigService{
    private final IAppConfigRepository appConfigRepository;


    @Override
    public void updateConfig(Long id, AppConfigDTO appConfigDTO) throws DataNotFoundException {
        AppConfig appConfig = appConfigRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Config not found"));

        appConfig.setValue(appConfigDTO.getValue());
        appConfig.setDescription(appConfigDTO.getDescription());
        appConfigRepository.save(appConfig);
    }

    @Override
    public void deleteConfig(Long id) throws DataNotFoundException {
        AppConfig appConfig = appConfigRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Config not found"));
        appConfigRepository.delete(appConfig);

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

    @Override
    public int getPaymentDeadlineValue() {
        AppConfig appConfig = appConfigRepository.findByKey("PAYMENT_DEADLINE").orElse(null);
        assert appConfig != null;
        return Integer.parseInt(appConfig.getValue());
    }

    @Override
    public int getApprovalDeadlineValue() {
        AppConfig appConfig = appConfigRepository.findByKey("APPROVAL_DEADLINE").orElse(null);
        assert appConfig != null;
        return Integer.parseInt(appConfig.getValue());
    }
}

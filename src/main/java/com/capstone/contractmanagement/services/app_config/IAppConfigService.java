package com.capstone.contractmanagement.services.app_config;

import com.capstone.contractmanagement.dtos.appconfig.AppConfigDTO;
import com.capstone.contractmanagement.entities.AppConfig;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IAppConfigService {

    void updateConfig(Long id, AppConfigDTO appConfigDTO) throws DataNotFoundException;

    void deleteConfig(Long id) throws DataNotFoundException;

    String getConfigValue(String key);

    List<AppConfig> getAllConfigs();

    Page<AppConfig> getAllConfigsPaging (Pageable pageable);

    int getPaymentDeadlineValue();
    int getApprovalDeadlineValue();
}

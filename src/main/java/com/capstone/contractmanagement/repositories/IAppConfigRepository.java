package com.capstone.contractmanagement.repositories;
import com.capstone.contractmanagement.entities.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IAppConfigRepository extends JpaRepository<AppConfig, Long> {
    Optional<AppConfig> findByKey(String key);

    void deleteByKey(String key);

}
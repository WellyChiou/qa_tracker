package com.example.helloworld.service.church;

import com.example.helloworld.entity.church.PositionConfig;
import com.example.helloworld.repository.church.PositionConfigRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PositionConfigService {

    @Autowired
    private PositionConfigRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 獲取默認配置
     */
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public Optional<Object> getDefaultConfig() {
        Optional<PositionConfig> config = repository.findByIsDefaultTrue();
        if (config.isEmpty()) {
            // 如果沒有默認配置，嘗試獲取名為 'default' 的配置
            config = repository.findByConfigName("default");
        }
        
        if (config.isPresent()) {
            try {
                Object configData = objectMapper.readValue(config.get().getConfigData(), Object.class);
                return Optional.of(configData);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("無法解析配置數據", e);
            }
        }
        
        return Optional.empty();
    }

    /**
     * 根據配置名稱獲取配置
     */
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public Optional<Object> getConfigByName(String configName) {
        Optional<PositionConfig> config = repository.findByConfigName(configName);
        if (config.isPresent()) {
            try {
                Object configData = objectMapper.readValue(config.get().getConfigData(), Object.class);
                return Optional.of(configData);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("無法解析配置數據", e);
            }
        }
        return Optional.empty();
    }

    /**
     * 保存或更新配置
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public PositionConfig saveConfig(String configName, Object configData, Boolean isDefault) {
        Optional<PositionConfig> existing = repository.findByConfigName(configName);
        
        PositionConfig config;
        if (existing.isPresent()) {
            config = existing.get();
        } else {
            config = new PositionConfig();
            config.setConfigName(configName);
        }
        
        try {
            config.setConfigData(objectMapper.writeValueAsString(configData));
            config.setIsDefault(isDefault != null ? isDefault : false);
            
            // 如果設置為默認配置，將其他配置的 isDefault 設為 false
            if (config.getIsDefault()) {
                repository.findAll().forEach(c -> {
                    if (!c.getId().equals(config.getId())) {
                        c.setIsDefault(false);
                        repository.save(c);
                    }
                });
            }
            
            return repository.save(config);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("無法序列化配置數據", e);
        }
    }

    /**
     * 保存默認配置
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public PositionConfig saveDefaultConfig(Object configData) {
        return saveConfig("default", configData, true);
    }
}


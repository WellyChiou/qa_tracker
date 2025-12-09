package com.example.helloworld.service.personal;

import com.example.helloworld.entity.personal.Config;
import com.example.helloworld.repository.personal.ConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ConfigService {
    @Autowired
    private ConfigRepository configRepository;

    public String getConfigValue(String configKey) {
        Optional<Config> config = configRepository.findByConfigKey(configKey);
        return config.map(Config::getConfigValue).orElse(null);
    }

    public Config saveConfig(String configKey, String configValue, String description) {
        Config config = configRepository.findByConfigKey(configKey)
                .orElse(new Config());
        config.setConfigKey(configKey);
        config.setConfigValue(configValue);
        if (description != null) {
            config.setDescription(description);
        }
        return configRepository.save(config);
    }
}

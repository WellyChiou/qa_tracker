package com.example.helloworld.service.personal;

import com.example.helloworld.entity.personal.SystemSetting;
import com.example.helloworld.repository.personal.SystemSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service("personalSystemSettingService")
public class SystemSettingService {

    @Autowired
    @Qualifier("personalSystemSettingRepository")
    private SystemSettingRepository systemSettingRepository;

    @Transactional(readOnly = true)
    public List<SystemSetting> getAllSettings() {
        return systemSettingRepository.findAllOrderByCategoryAndKey();
    }

    @Transactional(readOnly = true)
    public List<SystemSetting> getSettingsByCategory(String category) {
        return systemSettingRepository.findByCategory(category);
    }

    @Transactional(readOnly = true)
    public Optional<SystemSetting> getSettingByKey(String key) {
        return systemSettingRepository.findBySettingKey(key);
    }

    @Transactional(readOnly = true)
    public String getSettingValue(String key, String defaultValue) {
        return systemSettingRepository.findBySettingKey(key)
            .map(SystemSetting::getSettingValue)
            .orElse(defaultValue);
    }

    @Transactional(readOnly = true)
    public int getSettingValueAsInt(String key, int defaultValue) {
        String value = getSettingValue(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Transactional(readOnly = true)
    public boolean getSettingValueAsBoolean(String key, boolean defaultValue) {
        String value = getSettingValue(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

    @Transactional
    public SystemSetting updateSetting(String key, SystemSetting settingUpdate) {
        SystemSetting existing = systemSettingRepository.findBySettingKey(key)
            .orElseThrow(() -> new RuntimeException("系統參數不存在: " + key));

        if (!existing.getIsEditable()) {
            throw new RuntimeException("此參數不可編輯: " + key);
        }

        if (settingUpdate.getSettingValue() != null) {
            existing.setSettingValue(settingUpdate.getSettingValue());
        }
        if (settingUpdate.getDescription() != null) {
            existing.setDescription(settingUpdate.getDescription());
        }

        return systemSettingRepository.save(existing);
    }

    @Transactional
    public SystemSetting createSetting(SystemSetting setting) {
        if (systemSettingRepository.existsBySettingKey(setting.getSettingKey())) {
            throw new RuntimeException("系統參數已存在: " + setting.getSettingKey());
        }
        return systemSettingRepository.save(setting);
    }
}


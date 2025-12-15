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

<<<<<<< HEAD
    @Transactional(readOnly = true)
=======
    @Transactional(readOnly = true, transactionManager = "primaryTransactionManager")
>>>>>>> 45b7fd36d7e04bf5e2b8c79b7542d7cec8adf2d1
    public List<SystemSetting> getAllSettings() {
        return systemSettingRepository.findAllOrderByCategoryAndKey();
    }

<<<<<<< HEAD
    @Transactional(readOnly = true)
=======
    @Transactional(readOnly = true, transactionManager = "primaryTransactionManager")
>>>>>>> 45b7fd36d7e04bf5e2b8c79b7542d7cec8adf2d1
    public List<SystemSetting> getSettingsByCategory(String category) {
        return systemSettingRepository.findByCategory(category);
    }

<<<<<<< HEAD
    @Transactional(readOnly = true)
=======
    @Transactional(readOnly = true, transactionManager = "primaryTransactionManager")
>>>>>>> 45b7fd36d7e04bf5e2b8c79b7542d7cec8adf2d1
    public Optional<SystemSetting> getSettingByKey(String key) {
        return systemSettingRepository.findBySettingKey(key);
    }

<<<<<<< HEAD
    @Transactional(readOnly = true)
=======
    @Transactional(readOnly = true, transactionManager = "primaryTransactionManager")
>>>>>>> 45b7fd36d7e04bf5e2b8c79b7542d7cec8adf2d1
    public String getSettingValue(String key, String defaultValue) {
        return systemSettingRepository.findBySettingKey(key)
            .map(SystemSetting::getSettingValue)
            .orElse(defaultValue);
    }

<<<<<<< HEAD
    @Transactional(readOnly = true)
=======
    @Transactional(readOnly = true, transactionManager = "primaryTransactionManager")
>>>>>>> 45b7fd36d7e04bf5e2b8c79b7542d7cec8adf2d1
    public int getSettingValueAsInt(String key, int defaultValue) {
        String value = getSettingValue(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

<<<<<<< HEAD
    @Transactional(readOnly = true)
=======
    @Transactional(readOnly = true, transactionManager = "primaryTransactionManager")
>>>>>>> 45b7fd36d7e04bf5e2b8c79b7542d7cec8adf2d1
    public boolean getSettingValueAsBoolean(String key, boolean defaultValue) {
        String value = getSettingValue(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

<<<<<<< HEAD
    @Transactional
=======
    @Transactional(readOnly = true, transactionManager = "primaryTransactionManager")
    public long getSettingValueAsLong(String key, long defaultValue) {
        String value = getSettingValue(key, String.valueOf(defaultValue));
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Transactional(transactionManager = "primaryTransactionManager")
>>>>>>> 45b7fd36d7e04bf5e2b8c79b7542d7cec8adf2d1
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

<<<<<<< HEAD
    @Transactional
=======
    @Transactional(transactionManager = "primaryTransactionManager")
>>>>>>> 45b7fd36d7e04bf5e2b8c79b7542d7cec8adf2d1
    public SystemSetting createSetting(SystemSetting setting) {
        if (systemSettingRepository.existsBySettingKey(setting.getSettingKey())) {
            throw new RuntimeException("系統參數已存在: " + setting.getSettingKey());
        }
        return systemSettingRepository.save(setting);
    }
<<<<<<< HEAD
=======

    @Transactional(transactionManager = "primaryTransactionManager")
    public void deleteSetting(String key) {
        SystemSetting setting = systemSettingRepository.findBySettingKey(key)
            .orElseThrow(() -> new RuntimeException("系統參數不存在: " + key));
        
        // 檢查是否可刪除（某些系統關鍵參數不應被刪除）
        if (!setting.getIsEditable()) {
            throw new RuntimeException("此參數不可刪除: " + key);
        }
        
        systemSettingRepository.delete(setting);
    }
>>>>>>> 45b7fd36d7e04bf5e2b8c79b7542d7cec8adf2d1
}


package com.example.helloworld.controller.church;

import com.example.helloworld.entity.church.PositionConfig;
import com.example.helloworld.service.church.PositionConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/church/position-config")
@CrossOrigin(origins = "*")
public class PositionConfigController {

    @Autowired
    private PositionConfigService service;

    /**
     * 獲取默認配置
     */
    @GetMapping("/default")
    public ResponseEntity<Map<String, Object>> getDefaultConfig() {
        try {
            return service.getDefaultConfig()
                .map(configData -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("config", configData);
                    response.put("message", "獲取配置成功");
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.ok(Map.of(
                    "config", Map.of(),
                    "message", "未找到默認配置"
                )));
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取配置失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 根據配置名稱獲取配置
     */
    @GetMapping("/{configName}")
    public ResponseEntity<Map<String, Object>> getConfigByName(@PathVariable String configName) {
        try {
            return service.getConfigByName(configName)
                .map(configData -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("config", configData);
                    response.put("message", "獲取配置成功");
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.ok(Map.of(
                    "config", Map.of(),
                    "message", "未找到配置：" + configName
                )));
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取配置失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 保存默認配置
     */
    @PostMapping("/default")
    public ResponseEntity<Map<String, Object>> saveDefaultConfig(@RequestBody Map<String, Object> request) {
        try {
            Object configData = request.get("config");
            if (configData == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "配置數據不能為空");
                return ResponseEntity.badRequest().body(error);
            }

            PositionConfig saved = service.saveDefaultConfig(configData);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", saved.getId());
            response.put("configName", saved.getConfigName());
            response.put("isDefault", saved.getIsDefault());
            response.put("message", "配置保存成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "保存配置失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 保存或更新配置
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> saveConfig(@RequestBody Map<String, Object> request) {
        try {
            String configName = (String) request.getOrDefault("configName", "default");
            Object configData = request.get("config");
            Boolean isDefault = request.get("isDefault") != null ? 
                Boolean.parseBoolean(request.get("isDefault").toString()) : false;

            if (configData == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "配置數據不能為空");
                return ResponseEntity.badRequest().body(error);
            }

            PositionConfig saved = service.saveConfig(configName, configData, isDefault);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", saved.getId());
            response.put("configName", saved.getConfigName());
            response.put("isDefault", saved.getIsDefault());
            response.put("message", "配置保存成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "保存配置失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}


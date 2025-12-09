package com.example.helloworld.controller.personal;

import com.example.helloworld.service.personal.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = "*")
public class ConfigController {
    @Autowired
    private ConfigService configService;

    @GetMapping("/{configKey}")
    public ResponseEntity<Map<String, String>> getConfig(@PathVariable String configKey) {
        String value = configService.getConfigValue(configKey);
        Map<String, String> response = new HashMap<>();
        if (value != null) {
            response.put("value", value);
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{configKey}")
    public ResponseEntity<Map<String, String>> saveConfig(
            @PathVariable String configKey,
            @RequestBody Map<String, String> request) {
        String value = request.get("value");
        String description = request.get("description");
        configService.saveConfig(configKey, value, description);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Config saved successfully");
        return ResponseEntity.ok(response);
    }
}

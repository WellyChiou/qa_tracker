package com.example.helloworld.service.church;

import com.example.helloworld.config.LineBotConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 教會後台 LINE Bot 服務
 * 
 * 用於教會後台的 LINE 群組通知功能。
 * 注意：教會後台不需要綁定個人 LINE ID，直接透過群組 ID 發送通知。
 */
@Service
public class ChurchLineBotService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private LineBotConfig lineBotConfig;

    /**
     * 發送群組訊息
     * 
     * 使用 LINE Bot API 的 Push Message API 直接發送到群組。
     * 注意：Bot 必須已經加入該群組才能發送訊息。
     * 
     * @param groupId 群組 ID
     * @param message 訊息內容
     */
    public void sendGroupMessage(String groupId, String message) {
        try {
            if (groupId == null || groupId.trim().isEmpty()) {
                log.error("❌ [教會群組通知] 群組 ID 為空，無法發送群組訊息");
                return;
            }

            log.info("📤 [教會群組通知] 準備發送訊息到群組: {}", groupId);
            log.info("📝 [教會群組通知] 訊息內容預覽: {}", message.length() > 100 ? message.substring(0, 100) + "..." : message);

            String url = "https://api.line.me/v2/bot/message/push";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(lineBotConfig.getChannelToken());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("to", groupId); // 使用群組 ID 作為接收者
            requestBody.put("messages", new Object[]{
                Map.of("type", "text", "text", message)
            });

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [教會群組通知] 已成功發送訊息到群組: {}", groupId);
                log.info("✅ [教會群組通知] 響應狀態: {}", response.getStatusCode());
            } else {
                log.error("❌ [教會群組通知] 發送群組訊息失敗，狀態碼: {}", response.getStatusCode());
                log.error("❌ [教會群組通知] 響應內容: {}", response.getBody());
                log.error("💡 [教會群組通知] 提示：請確認 Bot 已經加入該群組");
            }

        } catch (Exception e) {
            log.error("❌ [教會群組通知] 發送群組訊息失敗: {}", e.getMessage(), e);
        }
    }
}


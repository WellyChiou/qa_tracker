package com.example.helloworld.service.invest.system;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InvestLineTestService {

    private static final String LINE_PUSH_API = "https://api.line.me/v2/bot/message/push";

    private final InvestSystemSettingService investSystemSettingService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public InvestLineTestService(InvestSystemSettingService investSystemSettingService, ObjectMapper objectMapper) {
        this.investSystemSettingService = investSystemSettingService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public Map<String, Object> sendGroupPush(String groupId, String message) {
        if (groupId == null || groupId.trim().isEmpty()) {
            throw new RuntimeException("groupId 不可為空");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new RuntimeException("message 不可為空");
        }

        String channelToken = investSystemSettingService.getSettingValue("line.bot.channel-token", "");
        if (channelToken == null || channelToken.trim().isEmpty()) {
            throw new RuntimeException("LINE Channel Token 尚未設定，請先於系統維護頁設定 line.bot.channel-token");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("to", groupId.trim());
        payload.put("messages", List.of(Map.of(
            "type", "text",
            "text", message.trim()
        )));

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("建立 LINE 請求資料失敗：" + e.getMessage(), e);
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(LINE_PUSH_API))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + channelToken.trim())
            .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            String responseBody = response.body();

            if (statusCode < 200 || statusCode >= 300) {
                throw new RuntimeException("LINE 測試訊息發送失敗，HTTP " + statusCode + "，回應：" + responseBody);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("status", "SUCCESS");
            result.put("statusCode", statusCode);
            result.put("groupId", groupId.trim());
            result.put("message", "LINE 測試訊息發送成功");
            result.put("response", parseJsonOrRaw(responseBody));
            return result;
        } catch (IOException e) {
            throw new RuntimeException("呼叫 LINE API 失敗：" + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("LINE 測試訊息發送被中斷", e);
        }
    }

    private Object parseJsonOrRaw(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ignore) {
            return raw;
        }
    }
}

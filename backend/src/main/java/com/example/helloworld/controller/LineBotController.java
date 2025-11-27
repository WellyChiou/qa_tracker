package com.example.helloworld.controller;

import com.example.helloworld.service.LineBotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/line")
@CrossOrigin(origins = "*")
public class LineBotController {

    @Autowired
    private LineBotService lineBotService;

    /**
     * LINE Bot Webhook 端點
     * 處理來自 LINE 平台的所有事件
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object> webhookRequest) {
        try {
            System.out.println("📨 收到 LINE webhook 請求");

            // 處理事件列表
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> events = (List<Map<String, Object>>) webhookRequest.get("events");

            if (events != null) {
                System.out.println("事件數量: " + events.size());

                for (Map<String, Object> event : events) {
                    handleEvent(event);
                }
            }

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            System.err.println("❌ 處理 LINE webhook 時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("ERROR");
        }
    }

    /**
     * 處理單個事件
     */
    private void handleEvent(Map<String, Object> event) {
        try {
            String type = (String) event.get("type");
            System.out.println("🎯 處理事件類型: " + type);

            // 處理文字訊息事件
            if ("message".equals(type)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) event.get("message");

                if (message != null && "text".equals(message.get("type"))) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> source = (Map<String, Object>) event.get("source");
                    String replyToken = (String) event.get("replyToken");
                    String userId = (String) source.get("userId");
                    String text = (String) message.get("text");

                    lineBotService.handleMessageEvent(replyToken, userId, text);
                } else {
                    System.out.println("⚠️ 收到非文字訊息事件，忽略處理");
                }
            } else {
                System.out.println("⚠️ 收到非訊息事件，類型: " + type);
            }

        } catch (Exception e) {
            System.err.println("❌ 處理事件時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 測試端點 - 用於驗證 LINE Bot 配置
     */
    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("LINE Bot webhook is active");
    }

    /**
     * 手動發送推播訊息測試端點
     * 支持兩種方式：
     * 1. URL 查詢參數: /api/line/test/push?userId=xxx&message=xxx
     * 2. JSON body: { "userId": "xxx", "message": "xxx" }
     */
    @PostMapping("/test/push")
    public ResponseEntity<Map<String, Object>> testPushMessage(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String message,
            @RequestBody(required = false) Map<String, String> body) {

        try {
            // 優先使用 body 中的參數，如果沒有則使用查詢參數
            String finalUserId = (body != null && body.containsKey("userId")) 
                ? body.get("userId") 
                : userId;
            String finalMessage = (body != null && body.containsKey("message")) 
                ? body.get("message") 
                : message;

            if (finalUserId == null || finalUserId.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "userId 參數不能為空");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            if (finalMessage == null || finalMessage.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "message 參數不能為空");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            lineBotService.sendPushMessage(finalUserId, finalMessage);
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("message", "推播訊息已發送");
            return ResponseEntity.ok(successResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "發送失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}

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
            if (event == null) {
                System.err.println("⚠️ 收到空事件，跳過處理");
                return;
            }

            String type = (String) event.get("type");
            if (type == null) {
                System.err.println("⚠️ 事件缺少 type 欄位，跳過處理");
                return;
            }

            System.out.println("🎯 處理事件類型: " + type);

            @SuppressWarnings("unchecked")
            Map<String, Object> source = (Map<String, Object>) event.get("source");
            String sourceType = source != null ? (String) source.get("type") : null;

            // 處理群組加入事件
            if ("join".equals(type)) {
                if (source == null) {
                    System.err.println("⚠️ join 事件缺少 source 欄位");
                    return;
                }
                if ("group".equals(sourceType)) {
                    String groupId = (String) source.get("groupId");
                    if (groupId != null && !groupId.trim().isEmpty()) {
                        lineBotService.handleGroupJoinEvent(groupId);
                    } else {
                        System.err.println("⚠️ 群組 join 事件缺少 groupId");
                    }
                } else if ("room".equals(sourceType)) {
                    String roomId = (String) source.get("roomId");
                    System.out.println("📥 Bot 被加入到聊天室: " + roomId);
                    // 聊天室功能可以後續擴展
                } else {
                    System.out.println("⚠️ 未知的 join 事件來源類型: " + sourceType);
                }
            }
            // 處理群組離開事件
            else if ("leave".equals(type)) {
                if (source == null) {
                    System.err.println("⚠️ leave 事件缺少 source 欄位");
                    return;
                }
                if ("group".equals(sourceType)) {
                    String groupId = (String) source.get("groupId");
                    if (groupId != null && !groupId.trim().isEmpty()) {
                        lineBotService.handleGroupLeaveEvent(groupId);
                    } else {
                        System.err.println("⚠️ 群組 leave 事件缺少 groupId");
                    }
                } else {
                    System.out.println("⚠️ 未知的 leave 事件來源類型: " + sourceType);
                }
            }
            // 處理文字訊息事件
            else if ("message".equals(type)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) event.get("message");

                if (message != null && "text".equals(message.get("type"))) {
                    String replyToken = (String) event.get("replyToken");
                    String text = (String) message.get("text");

                    if (source == null) {
                        System.err.println("⚠️ message 事件缺少 source 欄位");
                        return;
                    }

                    // 判斷是個人訊息還是群組訊息
                    if ("group".equals(sourceType)) {
                        String groupId = (String) source.get("groupId");
                        String userId = (String) source.get("userId");
                            System.out.println("📨 [Webhook] 收到群組訊息事件");
                        System.out.println("📨 [Webhook] Group ID: " + groupId);
                        System.out.println("📨 [Webhook] User ID: " + userId);
                        System.out.println("📨 [Webhook] Message: " + text);
                        
                        if (groupId != null && userId != null) {
                            lineBotService.handleGroupMessageEvent(replyToken, groupId, userId, text);
                        } else {
                            System.err.println("⚠️ [Webhook] 群組訊息缺少必要欄位 - groupId: " + groupId + ", userId: " + userId);
                        }
                    } else if ("user".equals(sourceType)) {
                        String userId = (String) source.get("userId");
                        System.out.println("📨 [Webhook] 收到個人訊息事件，User ID: " + userId);
                        if (userId != null) {
                            lineBotService.handleMessageEvent(replyToken, userId, text);
                        } else {
                            System.err.println("⚠️ [Webhook] 個人訊息缺少 userId");
                        }
                    }
                } else {
                    System.out.println("⚠️ 收到非文字訊息事件，忽略處理");
                }
            } else {
                System.out.println("⚠️ 收到未處理的事件類型: " + type);
            }

        } catch (Exception e) {
            // 記錄錯誤但不拋出異常，確保 webhook 始終返回 200 OK
            System.err.println("❌ 處理事件時發生錯誤: " + e.getMessage());
            System.err.println("錯誤類型: " + e.getClass().getName());
            e.printStackTrace();
            // 不重新拋出異常，避免影響 webhook 響應
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

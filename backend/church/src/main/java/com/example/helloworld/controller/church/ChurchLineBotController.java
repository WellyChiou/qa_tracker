package com.example.helloworld.controller.church;

import com.example.helloworld.service.line.LineBotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/church/line")
@CrossOrigin(origins = "*")
public class ChurchLineBotController {
    private static final Logger log = LoggerFactory.getLogger(ChurchLineBotController.class);

    private final LineBotService lineBotService;

    public ChurchLineBotController(LineBotService lineBotService) {
        this.lineBotService = lineBotService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object> webhookRequest) {
        try {
            log.info("📨 收到 Church LINE webhook 請求");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> events = (List<Map<String, Object>>) webhookRequest.get("events");

            if (events != null) {
                log.info("Church 事件數量: {}", events.size());
                for (Map<String, Object> event : events) {
                    handleEvent(event);
                }
            }

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("❌ 處理 Church LINE webhook 時發生錯誤: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("ERROR");
        }
    }

    private void handleEvent(Map<String, Object> event) {
        try {
            if (event == null) {
                log.warn("⚠️ 收到空事件，跳過處理");
                return;
            }

            String type = (String) event.get("type");
            if (type == null) {
                log.warn("⚠️ 事件缺少 type 欄位，跳過處理");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> source = (Map<String, Object>) event.get("source");
            String sourceType = source != null ? (String) source.get("type") : null;

            if ("join".equals(type) && "group".equals(sourceType) && source != null) {
                String groupId = (String) source.get("groupId");
                if (groupId != null && !groupId.trim().isEmpty()) {
                    lineBotService.handleGroupJoinEvent(groupId);
                }
                return;
            }

            if ("leave".equals(type) && "group".equals(sourceType) && source != null) {
                String groupId = (String) source.get("groupId");
                if (groupId != null && !groupId.trim().isEmpty()) {
                    lineBotService.handleGroupLeaveEvent(groupId);
                }
                return;
            }

            if (!"message".equals(type)) {
                log.info("ℹ️ Church webhook 略過未處理事件類型: {}", type);
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) event.get("message");
            if (message == null || !"text".equals(message.get("type"))) {
                log.info("ℹ️ Church webhook 略過非文字訊息");
                return;
            }

            String replyToken = (String) event.get("replyToken");
            String text = (String) message.get("text");
            if (source == null) {
                log.warn("⚠️ Church message 事件缺少 source 欄位");
                return;
            }

            if ("group".equals(sourceType)) {
                String groupId = (String) source.get("groupId");
                String userId = (String) source.get("userId");
                if (groupId != null && userId != null) {
                    lineBotService.handleGroupMessageEvent(replyToken, groupId, userId, text);
                } else {
                    log.warn("⚠️ Church 群組訊息缺少必要欄位 - groupId: {}, userId: {}", groupId, userId);
                }
                return;
            }

            if ("user".equals(sourceType)) {
                String userId = (String) source.get("userId");
                if (userId != null) {
                    lineBotService.handleMessageEvent(replyToken, userId, text);
                } else {
                    log.warn("⚠️ Church 個人訊息缺少 userId");
                }
            }
        } catch (Exception e) {
            log.error("❌ 處理 Church webhook 事件時發生錯誤: {}", e.getMessage(), e);
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Church LINE Bot webhook is active");
    }

    @PostMapping("/test/group-push")
    public ResponseEntity<Map<String, Object>> testGroupPushMessage(@RequestBody Map<String, String> body) {
        try {
            String groupId = body.get("groupId");
            String message = body.get("message");

            if (groupId == null || groupId.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "groupId 參數不能為空");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            if (message == null || message.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "message 參數不能為空");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            lineBotService.sendPushMessage(groupId, message);

            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("message", "群組測試訊息已發送");
            return ResponseEntity.ok(successResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "發送失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}

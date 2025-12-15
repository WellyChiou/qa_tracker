package com.example.helloworld.controller.personal;

import com.example.helloworld.config.LineBotConfig;
import com.example.helloworld.repository.church.ChurchLineGroupRepository;
import com.example.helloworld.service.personal.LineBotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/line")
@CrossOrigin(origins = "*")
public class LineBotController {
    
    private static final Logger log = LoggerFactory.getLogger(LineBotController.class);

    @Autowired
    private LineBotService lineBotService;

    @Autowired
    private LineBotConfig lineBotConfig;

    @Autowired
    private ChurchLineGroupRepository churchLineGroupRepository;

    /**
     * LINE Bot Webhook 端點
     * 處理來自 LINE 平台的所有事件
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object> webhookRequest) {
        try {
            log.info("📨 收到 LINE webhook 請求");

            // 處理事件列表
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> events = (List<Map<String, Object>>) webhookRequest.get("events");

            if (events != null) {
                log.info("事件數量: {}", events.size());

                for (Map<String, Object> event : events) {
                    handleEvent(event);
                }
            }

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("❌ 處理 LINE webhook 時發生錯誤: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("ERROR");
        }
    }

    /**
     * 處理單個事件
     */
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

            log.info("🎯 處理事件類型: {}", type);

            @SuppressWarnings("unchecked")
            Map<String, Object> source = (Map<String, Object>) event.get("source");
            String sourceType = source != null ? (String) source.get("type") : null;

            // 處理群組加入事件
            if ("join".equals(type)) {
                if (source == null) {
                    log.warn("⚠️ join 事件缺少 source 欄位");
                    return;
                }
                if ("group".equals(sourceType)) {
                    String groupId = (String) source.get("groupId");
                    if (groupId != null && !groupId.trim().isEmpty()) {
                        // 檢查是否為教會群組，如果是則跳過 Personal 處理
                        if (isChurchGroup(groupId)) {
                            log.info("📥 [教會群組] Bot 加入教會群組，跳過 Personal 處理: {}", groupId);
                            return;
                        }
                        lineBotService.handleGroupJoinEvent(groupId);
                    } else {
                        log.warn("⚠️ 群組 join 事件缺少 groupId");
                    }
                } else if ("room".equals(sourceType)) {
                    String roomId = (String) source.get("roomId");
                    log.info("📥 Bot 被加入到聊天室: {}", roomId);
                    // 聊天室功能可以後續擴展
                } else {
                    log.warn("⚠️ 未知的 join 事件來源類型: {}", sourceType);
                }
            }
            // 處理群組離開事件
            else if ("leave".equals(type)) {
                if (source == null) {
                    log.warn("⚠️ leave 事件缺少 source 欄位");
                    return;
                }
                if ("group".equals(sourceType)) {
                    String groupId = (String) source.get("groupId");
                    if (groupId != null && !groupId.trim().isEmpty()) {
                        // 檢查是否為教會群組，如果是則跳過 Personal 處理
                        if (isChurchGroup(groupId)) {
                            log.info("📤 [教會群組] Bot 離開教會群組，跳過 Personal 處理: {}", groupId);
                            return;
                        }
                        lineBotService.handleGroupLeaveEvent(groupId);
                    } else {
                        log.warn("⚠️ 群組 leave 事件缺少 groupId");
                    }
                } else {
                    log.warn("⚠️ 未知的 leave 事件來源類型: {}", sourceType);
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
                        log.warn("⚠️ message 事件缺少 source 欄位");
                        return;
                    }

                    // 判斷是個人訊息還是群組訊息
                    if ("group".equals(sourceType)) {
                        String groupId = (String) source.get("groupId");
                        String userId = (String) source.get("userId");
                        log.info("📨 [Webhook] 收到群組訊息事件");
                        log.info("📨 [Webhook] Group ID: {}", groupId);
                        log.info("📨 [Webhook] User ID: {}", userId);
                        log.info("📨 [Webhook] Message: {}", text);
                        
                        if (groupId != null && userId != null) {
                            // 檢查是否為教會群組，如果是則跳過 Personal 處理
                            if (isChurchGroup(groupId)) {
                                log.info("📨 [教會群組] 收到教會群組訊息，跳過 Personal 處理: {}", groupId);
                                return;
                            }
                            lineBotService.handleGroupMessageEvent(replyToken, groupId, userId, text);
                        } else {
                            log.warn("⚠️ [Webhook] 群組訊息缺少必要欄位 - groupId: {}, userId: {}", groupId, userId);
                        }
                    } else if ("user".equals(sourceType)) {
                        String userId = (String) source.get("userId");
                        log.info("📨 [Webhook] 收到個人訊息事件，User ID: {}", userId);
                        if (userId != null) {
                            lineBotService.handleMessageEvent(replyToken, userId, text);
                        } else {
                            log.warn("⚠️ [Webhook] 個人訊息缺少 userId");
                        }
                    }
                } else {
                    log.warn("⚠️ 收到非文字訊息事件，忽略處理");
                }
            } else {
                log.warn("⚠️ 收到未處理的事件類型: {}", type);
            }

        } catch (Exception e) {
            // 記錄錯誤但不拋出異常，確保 webhook 始終返回 200 OK
            log.error("❌ 處理事件時發生錯誤: {}", e.getMessage(), e);
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

    /**
     * 判斷是否為教會群組
     * 
     * 檢查方式：
     * 1. 檢查是否等於配置的 church-group-id
     * 2. 檢查是否在 church.church_line_groups 資料表中
     * 
     * @param groupId LINE 群組 ID
     * @return true 如果是教會群組，false 如果是 Personal 群組
     */
    private boolean isChurchGroup(String groupId) {
        if (groupId == null || groupId.trim().isEmpty()) {
            return false;
        }

        try {
            // 方式1：檢查是否等於配置的教會群組 ID
            String churchGroupId = lineBotConfig.getChurchGroupId();
            if (churchGroupId != null && !churchGroupId.trim().isEmpty() && churchGroupId.equals(groupId)) {
                log.debug("✅ [群組判斷] 群組 {} 匹配配置的教會群組 ID", groupId);
                return true;
            }

            // 方式2：檢查是否在教會群組資料表中
            boolean existsInChurchDb = churchLineGroupRepository.findByGroupId(groupId).isPresent();
            if (existsInChurchDb) {
                log.debug("✅ [群組判斷] 群組 {} 存在於教會群組資料表", groupId);
                return true;
            }

            log.debug("ℹ️ [群組判斷] 群組 {} 不是教會群組，屬於 Personal 群組", groupId);
            return false;

        } catch (Exception e) {
            // 如果查詢失敗，為了安全起見，不當作教會群組處理
            log.warn("⚠️ [群組判斷] 檢查群組 {} 時發生錯誤，當作 Personal 群組處理: {}", groupId, e.getMessage());
            return false;
        }
    }
}

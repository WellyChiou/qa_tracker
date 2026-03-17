package com.example.helloworld.service.line;

import com.example.helloworld.dto.church.admin.ReplyResult;
import com.example.helloworld.dto.church.admin.ServiceUpdatePayload;
import com.example.helloworld.entity.church.LineGroup;
import com.example.helloworld.entity.church.LineGroupMember;
import com.example.helloworld.service.church.ChurchLineMessageService;
import com.example.helloworld.service.church.ChurchLineGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class LineBotService {
    private static final Logger log = LoggerFactory.getLogger(LineBotService.class);

    @Autowired
    private LineApiClient lineApiClient;

    @Autowired
    private ChurchLineMessageService churchLineMessageService;

    @Autowired
    private ChurchLineGroupService churchLineGroupService;

    // 服事更新格式：日期(yyyyMMdd),崗位,人員
    private static final Pattern SERVICE_UPDATE_PATTERN = Pattern.compile("^(\\d{8}),([^,]+),(.+)$");

    /**
     * 處理來自 LINE 的訊息事件（Church Bot 私訊）
     */
    public void handleMessageEvent(String replyToken, String userId, String messageText) {
        messageText = messageText.trim();
        log.info("📨 收到 LINE 訊息: {} 來自用戶: {}", messageText, userId);

        try {
            if (messageText.startsWith("C") && messageText.length() >= 30 && messageText.length() <= 40) {
                sendReplyMessage(replyToken, getGroupInfo(messageText));
                return;
            }

            sendReplyMessage(replyToken, getChurchHelpMessage());

        } catch (Exception e) {
            log.error("❌ 處理 LINE 訊息時發生錯誤", e);
            try {
                sendReplyMessage(replyToken, "❌ 處理訊息時發生錯誤，請稍後再試。");
            } catch (Exception replyError) {
                log.error("❌ 發送錯誤回覆失敗", replyError);
            }
        }
    }

    /**
     * 處理訊息內容
     */
    private String processMessage(String messageText, String replyToken, String groupCode, String groupId, String requesterId) {
        String trimmedMessage = messageText.trim();
        boolean isChurchTechControl = "CHURCH_TECH_CONTROL".equals(groupCode);
        if (isChurchTechControl) {
            Matcher serviceMatcher = SERVICE_UPDATE_PATTERN.matcher(trimmedMessage);
            if (serviceMatcher.find()) {
                return processServiceUpdateMessage(serviceMatcher).getText();
            }
        }

        if (trimmedMessage.startsWith("C") && trimmedMessage.length() >= 30 && trimmedMessage.length() <= 40) {
            return getGroupInfo(trimmedMessage);
        }

        switch (messageText.toLowerCase()) {
            case "help":
            case "幫助":
                if (isChurchTechControl) {
                    return getChurchHelpMessage();
                }
                break;

            case "本周服事表":
            case "本週服事表":
                if (isChurchTechControl) {
                    churchLineMessageService.triggerWeeklyServiceNotification(
                            requesterId,
                            groupId,
                            replyToken,
                            () -> sendGroupMessageByPush(groupId, "❌ 發送通知失敗，請稍後再試。")
                    );
                    return null;
                }
                break;

            default:
                return null;
        }

        return "❓ 無法識別的指令。請輸入 '幫助' 查看可用指令。";
    }

    private ReplyResult processServiceUpdateMessage(Matcher matcher) {
        return churchLineMessageService.processServiceUpdate(
                matcher.group(1),
                matcher.group(2).trim(),
                matcher.group(3).trim()
        );
    }

    /**
     * 處理費用記錄訊息（格式：類型 主類別 細項 金額 描述）
     */
    /**
     * 發送回覆訊息（純文字）
     */
    
    /**
     * 發送回覆訊息（純文字）
     */
    public void sendReplyMessage(String replyToken, String message) {
        try {
            if (replyToken == null || replyToken.trim().isEmpty()) {
                log.error("❌ [Reply] Reply Token 為空，無法發送回覆");
                return;
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("replyToken", replyToken);
            requestBody.put("messages", new Object[]{Map.of("type", "text", "text", message)});

            log.info("📤 [Reply] 準備發送回覆訊息，Reply Token: {}...", replyToken.substring(0, Math.min(20, replyToken.length())));
            log.info("📤 [Reply] 訊息內容: {}", (message != null && message.length() > 50 ? message.substring(0, 50) + "..." : message));

            ResponseEntity<String> response = lineApiClient.reply(requestBody);
            if (response == null) return;

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [Reply] 已成功發送回覆訊息");
                log.info("✅ [Reply] 響應狀態: {}", response.getStatusCode());
            } else {
                log.error("❌ [Reply] 發送回覆訊息失敗，狀態碼: {}", response.getStatusCode());
                log.error("❌ [Reply] 響應內容: {}", response.getBody());
            }
        } catch (Exception e) {
            log.error("❌ [Reply] 發送回覆訊息失敗", e);
        }
    }




    /**
     * ✅ Reply Flex 訊息（用於 webhook 即時回覆）
     */
    
    /**
     * ✅ Reply Flex 訊息（用於 webhook 即時回覆）
     */
    public void sendReplyFlexMessage(String replyToken, String altText, Map<String, Object> contents) {
        try {
            if (replyToken == null || replyToken.trim().isEmpty()) {
                log.error("❌ [Reply-Flex] Reply Token 為空，無法發送");
                return;
            }

            Map<String, Object> flexMsg = new LinkedHashMap<>();
            flexMsg.put("type", "flex");
            flexMsg.put("altText", altText == null ? "通知" : altText);
            flexMsg.put("contents", contents);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("replyToken", replyToken);
            requestBody.put("messages", new Object[]{flexMsg});

            ResponseEntity<String> response = lineApiClient.reply(requestBody);
            if (response == null) return;

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [Reply-Flex] 已成功回覆 Flex 訊息");
            } else {
                log.error("❌ [Reply-Flex] 發送失敗，狀態碼: {}", response.getStatusCode());
                log.error("❌ [Reply-Flex] 響應內容: {}", response.getBody());
            }
        } catch (Exception e) {
            log.error("❌ [Reply-Flex] 發送失敗", e);
        }
    }




    /**
     * ✅ Push 群組 Flex 訊息（供排程/後續結果通知使用）
     */
    
    /**
     * ✅ Push 群組 Flex 訊息（供排程/後續結果通知使用）
     */
    public void sendGroupFlexMessageByPush(String groupId, String altText, Map<String, Object> contents) {
        try {
            if (groupId == null || groupId.trim().isEmpty()) {
                log.error("❌ [群組通知-Flex-Push] 群組 ID 為空，無法發送");
                return;
            }

            Map<String, Object> flexMsg = new LinkedHashMap<>();
            flexMsg.put("type", "flex");
            flexMsg.put("altText", altText == null ? "通知" : altText);
            flexMsg.put("contents", contents);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("to", groupId);
            requestBody.put("messages", new Object[]{flexMsg});

            ResponseEntity<String> response = lineApiClient.push(requestBody);
            if (response == null) return;

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [群組通知-Flex-Push] 已成功發送 Flex 訊息到群組: {}", groupId);
            } else {
                log.error("❌ [群組通知-Flex-Push] 發送失敗，狀態碼: {}", response.getStatusCode());
                log.error("❌ [群組通知-Flex-Push] 響應內容: {}", response.getBody());
            }
        } catch (Exception e) {
            log.error("❌ [群組通知-Flex-Push] 發送失敗", e);
        }
    }




    private Map<String, Object> buildServiceUpdateBubble(ServiceUpdatePayload p) {

        boolean testMode = p.testMode();

        // 🎨 主題色
        String headerColor = testMode ? "#F2994A" : "#1DB446"; // 橘 / 綠
        String badgeText = testMode ? "🧪 【測試模式】" : null;

        Map<String, Object> bubble = new LinkedHashMap<>();
        bubble.put("type", "bubble");
        bubble.put("size", "mega");

        /* ===== Header ===== */
        List<Map<String, Object>> headerContents = new ArrayList<>();

        headerContents.add(
                Map.of(
                        "type", "text",
                        "text", "服事更新成功",
                        "color", "#FFFFFF",
                        "weight", "bold",
                        "size", "lg"
                )
        );

        headerContents.add(
                Map.of(
                        "type", "text",
                        "text", p.dateText() + "（" + p.dayText() + "）",
                        "color", "#FFFFFF",
                        "size", "sm"
                )
        );

        // 🧪 測試模式標籤
        if (badgeText != null) {
            headerContents.add(
                    Map.of(
                            "type", "text",
                            "text", badgeText,
                            "color", "#FFF3E0",
                            "size", "xs",
                            "margin", "sm"
                    )
            );
        }

        Map<String, Object> header = Map.of(
                "type", "box",
                "layout", "vertical",
                "backgroundColor", headerColor,
                "paddingAll", "12px",
                "contents", headerContents
        );

        /* ===== Body ===== */
        Map<String, Object> body = Map.of(
                "type", "box",
                "layout", "vertical",
                "spacing", "md",
                "contents", List.of(
                        kvRow("崗位", p.positionName()),
                        Map.of("type", "separator"),
                        kvRow("變更前", p.beforeName()),
                        kvRow("變更後", p.afterName()),
                        Map.of("type", "separator"),
                        Map.of(
                                "type", "box",
                                "layout", "vertical",
                                "backgroundColor", "#F7F7F7",
                                "paddingAll", "10px",
                                "cornerRadius", "8px",
                                "contents", List.of(
                                        Map.of(
                                                "type", "text",
                                                "text", p.googleText(),
                                                "size", "sm",
                                                "wrap", true
                                        )
                                )
                        )
                )
        );

        bubble.put("header", header);
        bubble.put("body", body);

        return bubble;
    }

    /**
     * ✅ 服事更新結果：用 Reply 送到群組（
     */
    private void sendServiceUpdateFlexToGroupByReply(String replyToken, String altText, ServiceUpdatePayload p) {
        try {
            Map<String, Object> bubble = buildServiceUpdateBubble(p);

            Map<String, Object> contents = new LinkedHashMap<>();
            contents.put("type", "carousel");
            contents.put("contents", List.of(bubble));

            sendReplyFlexMessage(replyToken, altText, contents);
        } catch (Exception e) {
            log.error("❌ [群組通知-Flex-Reply] 發送服事更新 Flex 失敗", e);
            // 降級：純文字
            sendReplyMessage(replyToken, altText);
        }
    }

    /**
     * ✅ 服事更新結果：用 Push 送到群組（因為 ReplyToken 已用來回「處理中」）
     */
    private void sendServiceUpdateFlexToGroupByPush(String groupId, String altText, ServiceUpdatePayload p) {
        try {
            Map<String, Object> bubble = buildServiceUpdateBubble(p);

            Map<String, Object> contents = new LinkedHashMap<>();
            contents.put("type", "carousel");
            contents.put("contents", List.of(bubble));

            sendGroupFlexMessageByPush(groupId, altText, contents);
        } catch (HttpClientErrorException.TooManyRequests e) {
            String body = e.getResponseBodyAsString();

            // ✅ 月額度用完：不要再往上丟例外，改成「略過推送」
            if (body != null && body.contains("monthly limit")) {
                log.warn("⚠️ LINE monthly quota exceeded. Skip group push. body={}", body);
                log.warn("⚠️ LINE 群組通知：本月 LINE 額度已用完，已略過推送");
                return; // 直接結束，讓主流程繼續
            }

            // 其他 429（真的太頻繁）仍然丟出去給上層處理
            throw e;
        } catch (Exception e) {
            log.error("❌ [群組通知-Flex-Push] 發送服事更新 Flex 失敗", e);
            // 降級：純文字
            sendGroupMessageByPush(groupId, altText);
        }
    }

    /**
     * ✅ 發送服事更新 Flex（成功才用）
     */
    /**
     * ✅ 發送服事更新 Flex（成功才用）
     * 目前已由 sendServiceUpdateFlexToGroupByReply / sendServiceUpdateFlexToGroupByPush 統一處理，
     * 這裡保留方法避免其他地方呼叫時編譯失敗。
     */
    private void sendServiceUpdateFlexMessage(String replyToken, String altText, ServiceUpdatePayload p) {
        // 統一走新的共用方法：carousel + bubble（含 test mode 主題）
        sendServiceUpdateFlexToGroupByReply(replyToken, altText, p);
    }


    private Map<String, Object> kvRow(String k, String v) {
        return Map.of(
                "type", "box",
                "layout", "baseline",
                "contents", List.of(
                        Map.of("type", "text", "text", k, "size", "sm", "color", "#888888", "flex", 2),
                        Map.of("type", "text", "text", (v == null ? "" : v), "size", "sm", "wrap", true, "flex", 5)
                )
        );
    }

    /**
     * 發送推播訊息給特定用戶
     */
    
    /**
     * 發送推播訊息給特定用戶（純文字）
     */
    public void sendPushMessage(String userId, String message) {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                log.error("❌ 用戶 ID 為空，無法發送推播訊息");
                return;
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("to", userId);
            requestBody.put("messages", new Object[]{Map.of("type", "text", "text", message)});

            ResponseEntity<String> response = lineApiClient.push(requestBody);
            if (response == null) return;

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ 已發送推播訊息給用戶 {}", userId);
            } else {
                log.error("❌ 發送推播訊息失敗，狀態碼: {}", response.getStatusCode());
                log.error("❌ 發送推播訊息失敗，內容: {}", response.getBody());
            }
        } catch (Exception e) {
            log.error("❌ 發送推播訊息失敗", e);
        }
    }




    /**
     * 發送群組訊息（使用 Push API 直接發送到群組 ID）
     */
    
    /**
     * 發送群組訊息（使用 Push API 直接發送到群組 ID，純文字）
     */
    public void sendGroupMessageByPush(String groupId, String message) {
        try {
            if (groupId == null || groupId.trim().isEmpty()) {
                log.error("❌ [群組通知-Push] 群組 ID 為空，無法發送群組訊息");
                return;
            }

            log.info("📤 [群組通知-Push] 準備發送訊息到群組: {}", groupId);
            log.info("📝 [群組通知-Push] 訊息內容預覽: {}", message != null && message.length() > 100 ? message.substring(0, 100) + "..." : message);

            Optional<LineGroup> groupOpt = churchLineGroupService.findGroup(groupId);
            if (groupOpt.isEmpty()) {
                log.warn("⚠️ [群組通知-Push] 群組 {} 不存在，無法發送訊息", groupId);
                return;
            }

            LineGroup group = groupOpt.get();
            if (!group.getIsActive()) {
                log.info("ℹ️ [群組通知-Push] 群組 {} 已停用，不發送訊息", groupId);
                return;
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("to", groupId);
            requestBody.put("messages", new Object[]{Map.of("type", "text", "text", message)});

            ResponseEntity<String> response = lineApiClient.push(requestBody);
            if (response == null) return;

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [群組通知-Push] 已成功發送訊息到群組: {}", groupId);
            } else {
                log.error("❌ [群組通知-Push] 發送群組訊息失敗，狀態碼: {}", response.getStatusCode());
                log.error("❌ [群組通知-Push] 響應內容: {}", response.getBody());
                log.error("💡 [群組通知-Push] 提示：請確認 Bot 已經加入該群組");
            }

        } catch (Exception e) {
            log.error("❌ [群組通知-Push] 發送群組訊息失敗: {}", e.getMessage(), e);
        }
    }







    @Transactional
    public void handleGroupJoinEvent(String groupId) {
        try {
            churchLineGroupService.handleGroupJoinEvent(groupId);
        } catch (Exception e) {
            log.error("❌ 處理群組加入事件失敗", e);
        }
    }

    @Transactional
    public void handleGroupLeaveEvent(String groupId) {
        try {
            churchLineGroupService.handleGroupLeaveEvent(groupId);
        } catch (Exception e) {
            log.error("❌ 處理群組離開事件失敗", e);
        }
    }

    /**
     * ✅ 群組訊息事件（這裡才有 replyToken，所以 Flex 分流寫在這裡）
     */
    public void handleGroupMessageEvent(String replyToken, String groupId, String userId, String messageText) {
        messageText = messageText.trim();

        log.info("📨 [群組訊息] 收到群組訊息: {}", messageText);
        log.info("📨 [群組訊息] 群組 ID: {}", groupId);
        log.info("📨 [群組訊息] 用戶 ID: {}", userId);
        log.info("📨 [群組訊息] Reply Token: {}", (replyToken != null ? replyToken.substring(0, Math.min(20, replyToken.length())) + "..." : "null"));
        try {
            LineGroup group;
            Optional<LineGroup> groupOpt = churchLineGroupService.findGroup(groupId);
            if (groupOpt.isEmpty()) {
                log.warn("⚠️ [群組訊息] 群組不存在，自動記錄: {}", groupId);
                try {
                    group = churchLineGroupService.ensureActiveGroup(groupId);
                    log.info("✅ [群組訊息] 已自動記錄新群組: {}", groupId);
                } catch (Exception e) {
                    log.error("❌ [群組訊息] 自動記錄群組失敗", e);
                    return;
                }
            } else {
                group = groupOpt.get();
                log.info("✅ [群組訊息] 群組已存在: {}", groupId);

                if (!group.getIsActive()) {
                    log.info("ℹ️ [群組訊息] 群組 {} 已停用，不處理訊息", groupId);
                    return;
                }
            }

            // 記錄成員
            if (userId != null && !userId.isEmpty()) {
                try {
                    churchLineGroupService.ensureActiveMember(group, userId);
                } catch (Exception e) {
                    log.error("❌ [群組訊息] 記錄成員失敗: {}", e.getMessage());
                }
            }

            String groupCode = group.getGroupCode();

            // 群組 ID 查詢
            if (messageText.startsWith("C") && messageText.length() >= 30 && messageText.length() <= 40) {
                String groupInfo = getGroupInfo(messageText);
                sendReplyMessage(replyToken, groupInfo);
                log.info("✅ [群組訊息] 已回應群組 ID 查詢");
                return;
            }

            if (userId != null && !userId.isEmpty()) {
                Optional<LineGroupMember> memberOpt = churchLineGroupService.findMember(group, userId);
                boolean isAdmin = memberOpt.isPresent() && memberOpt.get().getIsAdmin() && memberOpt.get().getIsActive();
                boolean isActiveMember = memberOpt.isPresent() && memberOpt.get().getIsActive();

                if (!isActiveMember) {
                    log.info("ℹ️ [群組訊息] 成員 {} 未啟用，無法執行指令", userId);
                    return;
                }

                boolean canProcessMessage;
                if ("CHURCH_TECH_CONTROL".equals(groupCode)) {
                    String lowerMessage = messageText.toLowerCase();
                    if ("本周服事表".equals(messageText) || "本週服事表".equals(messageText) || "help".equals(lowerMessage) || "幫助".equals(messageText)) {
                        canProcessMessage = true;
                    } else {
                        canProcessMessage = isAdmin;
                    }
                } else {
                    canProcessMessage = isAdmin;
                }

                if (canProcessMessage) {
                    log.info("🛡️ [群組訊息] 用戶 {} {}，開始解析訊息，群組代碼: {}", userId, isAdmin ? "是管理員" : "有權限", groupCode);

                    if ("CHURCH_TECH_CONTROL".equals(groupCode)) {
                        Matcher serviceMatcher = SERVICE_UPDATE_PATTERN.matcher(messageText.trim());
                        if (serviceMatcher.find()) {
                            try {
                                ReplyResult rr = processServiceUpdateMessage(serviceMatcher);

                                if (rr.isOk() && "SERVICE_UPDATE".equals(rr.getType()) && rr.getPayload() instanceof ServiceUpdatePayload) {
                                    sendServiceUpdateFlexToGroupByReply(replyToken, rr.getText(), (ServiceUpdatePayload) rr.getPayload());
                                } else {
                                    sendReplyMessage(replyToken, rr.getText());
                                }
                            } catch (Exception ex) {
                                log.error("❌ [群組訊息] 服事更新處理失敗", ex);
                                sendGroupMessageByPush(groupId, "❌ 更新失敗，系統發生錯誤：" + ex.getMessage());
                            }
                            return;
                        }
                    }

                    String response = processMessage(messageText, replyToken, groupCode, groupId, userId);

                    if (response != null && !response.isEmpty()) {
                        if (response.startsWith("❓ 無法識別的指令")) {
                            log.info("ℹ️ [群組訊息] 無法識別的指令，不發送回覆");
                            return;
                        }
                        sendReplyMessage(replyToken, response);
                        log.info("✅ [群組訊息] 已回應指令");
                        return;
                    }
                } else {
                    log.info("ℹ️ [群組訊息] 用戶 {} 沒有權限執行此指令", userId);
                }
            }

            log.info("ℹ️ [群組訊息] 群組訊息已記錄，但不回應（避免群組訊息干擾）");

        } catch (Exception e) {
            log.error("❌ 處理群組訊息時發生錯誤: {}", e.getMessage(), e);
        }
    }

    private String getChurchHelpMessage() {
        return "💡 教會服事表管理 LINE Bot 使用說明：\n\n" +
                "📝 可用指令：\n\n" +
                "1️⃣ 修改崗位人員\n" +
                "格式：日期(yyyyMMdd),崗位,人員\n" +
                "範例：20260101,音控,家偉\n\n" +
                "2️⃣ 本週服事表\n" +
                "指令：本週服事表 或 本周服事表\n\n" +
                "❓ 其他：\n" +
                "• 幫助 - 顯示此說明";
    }

    private String getGroupInfo(String groupId) {
        try {
            return churchLineGroupService.getGroupInfo(groupId);
        } catch (Exception e) {
            log.error("❌ 查詢群組資訊時發生錯誤: {}", e.getMessage(), e);
            return "❌ 查詢群組資訊時發生錯誤，請稍後再試。";
        }
    }

}

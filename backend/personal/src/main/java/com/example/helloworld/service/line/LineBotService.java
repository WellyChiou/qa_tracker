package com.example.helloworld.service.line;

import com.example.helloworld.entity.personal.Expense;
import com.example.helloworld.entity.personal.LineGroup;
import com.example.helloworld.entity.personal.LineGroupMember;
import com.example.helloworld.entity.personal.User;
import com.example.helloworld.repository.personal.UserRepository;
import com.example.helloworld.service.personal.ExpenseService;
import com.example.helloworld.service.personal.PersonalLineGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class LineBotService {
    private static final Logger log = LoggerFactory.getLogger(LineBotService.class);

    @Autowired
    private LineApiClient lineApiClient;

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PersonalLineGroupService personalLineGroupService;

    // 費用記錄格式：類型 主類別 細項 金額 描述
    private static final Pattern EXPENSE_PATTERN = Pattern.compile(
            "(支出|收入)\\s+([^\\d\\s]+)(?:\\s+([^\\d\\s]+))?\\s+(\\d+(?:\\.\\d{1,2})?)(?:\\s+(.*))?",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 處理來自 LINE 的訊息事件（個人）
     */
    public void handleMessageEvent(String replyToken, String userId, String messageText) {
        messageText = messageText.trim();
        log.info("📨 收到 LINE 訊息: {} 來自用戶: {}", messageText, userId);

        try {
            Optional<User> userOpt = userRepository.findByLineUserId(userId);

            if (!userOpt.isPresent()) {
                sendReplyMessage(replyToken, getBindingInstructions(userId));
                return;
            }

            User user = userOpt.get();

            // 個人訊息，groupCode = null
            String response = processMessage(messageText, user, null, null, null);

            sendReplyMessage(replyToken, response);

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
    private String processMessage(String messageText, User user, String replyToken, String groupCode, String groupId) {
        String trimmedMessage = messageText.trim();

        boolean isPersonal = (groupCode == null || "PERSONAL".equals(groupCode));
        if (isPersonal) {
            Matcher matcher = EXPENSE_PATTERN.matcher(messageText);
            if (matcher.find()) {
                return processExpenseMessage(matcher, user);
            }
        }

        if (trimmedMessage.startsWith("C") && trimmedMessage.length() >= 30 && trimmedMessage.length() <= 40) {
            return getGroupInfo(trimmedMessage);
        }

        switch (messageText.toLowerCase()) {
            default:
                return null;
        }
    }

    /**
     * 處理費用記錄訊息（格式：類型 主類別 細項 金額 描述）
     */
    private String processExpenseMessage(Matcher matcher, User user) {
        try {
            String type = matcher.group(1);
            String firstPart = matcher.group(2).trim();
            String secondPart = matcher.group(3);
            String amountStr = matcher.group(4);
            String description = matcher.group(5) != null ? matcher.group(5).trim() : "";

            String expenseType = "支出".equals(type) ? "支出" : "收入";

            String mainCategory;
            String subCategory;

            if (secondPart != null && !secondPart.trim().isEmpty()) {
                mainCategory = firstPart;
                subCategory = secondPart.trim();

                if (!isValidCategory(expenseType, mainCategory, subCategory)) {
                    return String.format("❌ 無效的類別組合：%s > %s\n\n請輸入「幫助」查看支援的類別和細項。", mainCategory, subCategory);
                }
            } else {
                subCategory = firstPart;
                mainCategory = inferMainCategoryFromSubCategory(expenseType, subCategory);

                if (mainCategory == null) {
                    return String.format("❌ 無法識別的細項：%s\n\n請輸入「幫助」查看支援的細項，或使用完整格式：%s [主類別] %s [金額]", subCategory, type, subCategory);
                }

                if (!isValidCategory(expenseType, mainCategory, subCategory)) {
                    return String.format("❌ 系統錯誤：推斷的類別組合無效\n\n請使用完整格式：%s [主類別] %s [金額]", type, subCategory);
                }
            }

            BigDecimal amount = new BigDecimal(amountStr);

            Expense expense = new Expense();
            expense.setDate(LocalDate.now());
            expense.setMember(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
            expense.setType(expenseType);
            expense.setMainCategory(mainCategory);
            expense.setSubCategory(subCategory);
            expense.setAmount(amount);
            expense.setCurrency("TWD");
            expense.setDescription(description);
            expense.setCreatedByUid(user.getUid());
            expense.setUpdatedByUid(user.getUid());

            Expense saved = expenseService.saveExpense(expense);

            return String.format(
                    "✅ 已記錄：%s %s %.2f 元\n類別：%s > %s\n%s",
                    saved.getDate().toString(), expenseType, saved.getAmount(), mainCategory, subCategory,
                    description.isEmpty() ? "" : "描述：" + description
            );

        } catch (NumberFormatException e) {
            return "❌ 金額格式錯誤，請輸入有效的數字。";
        } catch (Exception e) {
            log.error("❌ 創建費用記錄時發生錯誤", e);
            return "❌ 記錄費用失敗，請稍後再試。";
        }
    }

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

            Optional<LineGroup> groupOpt = personalLineGroupService.findGroup(groupId);
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




    /**
     * 發送群組訊息（使用 Multicast API）
     */
    public void sendGroupMessage(String groupId, String message) {
        try {
            if (groupId == null || groupId.trim().isEmpty()) {
                log.error("❌ [群組通知] 群組 ID 為空，無法發送群組訊息");
                return;
            }

            log.info("📤 [群組通知] 準備發送訊息到群組: {}", groupId);

            Optional<LineGroup> groupOpt = personalLineGroupService.findGroup(groupId);
            if (groupOpt.isEmpty()) {
                log.warn("⚠️ [群組通知] 群組 {} 不存在，無法發送訊息", groupId);
                return;
            }

            LineGroup group = groupOpt.get();
            if (!group.getIsActive()) {
                log.info("ℹ️ [群組通知] 群組 {} 已停用，不發送訊息", groupId);
                return;
            }

            List<User> allLineUsers = userRepository.findAll().stream()
                    .filter(user -> user.getLineUserId() != null && !user.getLineUserId().trim().isEmpty())
                    .collect(java.util.stream.Collectors.toList());

            log.info("👥 [群組通知] 找到 {} 個已綁定 LINE 的用戶", allLineUsers.size());

            if (allLineUsers.isEmpty()) {
                log.warn("⚠️ [群組通知] 群組 {} 中沒有已綁定的用戶，無法發送群組訊息", groupId);
                return;
            }

            List<String> userIds = allLineUsers.stream().map(User::getLineUserId).collect(java.util.stream.Collectors.toList());

            log.info("📨 [群組通知] 準備使用 Multicast API 發送給 {} 個用戶", userIds.size());
            log.info("📝 [群組通知] 訊息內容預覽: {}", (message.length() > 100 ? message.substring(0, 100) + "..." : message));

            sendMulticastMessage(userIds, message);
            log.info("✅ [群組通知] 已發送群組訊息到群組: {}，共 {} 個用戶", groupId, userIds.size());

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
            log.error("❌ [群組通知] 發送群組訊息失敗", e);
        }
    }

    public void sendMulticastMessage(List<String> userIds, String message) {
        try {
            if (userIds == null || userIds.isEmpty()) {
                log.error("❌ 用戶 ID 列表為空，無法發送多播訊息");
                return;
            }

            if (userIds.size() > 500) {
                log.warn("⚠️ 用戶數量超過 500，將分批發送");
                for (int i = 0; i < userIds.size(); i += 500) {
                    int end = Math.min(i + 500, userIds.size());
                    List<String> batch = userIds.subList(i, end);
                    sendMulticastBatch(batch, message);
                }
                return;
            }

            sendMulticastBatch(userIds, message);

        } catch (HttpClientErrorException.TooManyRequests e) {
            String body = e.getResponseBodyAsString();

            // ✅ 月額度用完：不要再往上丟例外，改成「略過推送」
            if (body != null && body.contains("monthly limit")) {
                log.warn("⚠️ LINE monthly quota exceeded. Skip push. body={}", body);
                log.warn("⚠️ LINE 通知：本月 LINE 額度已用完，已略過推送");
                return; // 直接結束，讓主流程繼續
            }

            // 其他 429（真的太頻繁）仍然丟出去給上層處理
            throw e;
        } catch (Exception e) {
            log.error("❌ 發送多播訊息失敗: {}", e.getMessage(), e);
        }
    }
    
    private void sendMulticastBatch(List<String> userIds, String message) {
        try {
            String url = "https://api.line.me/v2/bot/message/multicast";

            log.info("📡 [Multicast] 準備發送到 LINE API: {}", url);
            log.info("📡 [Multicast] 目標用戶數量: {}", userIds.size());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("to", userIds);
            requestBody.put("messages", new Object[]{Map.of("type", "text", "text", message)});

            ResponseEntity<String> response = lineApiClient.multicast(requestBody);
            if (response == null) return;

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [Multicast] 已成功發送多播訊息給 {} 個用戶", userIds.size());
                log.info("✅ [Multicast] 響應狀態: {}", response.getStatusCode());
            } else {
                log.error("❌ [Multicast] 發送多播訊息失敗，狀態碼: {}", response.getStatusCode());
                log.error("❌ [Multicast] 響應內容: {}", response.getBody());
            }
        } catch (Exception e) {
            log.error("❌ [Multicast] 發送多播訊息批次失敗", e);
        }
    }




    @Transactional
    public void handleGroupJoinEvent(String groupId) {
        try {
            personalLineGroupService.handleGroupJoinEvent(groupId);
        } catch (Exception e) {
            log.error("❌ 處理群組加入事件失敗", e);
        }
    }

    @Transactional
    public void handleGroupLeaveEvent(String groupId) {
        try {
            personalLineGroupService.handleGroupLeaveEvent(groupId);
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
            Optional<LineGroup> groupOpt = personalLineGroupService.findGroup(groupId);
            if (groupOpt.isEmpty()) {
                log.warn("⚠️ [群組訊息] 群組不存在，自動記錄: {}", groupId);
                try {
                    group = personalLineGroupService.ensureActiveGroup(groupId);
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
                    personalLineGroupService.ensureActiveMember(group, userId);
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
                Optional<LineGroupMember> memberOpt = personalLineGroupService.findMember(group, userId);
                boolean isAdmin = memberOpt.isPresent() && memberOpt.get().getIsAdmin() && memberOpt.get().getIsActive();
                boolean isActiveMember = memberOpt.isPresent() && memberOpt.get().getIsActive();

                if (!isActiveMember) {
                    log.info("ℹ️ [群組訊息] 成員 {} 未啟用，無法執行指令", userId);
                    return;
                }

                boolean canProcessMessage = isAdmin;

                if (canProcessMessage) {
                    log.info("🛡️ [群組訊息] 用戶 {} {}，開始解析訊息，群組代碼: {}", userId, isAdmin ? "是管理員" : "有權限", groupCode);

                    Optional<User> userOpt = userRepository.findByLineUserId(userId);
                    if (userOpt.isPresent()) {

                        String response = processMessage(messageText, userOpt.get(), replyToken, groupCode, groupId);

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
                        log.warn("⚠️ [群組訊息] 用戶 {} 未綁定系統帳號，無法執行指令", userId);
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

    @Transactional
    public boolean bindUserLineId(String userUid, String lineUserId) {
        try {
            Optional<User> userOpt = userRepository.findById(userUid);
            if (!userOpt.isPresent()) {
                return false;
            }

            User user = userOpt.get();
            user.setLineUserId(lineUserId);
            userRepository.save(user);

            log.info("✅ 已綁定用戶 {} 與 LINE ID {}", userUid, lineUserId);
            return true;
        } catch (Exception e) {
            log.error("❌ 綁定 LINE ID 失敗: {}", e.getMessage(), e);
            return false;
        }
    }

    private String getBindingInstructions(String userId) {
        return "👋 歡迎使用費用記錄 LINE Bot！\n\n" +
                "您的 LINE User ID 是：\n" +
                "📋 " + userId + "\n\n" +
                "請在網頁應用中綁定您的帳號：\n" +
                "1. 登入網頁應用\n" +
                "2. 進入個人設定\n" +
                "3. 在「LINE Bot 設定」區域輸入上方顯示的 ID\n" +
                "4. 點擊「綁定 LINE 帳號」\n\n" +
                "綁定成功後，您就可以記錄費用了！";
    }

    private String inferMainCategoryFromSubCategory(String expenseType, String subCategory) {
        Map<String, String> subToMainExpense = Map.ofEntries(
                Map.entry("外食", "食"), Map.entry("食材", "食"), Map.entry("飲料", "食"), Map.entry("零食", "食"),
                Map.entry("服飾", "衣"), Map.entry("鞋子", "衣"), Map.entry("配件", "衣"), Map.entry("美容", "衣"),
                Map.entry("房貸", "住"), Map.entry("租金", "住"), Map.entry("水電瓦斯", "住"), Map.entry("居家用品", "住"),
                Map.entry("家具家電", "住"), Map.entry("裝潢修繕", "住"), Map.entry("網路費", "住"), Map.entry("通訊", "住"),
                Map.entry("交通費", "行"), Map.entry("油費", "行"), Map.entry("停車費", "行"), Map.entry("大眾運輸", "行"),
                Map.entry("交通工具保養", "行"),
                Map.entry("學費", "育"), Map.entry("書籍", "育"), Map.entry("進修", "育"), Map.entry("文具", "育"),
                Map.entry("娛樂", "樂"), Map.entry("旅遊", "樂"), Map.entry("運動", "樂"), Map.entry("社交", "樂"),
                Map.entry("診療", "醫療"), Map.entry("藥品", "醫療"), Map.entry("健檢", "醫療"), Map.entry("醫療用品", "醫療"),
                Map.entry("投資", "其他支出"), Map.entry("教會奉獻", "其他支出"), Map.entry("保險", "其他支出"), Map.entry("稅務", "其他支出")
        );

        Map<String, String> subToMainIncome = Map.ofEntries(
                Map.entry("本薪", "薪資"), Map.entry("獎金", "薪資"), Map.entry("兼職", "薪資"),
                Map.entry("股票", "投資"), Map.entry("基金", "投資"), Map.entry("債券", "投資"), Map.entry("加密貨幣", "投資")
        );

        Map<String, String> subToMainMap = "支出".equals(expenseType) ? subToMainExpense : subToMainIncome;
        return subToMainMap.get(subCategory);
    }

    private boolean isValidCategory(String expenseType, String mainCategory, String subCategory) {
        Map<String, Set<String>> expenseCategories = Map.of(
                "食", Set.of("外食", "食材", "飲料", "零食", "其他"),
                "衣", Set.of("服飾", "鞋子", "配件", "美容", "其他"),
                "住", Set.of("房貸", "租金", "水電瓦斯", "居家用品", "家具家電", "裝潢修繕", "網路費", "通訊", "其他"),
                "行", Set.of("交通費", "油費", "停車費", "大眾運輸", "交通工具保養", "其他"),
                "育", Set.of("學費", "書籍", "進修", "文具", "其他"),
                "樂", Set.of("娛樂", "旅遊", "運動", "社交", "其他"),
                "醫療", Set.of("診療", "藥品", "健檢", "醫療用品", "其他"),
                "其他支出", Set.of("投資", "教會奉獻", "保險", "稅務", "其他")
        );

        Map<String, Set<String>> incomeCategories = Map.of(
                "薪資", Set.of("本薪", "獎金", "兼職", "其他"),
                "投資", Set.of("股票", "基金", "債券", "加密貨幣", "其他")
        );

        Map<String, Set<String>> categoryMap = "支出".equals(expenseType) ? expenseCategories : incomeCategories;
        Set<String> validSubCategories = categoryMap.get(mainCategory);

        return validSubCategories != null && validSubCategories.contains(subCategory);
    }

    private String getHelpMessage() {
        return "💡 費用記錄 LINE Bot 使用說明：\n\n" +
                "📝 記錄格式：\n" +
                "支出 [細項] [金額] [備註]     ← 推薦\n" +
                "支出 [主類別] [細項] [金額] [備註]  ← 完整格式\n\n" +
                "📊 支援的細項（系統會自動辨識主類別）：\n" +
                "🏠 支出：\n" +
                "• 食：外食、食材、飲料、零食\n" +
                "• 衣：服飾、鞋子、配件、美容\n" +
                "• 住：房貸、租金、水電瓦斯、網路費、通訊\n" +
                "• 行：交通費、油費、停車費、大眾運輸\n" +
                "• 育：學費、書籍、文具\n" +
                "• 樂：娛樂、旅遊、運動、社交\n" +
                "• 醫療：診療、藥品、健檢\n" +
                "• 其他：投資、保險、稅務\n\n" +
                "💼 收入：\n" +
                "• 薪資：本薪、獎金、兼職\n" +
                "• 投資：股票、基金、債券\n\n" +
                "📈 查詢指令：\n" +
                "• 狀態 - 查看今日費用總計\n" +
                "• 今天 - 查看今日所有費用\n\n" +
                "❓ 其他：\n" +
                "• 幫助 - 顯示此說明";
    }

    private String getStatusMessage(User user) {
        try {
            var allUserExpenses = expenseService.getExpensesByUserUid(user.getUid());
            LocalDate today = LocalDate.now();

            BigDecimal totalIncome = BigDecimal.ZERO;
            BigDecimal totalExpense = BigDecimal.ZERO;

            for (Expense expense : allUserExpenses) {
                if (expense.getDate().equals(today)) {
                    if ("收入".equals(expense.getType())) {
                        totalIncome = totalIncome.add(expense.getAmount());
                    } else if ("支出".equals(expense.getType())) {
                        totalExpense = totalExpense.add(expense.getAmount());
                    }
                }
            }

            return String.format(
                    "📊 今日費用狀態：\n" +
                            "💰 收入：%.2f 元\n" +
                            "💸 支出：%.2f 元\n" +
                            "📈 淨額：%.2f 元",
                    totalIncome, totalExpense, totalIncome.subtract(totalExpense)
            );

        } catch (Exception e) {
            log.error("❌ 獲取狀態訊息失敗: {}", e.getMessage(), e);
            return "❌ 獲取狀態失敗，請稍後再試。";
        }
    }

    private String getGroupInfo(String groupId) {
        try {
            return personalLineGroupService.getGroupInfo(groupId);
        } catch (Exception e) {
            log.error("❌ 查詢群組資訊時發生錯誤: {}", e.getMessage(), e);
            return "❌ 查詢群組資訊時發生錯誤，請稍後再試。";
        }
    }

    private String getTodayExpensesMessage(User user) {
        try {
            var allUserExpenses = expenseService.getExpensesByUserUid(user.getUid());
            LocalDate today = LocalDate.now();

            StringBuilder message = new StringBuilder("📅 今日費用記錄：\n\n");

            boolean hasRecords = false;
            for (Expense expense : allUserExpenses) {
                if (expense.getDate().equals(today)) {
                    message.append(String.format("%s %s %.2f 元\n", expense.getMainCategory(), expense.getType(), expense.getAmount()));
                    if (expense.getDescription() != null && !expense.getDescription().trim().isEmpty()) {
                        message.append("   └ ").append(expense.getDescription()).append("\n");
                    }
                    hasRecords = true;
                }
            }

            if (!hasRecords) {
                message.append("📝 今日還沒有記錄任何費用");
            }

            return message.toString();

        } catch (Exception e) {
            log.error("❌ 獲取今日費用失敗: {}", e.getMessage(), e);
            return "❌ 獲取今日費用失敗，請稍後再試。";
        }
    }
}

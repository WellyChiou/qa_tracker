package com.example.helloworld.service.personal;

import com.example.helloworld.config.PersonalLineBotConfig;
import com.example.helloworld.entity.personal.Expense;
import com.example.helloworld.entity.personal.LineGroup;
import com.example.helloworld.entity.personal.User;
import com.example.helloworld.repository.personal.LineGroupRepository;
import com.example.helloworld.repository.personal.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class LineBotService {
    private static final Logger log = LoggerFactory.getLogger(LineBotService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private PersonalLineBotConfig lineBotConfig;

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LineGroupRepository lineGroupRepository;

    // 費用記錄格式：類型 主類別 細項 金額 描述
    // 例如：支出 食 外食 150 午餐
    // 支援智慧辨識：支出 外食 150 早餐（自動識別為「食 > 外食」）
    private static final Pattern EXPENSE_PATTERN = Pattern.compile(
        "(支出|收入)\\s+([^\\d\\s]+)(?:\\s+([^\\d\\s]+))?\\s+(\\d+(?:\\.\\d{1,2})?)(?:\\s+(.*))?",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * 處理來自 LINE 的訊息事件
     */
    public void handleMessageEvent(String replyToken, String userId, String messageText) {
        messageText = messageText.trim();

        log.info("📨 收到 LINE 訊息: {} 來自用戶: {}", messageText, userId);

        try {
            // 檢查用戶是否已綁定
            Optional<User> userOpt = userRepository.findByLineUserId(userId);

            if (!userOpt.isPresent()) {
                // 用戶未綁定，發送綁定指引（包含用戶的 LINE User ID）
                sendReplyMessage(replyToken, getBindingInstructions(userId));
                return;
            }

            User user = userOpt.get();

            // 處理訊息
            String response = processMessage(messageText, user);

            // 回覆訊息
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
    private String processMessage(String messageText, User user) {
        // 檢查是否為費用記錄訊息（格式：類型 主類別 細項 金額 描述）
        Matcher matcher = EXPENSE_PATTERN.matcher(messageText);
        if (matcher.find()) {
            return processExpenseMessage(matcher, user);
        }

        // 檢查是否為群組 ID（LINE 群組 ID 通常以 C 開頭，長度約 33 個字符）
        String trimmedMessage = messageText.trim();
        if (trimmedMessage.startsWith("C") && trimmedMessage.length() >= 30 && trimmedMessage.length() <= 40) {
            return getGroupInfo(trimmedMessage);
        }

        // 處理其他命令
        switch (messageText.toLowerCase()) {
            case "help":
            case "幫助":
                return getHelpMessage();

            case "status":
            case "狀態":
                return getStatusMessage(user);

            case "today":
            case "今天":
                return getTodayExpensesMessage(user);

            default:
                return "❓ 無法識別的指令。請輸入 '幫助' 查看可用指令，或使用格式：\n" +
                       "'支出 主類別 細項 金額 描述'\n" +
                       "例如：'支出 食 外食 150 午餐' 或 '收入 薪資 本薪 50000'";
        }
    }

    /**
     * 處理費用記錄訊息（格式：類型 主類別 細項 金額 描述）
     * 支援智慧辨識：如果只提供細項，系統會自動推斷主類別
     */
    private String processExpenseMessage(Matcher matcher, User user) {
        try {
            String type = matcher.group(1); // 支出或收入
            String firstPart = matcher.group(2).trim(); // 第一個分類詞彙
            String secondPart = matcher.group(3); // 第二個分類詞彙（可選）
            String amountStr = matcher.group(4); // 金額
            String description = matcher.group(5) != null ? matcher.group(5).trim() : ""; // 描述

            // 轉換為支出/收入類型
            String expenseType = "支出".equals(type) ? "支出" : "收入";

            String mainCategory;
            String subCategory;

            // 解析分類邏輯
            if (secondPart != null && !secondPart.trim().isEmpty()) {
                // 有兩個分類詞彙：第一個是主類別，第二個是細項
                mainCategory = firstPart;
                subCategory = secondPart.trim();

                // 驗證主類別和細項組合
                if (!isValidCategory(expenseType, mainCategory, subCategory)) {
                    return String.format("❌ 無效的類別組合：%s > %s\n\n請輸入「幫助」查看支援的類別和細項。",
                        mainCategory, subCategory);
                }
            } else {
                // 只有一個分類詞彙，假設它是細項，嘗試推斷主類別
                subCategory = firstPart;
                mainCategory = inferMainCategoryFromSubCategory(expenseType, subCategory);

                if (mainCategory == null) {
                    return String.format("❌ 無法識別的細項：%s\n\n請輸入「幫助」查看支援的細項，或使用完整格式：%s [主類別] %s [金額]",
                        subCategory, type, subCategory);
                }

                // 雙重驗證：確保推斷出的組合是有效的
                if (!isValidCategory(expenseType, mainCategory, subCategory)) {
                    return String.format("❌ 系統錯誤：推斷的類別組合無效\n\n請使用完整格式：%s [主類別] %s [金額]",
                        type, subCategory);
                }
            }

            // 解析金額
            BigDecimal amount = new BigDecimal(amountStr);

            // 創建費用記錄
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

            return String.format("✅ 已記錄：%s %s %.2f 元\n類別：%s > %s\n%s",
                saved.getDate().toString(),
                expenseType,
                saved.getAmount(),
                mainCategory,
                subCategory,
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
     * 發送回覆訊息
     */
    private void sendReplyMessage(String replyToken, String message) {
        try {
            if (replyToken == null || replyToken.trim().isEmpty()) {
                log.error("❌ [Reply] Reply Token 為空，無法發送回覆");
                return;
            }

            String url = "https://api.line.me/v2/bot/message/reply";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(lineBotConfig.getChannelToken());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("replyToken", replyToken);
            requestBody.put("messages", new Object[]{
                Map.of("type", "text", "text", message)
            });

            log.info("📤 [Reply] 準備發送回覆訊息，Reply Token: {}...", replyToken.substring(0, Math.min(20, replyToken.length())));
            log.info("📤 [Reply] 訊息內容: {}", (message.length() > 50 ? message.substring(0, 50) + "..." : message));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [Reply] 已成功發送回覆訊息");
                log.info("✅ [Reply] 響應狀態: {}", response.getStatusCode());
            } else {
                log.error("❌ [Reply] 發送回覆訊息失敗，狀態碼: {}", response.getStatusCode());
                log.error("❌ [Reply] 響應內容: {}", response.getBody());
            }
        } catch (Exception e) {
            log.error("❌ [Reply] 發送回覆訊息失敗", e);
            // 不重新拋出異常，避免影響 webhook 響應
        }
    }

    /**
     * 發送推播訊息給特定用戶
     */
    public void sendPushMessage(String userId, String message) {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                log.error("❌ 用戶 ID 為空，無法發送推播訊息");
                return;
            }

            String url = "https://api.line.me/v2/bot/message/push";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(lineBotConfig.getChannelToken());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("to", userId);
            requestBody.put("messages", new Object[]{
                Map.of("type", "text", "text", message)
            });

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ 已發送推播訊息給用戶 {}", userId);
            } else {
                log.error("❌ 發送推播訊息失敗，狀態碼: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("❌ 發送推播訊息失敗", e);
        }
    }

    /**
     * 發送群組訊息
     * 
     * 注意：LINE Bot API 不支援直接向群組發送 Push Message。
     * 此方法使用 Multicast API 發送給所有已綁定 LINE 的用戶。
     * 
     * LINE Bot API 限制：
     * - 無法獲取群組成員列表
     * - 無法直接向群組發送 Push Message
     * - 只能使用 Multicast API 發送給多個用戶（最多 500 個）
     * 
     * @param groupId 群組 ID（僅用於日誌記錄，實際發送給所有已綁定用戶）
     * @param message 訊息內容
     */
    public void sendGroupMessage(String groupId, String message) {
        try {
            if (groupId == null || groupId.trim().isEmpty()) {
                log.error("❌ [群組通知] 群組 ID 為空，無法發送群組訊息");
                return;
            }

            log.info("📤 [群組通知] 準備發送訊息到群組: {}", groupId);

            // 獲取所有已綁定 LINE 的用戶（LINE API 無法獲取群組成員列表，所以發送給所有已綁定用戶）
            List<User> allLineUsers = userRepository.findAll().stream()
                .filter(user -> user.getLineUserId() != null && !user.getLineUserId().trim().isEmpty())
                .collect(java.util.stream.Collectors.toList());

            log.info("👥 [群組通知] 找到 {} 個已綁定 LINE 的用戶", allLineUsers.size());

            if (allLineUsers.isEmpty()) {
                log.warn("⚠️ [群組通知] 群組 {} 中沒有已綁定的用戶，無法發送群組訊息", groupId);
                return;
            }

            // 使用 Multicast API 發送給所有已綁定的用戶
            List<String> userIds = allLineUsers.stream()
                .map(User::getLineUserId)
                .collect(java.util.stream.Collectors.toList());

            log.info("📨 [群組通知] 準備使用 Multicast API 發送給 {} 個用戶", userIds.size());
            log.info("📝 [群組通知] 訊息內容預覽: {}", (message.length() > 100 ? message.substring(0, 100) + "..." : message));

            sendMulticastMessage(userIds, message);
            log.info("✅ [群組通知] 已發送群組訊息到群組: {}，共 {} 個用戶", groupId, userIds.size());

        } catch (Exception e) {
            log.error("❌ [群組通知] 發送群組訊息失敗", e);
        }
    }

    /**
     * 使用 Multicast API 發送訊息給多個用戶
     * 用於群組通知：發送給群組中所有已綁定的用戶
     */
    public void sendMulticastMessage(java.util.List<String> userIds, String message) {
        try {
            if (userIds == null || userIds.isEmpty()) {
                log.error("❌ 用戶 ID 列表為空，無法發送多播訊息");
                return;
            }

            // LINE Multicast API 最多支援 500 個用戶
            if (userIds.size() > 500) {
                log.warn("⚠️ 用戶數量超過 500，將分批發送");
                // 分批發送
                for (int i = 0; i < userIds.size(); i += 500) {
                    int end = Math.min(i + 500, userIds.size());
                    java.util.List<String> batch = userIds.subList(i, end);
                    sendMulticastBatch(batch, message);
                }
                return;
            }

            sendMulticastBatch(userIds, message);

        } catch (Exception e) {
            log.error("❌ 發送多播訊息失敗: {}", e.getMessage(), e);
        }
    }

    /**
     * 發送多播訊息批次（最多 500 個用戶）
     */
    private void sendMulticastBatch(java.util.List<String> userIds, String message) {
        try {
            String url = "https://api.line.me/v2/bot/message/multicast";

            log.info("📡 [Multicast] 準備發送到 LINE API: {}", url);
            log.info("📡 [Multicast] 目標用戶數量: {}", userIds.size());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(lineBotConfig.getChannelToken());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("to", userIds);
            requestBody.put("messages", new Object[]{
                Map.of("type", "text", "text", message)
            });

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [Multicast] 已成功發送多播訊息給 {} 個用戶", userIds.size());
                log.info("✅ [Multicast] 響應狀態: {}", response.getStatusCode());
            } else {
                log.error("❌ [Multicast] 發送多播訊息失敗，狀態碼: {}", response.getStatusCode());
                log.error("❌ [Multicast] 響應內容: {}", response.getBody());
            }
        } catch (Exception e) {
            log.error("❌ [Multicast] 發送多播訊息批次失敗", e);
            // 不重新拋出異常，避免影響其他群組的通知
        }
    }

    /**
     * 處理群組加入事件
     */
    @Transactional
    public void handleGroupJoinEvent(String groupId) {
        try {
            if (groupId == null || groupId.trim().isEmpty()) {
                log.warn("⚠️ 群組 ID 為空，無法處理加入事件");
                return;
            }

            log.info("📥 處理群組加入事件，群組 ID: {}", groupId);

            Optional<LineGroup> groupOpt = lineGroupRepository.findByGroupId(groupId);
            if (groupOpt.isPresent()) {
                LineGroup group = groupOpt.get();
                group.setIsActive(true);
                lineGroupRepository.save(group);
                log.info("✅ 群組已存在，已重新啟用: {}", groupId);
            } else {
                LineGroup newGroup = new LineGroup();
                newGroup.setGroupId(groupId);
                newGroup.setGroupName("未命名群組");
                newGroup.setIsActive(true);
                lineGroupRepository.save(newGroup);
                log.info("✅ 已記錄新群組: {}", groupId);
            }
        } catch (Exception e) {
            // 記錄錯誤但不拋出異常，確保 webhook 返回 200 OK
            log.error("❌ 處理群組加入事件失敗", e);
            // 不重新拋出異常，避免影響 webhook 響應
        }
    }

    /**
     * 處理群組離開事件
     */
    @Transactional
    public void handleGroupLeaveEvent(String groupId) {
        try {
            if (groupId == null || groupId.trim().isEmpty()) {
                log.warn("⚠️ 群組 ID 為空，無法處理離開事件");
                return;
            }

            log.info("📤 處理群組離開事件，群組 ID: {}", groupId);

            Optional<LineGroup> groupOpt = lineGroupRepository.findByGroupId(groupId);
            if (groupOpt.isPresent()) {
                LineGroup group = groupOpt.get();
                group.setIsActive(false);
                lineGroupRepository.save(group);
                log.info("✅ 群組已停用: {}", groupId);
            } else {
                log.warn("⚠️ 群組不存在: {}", groupId);
            }
        } catch (Exception e) {
            // 記錄錯誤但不拋出異常，確保 webhook 返回 200 OK
            log.error("❌ 處理群組離開事件失敗", e);
            // 不重新拋出異常，避免影響 webhook 響應
        }
    }

    /**
     * 處理群組訊息事件
     */
    public void handleGroupMessageEvent(String replyToken, String groupId, String userId, String messageText) {
        messageText = messageText.trim();

        log.info("📨 [群組訊息] 收到群組訊息: {}", messageText);
        log.info("📨 [群組訊息] 群組 ID: {}", groupId);
        log.info("📨 [群組訊息] 用戶 ID: {}", userId);
        log.info("📨 [群組訊息] Reply Token: {}", (replyToken != null ? replyToken.substring(0, Math.min(20, replyToken.length())) + "..." : "null"));

        try {
            // 檢查群組是否存在，不存在則自動記錄（用於發送通知）
            Optional<LineGroup> groupOpt = lineGroupRepository.findByGroupId(groupId);
            
            if (!groupOpt.isPresent()) {
                log.warn("⚠️ [群組訊息] 群組不存在，自動記錄: {}", groupId);
                // 自動記錄新群組（用於發送通知）
                try {
                    LineGroup newGroup = new LineGroup();
                    newGroup.setGroupId(groupId);
                    newGroup.setGroupName("未命名群組");
                    newGroup.setIsActive(true); // 預設啟用
                    lineGroupRepository.save(newGroup);
                    log.info("✅ [群組訊息] 已自動記錄新群組: {}", groupId);
                } catch (Exception e) {
                    log.error("❌ [群組訊息] 自動記錄群組失敗", e);
                }
            } else {
                log.info("✅ [群組訊息] 群組已存在: {}", groupId);
            }

            // 檢查是否為群組 ID 查詢（LINE 群組 ID 通常以 C 開頭，長度約 33 個字符）
            if (messageText.startsWith("C") && messageText.length() >= 30 && messageText.length() <= 40) {
                // 用戶輸入群組 ID，查詢並返回群組資訊
                String groupInfo = getGroupInfo(messageText);
                sendReplyMessage(replyToken, groupInfo);
                log.info("✅ [群組訊息] 已回應群組 ID 查詢");
                return;
            }

            // 其他群組訊息不回應，只記錄群組資訊（用於發送通知）
            // 如需使用 Bot 功能，請在個人對話中使用
            log.info("ℹ️ [群組訊息] 群組訊息已記錄，但不回應（避免群組訊息干擾）");
            log.info("💡 [群組訊息] 提示：如需使用 Bot 功能，請在個人對話中與 Bot 一對一聊天");
            
        } catch (Exception e) {
            log.error("❌ 處理群組訊息時發生錯誤: {}", e.getMessage(), e);
            // 群組中發生錯誤也不回覆，避免干擾
        }
    }

    /**
     * 綁定用戶 LINE ID
     */
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

    /**
     * 獲取綁定指引訊息
     */
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


    /**
     * 從細項推斷主類別
     */
    private String inferMainCategoryFromSubCategory(String expenseType, String subCategory) {
        // 定義細項到主類別的映射
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
            Map.entry("投資", "其他支出"), Map.entry("教會奉獻", "其他支出"), Map.entry("保險", "其他支出"),
            Map.entry("稅務", "其他支出")
        );

        Map<String, String> subToMainIncome = Map.ofEntries(
            Map.entry("本薪", "薪資"), Map.entry("獎金", "薪資"), Map.entry("兼職", "薪資"),
            Map.entry("股票", "投資"), Map.entry("基金", "投資"), Map.entry("債券", "投資"), Map.entry("加密貨幣", "投資")
        );

        Map<String, String> subToMainMap = "支出".equals(expenseType) ? subToMainExpense : subToMainIncome;
        return subToMainMap.get(subCategory);
    }

    /**
     * 驗證類別和細項的有效性
     */
    private boolean isValidCategory(String expenseType, String mainCategory, String subCategory) {
        // 定義有效的類別組合
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

    /**
     * 獲取幫助訊息
     */
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
               "💡 智慧範例：\n" +
               "• 支出 外食 150 早餐     ← 自動識別為「食 > 外食」\n" +
               "• 支出 交通費 50 公車     ← 自動識別為「行 > 交通費」\n" +
               "• 收入 本薪 50000 月薪    ← 自動識別為「薪資 > 本薪」\n\n" +
               "📈 查詢指令：\n" +
               "• 狀態 - 查看今日費用總計\n" +
               "• 今天 - 查看今日所有費用\n\n" +
               "❓ 其他：\n" +
               "• 幫助 - 顯示此說明";
    }

    /**
     * 獲取狀態訊息
     */
    private String getStatusMessage(User user) {
        try {
            // 使用 created_by_uid 來查詢，這樣更準確
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

            return String.format("📊 今日費用狀態：\n" +
                               "💰 收入：%.2f 元\n" +
                               "💸 支出：%.2f 元\n" +
                               "📈 淨額：%.2f 元",
                               totalIncome, totalExpense, totalIncome.subtract(totalExpense));

        } catch (Exception e) {
            log.error("❌ 獲取狀態訊息失敗: {}", e.getMessage(), e);
            return "❌ 獲取狀態失敗，請稍後再試。";
        }
    }

    /**
     * 獲取今日所有費用訊息
     */
    /**
     * 獲取群組資訊
     */
    private String getGroupInfo(String groupId) {
        try {
            Optional<LineGroup> groupOpt = lineGroupRepository.findByGroupId(groupId);
            
            if (groupOpt.isPresent()) {
                LineGroup group = groupOpt.get();
                String groupName = group.getGroupName() != null && !group.getGroupName().trim().isEmpty() 
                    ? group.getGroupName() 
                    : "未命名群組";
                String status = group.getIsActive() ? "✅ 啟用" : "❌ 停用";
                
                return String.format(
                    "📋 群組資訊：\n\n" +
                    "群組 ID：\n%s\n\n" +
                    "群組名稱：%s\n\n" +
                    "狀態：%s",
                    groupId,
                    groupName,
                    status
                );
            } else {
                return String.format(
                    "❓ 找不到群組資訊\n\n" +
                    "群組 ID：%s\n\n" +
                    "💡 提示：請確認 Bot 已經加入該群組，或該群組 ID 是否正確。",
                    groupId
                );
            }
        } catch (Exception e) {
            log.error("❌ 查詢群組資訊時發生錯誤: {}", e.getMessage(), e);
            return "❌ 查詢群組資訊時發生錯誤，請稍後再試。";
        }
    }

    private String getTodayExpensesMessage(User user) {
        try {
            // 使用 created_by_uid 來查詢，這樣更準確
            var allUserExpenses = expenseService.getExpensesByUserUid(user.getUid());
            LocalDate today = LocalDate.now();

            StringBuilder message = new StringBuilder("📅 今日費用記錄：\n\n");

            boolean hasRecords = false;
            for (Expense expense : allUserExpenses) {
                if (expense.getDate().equals(today)) {
                    message.append(String.format("%s %s %.2f 元\n",
                        expense.getMainCategory(),
                        expense.getType(),
                        expense.getAmount()
                    ));
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

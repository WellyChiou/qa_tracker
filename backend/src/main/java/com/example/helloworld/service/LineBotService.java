package com.example.helloworld.service;

import com.example.helloworld.config.LineBotConfig;
import com.example.helloworld.entity.Expense;
import com.example.helloworld.entity.User;
import com.example.helloworld.repository.UserRepository;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LineBotService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private LineBotConfig lineBotConfig;

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private UserRepository userRepository;

    private static final Pattern EXPENSE_PATTERN = Pattern.compile(
        "(支出|收入)\\s+([^\\d\\s]+)(?:\\s+([^\\d\\s]+))?\\s+(\\d+(?:\\.\\d{1,2})?)(?:\\s+(.*))?",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * 處理來自 LINE 的訊息事件
     */
    public void handleMessageEvent(String replyToken, String userId, String messageText) {
        messageText = messageText.trim();

        System.out.println("📨 收到 LINE 訊息: " + messageText + " 來自用戶: " + userId);

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
            System.err.println("❌ 處理 LINE 訊息時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            try {
                sendReplyMessage(replyToken, "❌ 處理訊息時發生錯誤，請稍後再試。");
            } catch (Exception replyError) {
                System.err.println("❌ 發送錯誤回覆失敗: " + replyError.getMessage());
            }
        }
    }

    /**
     * 處理訊息內容
     */
    private String processMessage(String messageText, User user) {
        // 檢查是否為費用記錄訊息
        Matcher matcher = EXPENSE_PATTERN.matcher(messageText);

        if (matcher.find()) {
            return processExpenseMessage(matcher, user);
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
                return "❓ 無法識別的指令。請輸入 '幫助' 查看可用指令，或使用格式：\n'支出 餐費 150' 或 '收入 薪水 50000'";
        }
    }

    /**
     * 處理費用記錄訊息
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
            System.err.println("❌ 創建費用記錄時發生錯誤: " + e.getMessage());
            return "❌ 記錄費用失敗，請稍後再試。";
        }
    }

    /**
     * 發送回覆訊息
     */
    private void sendReplyMessage(String replyToken, String message) {
        try {
            String url = "https://api.line.me/v2/bot/message/reply";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(lineBotConfig.getChannelToken());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("replyToken", replyToken);
            requestBody.put("messages", new Object[]{
                Map.of("type", "text", "text", message)
            });

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ 已發送回覆訊息");
            } else {
                System.err.println("❌ 發送回覆訊息失敗，狀態碼: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("❌ 發送回覆訊息失敗: " + e.getMessage());
            throw new RuntimeException("發送回覆訊息失敗", e);
        }
    }

    /**
     * 發送推播訊息給特定用戶
     */
    public void sendPushMessage(String userId, String message) {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                System.err.println("❌ 用戶 ID 為空，無法發送推播訊息");
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
                System.out.println("✅ 已發送推播訊息給用戶 " + userId);
            } else {
                System.err.println("❌ 發送推播訊息失敗，狀態碼: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("❌ 發送推播訊息失敗: " + e.getMessage());
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

            System.out.println("✅ 已綁定用戶 " + userUid + " 與 LINE ID " + lineUserId);
            return true;
        } catch (Exception e) {
            System.err.println("❌ 綁定 LINE ID 失敗: " + e.getMessage());
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
            // 查詢今日費用
            var todayExpenses = expenseService.getAllExpenses(
                LocalDate.now().getYear(),
                LocalDate.now().getMonthValue(),
                user.getDisplayName(),
                null, null
            );

            BigDecimal totalIncome = BigDecimal.ZERO;
            BigDecimal totalExpense = BigDecimal.ZERO;

            for (Expense expense : todayExpenses) {
                if (expense.getDate().equals(LocalDate.now())) {
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
            System.err.println("❌ 獲取狀態訊息失敗: " + e.getMessage());
            return "❌ 獲取狀態失敗，請稍後再試。";
        }
    }

    /**
     * 獲取今日所有費用訊息
     */
    private String getTodayExpensesMessage(User user) {
        try {
            var todayExpenses = expenseService.getAllExpenses(
                LocalDate.now().getYear(),
                LocalDate.now().getMonthValue(),
                user.getDisplayName(),
                null, null
            );

            StringBuilder message = new StringBuilder("📅 今日費用記錄：\n\n");

            boolean hasRecords = false;
            for (Expense expense : todayExpenses) {
                if (expense.getDate().equals(LocalDate.now())) {
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
            System.err.println("❌ 獲取今日費用失敗: " + e.getMessage());
            return "❌ 獲取今日費用失敗，請稍後再試。";
        }
    }
}

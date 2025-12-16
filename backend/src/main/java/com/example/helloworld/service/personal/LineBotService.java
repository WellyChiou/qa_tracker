package com.example.helloworld.service.personal;

import com.example.helloworld.config.PersonalLineBotConfig;
import com.example.helloworld.entity.personal.Expense;
import com.example.helloworld.entity.personal.LineGroup;
import com.example.helloworld.entity.personal.User;
import com.example.helloworld.repository.personal.LineGroupRepository;
import com.example.helloworld.repository.personal.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.example.helloworld.entity.personal.LineGroupMember;
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

    @Autowired
    private com.example.helloworld.repository.personal.LineGroupMemberRepository lineGroupMemberRepository;

    @Autowired
    private com.example.helloworld.repository.church.ServiceScheduleDateRepository serviceScheduleDateRepository;
    @Autowired
    private com.example.helloworld.repository.church.PositionRepository positionRepository;
    @Autowired
    private com.example.helloworld.repository.church.PersonRepository personRepository;
    @Autowired
    private com.example.helloworld.repository.church.PositionPersonRepository positionPersonRepository;
    @Autowired
    private com.example.helloworld.repository.church.ServiceSchedulePositionConfigRepository serviceSchedulePositionConfigRepository;
    @Autowired
    private com.example.helloworld.repository.church.ServiceScheduleAssignmentRepository serviceScheduleAssignmentRepository;
    
    @Autowired
    @Lazy
    private com.example.helloworld.scheduler.church.ServiceScheduleNotificationScheduler serviceScheduleNotificationScheduler;

    // 費用記錄格式：類型 主類別 細項 金額 描述
    // 例如：支出 食 外食 150 午餐
    // 支援智慧辨識：支出 外食 150 早餐（自動識別為「食 > 外食」）
    private static final Pattern EXPENSE_PATTERN = Pattern.compile(
        "(支出|收入)\\s+([^\\d\\s]+)(?:\\s+([^\\d\\s]+))?\\s+(\\d+(?:\\.\\d{1,2})?)(?:\\s+(.*))?",
        Pattern.CASE_INSENSITIVE
    );

    // 服事更新格式：日期(yyyyMMdd),崗位,人員
    // 範例：20260101,音控,家偉
    private static final Pattern SERVICE_UPDATE_PATTERN = Pattern.compile(
        "^(\\d{8}),([^,]+),(.+)$"
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

            // 處理訊息（個人訊息，groupCode 為 null 表示個人訊息）
            String response = processMessage(messageText, user, null);

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
     * @param messageText 訊息內容
     * @param user 用戶物件
     * @param groupCode 群組代碼（null 表示個人訊息，PERSONAL 表示個人群組，CHURCH_TECH_CONTROL 表示教會技術控制群組）
     */
    private String processMessage(String messageText, User user, String groupCode) {
        String trimmedMessage = messageText.trim();
        
        // 判斷是否為個人訊息或個人群組（PERSONAL）
        boolean isPersonal = (groupCode == null || "PERSONAL".equals(groupCode));
        // 判斷是否為教會技術控制群組
        boolean isChurchTechControl = "CHURCH_TECH_CONTROL".equals(groupCode);
        
        // 個人訊息或 PERSONAL 群組：處理費用記錄
        if (isPersonal) {
            Matcher matcher = EXPENSE_PATTERN.matcher(messageText);
            if (matcher.find()) {
                return processExpenseMessage(matcher, user);
            }
        }
        
        // 教會技術控制群組：處理服事更新指令（格式：yyyyMMdd,崗位,人員）
        if (isChurchTechControl) {
            Matcher serviceMatcher = SERVICE_UPDATE_PATTERN.matcher(trimmedMessage);
            if (serviceMatcher.find()) {
                return processServiceUpdateMessage(serviceMatcher);
            }
        }

        // 檢查是否為群組 ID（LINE 群組 ID 通常以 C 開頭，長度約 33 個字符）
        if (trimmedMessage.startsWith("C") && trimmedMessage.length() >= 30 && trimmedMessage.length() <= 40) {
            return getGroupInfo(trimmedMessage);
        }

        // 處理其他命令
        switch (messageText.toLowerCase()) {
            case "help":
            case "幫助":
                // 根據群組類型返回不同的幫助訊息
                if (isChurchTechControl) {
                    return getChurchHelpMessage();
                } else {
                    return getHelpMessage();
                }

            case "status":
            case "狀態":
                // 個人訊息或 PERSONAL 群組才支援狀態查詢
                if (isPersonal) {
                    return getStatusMessage(user);
                }
                break;

            case "today":
            case "今天":
                // 個人訊息或 PERSONAL 群組才支援今日費用查詢
                if (isPersonal) {
                    return getTodayExpensesMessage(user);
                }
                break;

            case "本周服事表":
            case "本週服事表":
                // 教會技術控制群組才支援本週服事表
                if (isChurchTechControl) {
                    // 異步執行通知任務，避免阻塞 LINE 回應
                    new Thread(() -> {
                        try {
                            log.info("🔔 [LINE Bot] 用戶 {} 請求發送本週服事表通知", user.getUid());
                            serviceScheduleNotificationScheduler.sendWeeklyServiceNotification();
                        } catch (Exception e) {
                            log.error("❌ [LINE Bot] 執行本週服事表通知失敗", e);
                            // 可以選擇發送失敗訊息給用戶
                            sendPushMessage(user.getLineUserId(), "❌ 發送通知失敗：" + e.getMessage());
                        }
                    }).start();
                    return "✅ 已觸發本週服事表通知任務，請稍候...";
                }
                break;

            default:
                // 根據群組類型返回不同的錯誤訊息
                if (isChurchTechControl) {
                    return "❓ 無法識別的指令。請輸入 '幫助' 查看可用指令。";
                } else {
                    return "❓ 無法識別的指令。請輸入 '幫助' 查看可用指令，或使用格式：\n" +
                           "'支出 主類別 細項 金額 描述'\n" +
                           "例如：'支出 食 外食 150 午餐' 或 '收入 薪資 本薪 50000'";
                }
        }
        
        // 如果指令不適用於當前群組類型
        return "❓ 無法識別的指令。請輸入 '幫助' 查看可用指令。";
    }

    /**
     * 處理服事更新訊息
     * 格式：日期(yyyyMMdd),崗位,人員
     */
    private String processServiceUpdateMessage(Matcher matcher) {
        try {
            String dateStr = matcher.group(1);
            String positionName = matcher.group(2).trim();
            String personName = matcher.group(3).trim();

            // 1. 驗證日期
            LocalDate date;
            try {
                date = LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
            } catch (Exception e) {
                return "❌ 日期格式錯誤，請使用 yyyyMMdd 格式（例如：20260101）。";
            }

            // 檢查日期是否為過去
            if (date.isBefore(LocalDate.now())) {
                return "❌ 無法更新過去的服事表，請輸入未來的日期。";
            }

            // 檢查日期是否為週六或週日
            java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
            String dayType;
            if (dayOfWeek == java.time.DayOfWeek.SATURDAY) {
                dayType = "saturday";
            } else if (dayOfWeek == java.time.DayOfWeek.SUNDAY) {
                dayType = "sunday";
            } else {
                return "❌ 該日期不是週六或週日，請輸入週末的日期。";
            }

            // 2. 驗證崗位
            Optional<com.example.helloworld.entity.church.Position> positionOpt = positionRepository.findByPositionName(positionName);
            if (!positionOpt.isPresent()) {
                // 如果找不到，嘗試列出所有可用崗位
                List<com.example.helloworld.entity.church.Position> allPositions = positionRepository.findByIsActiveTrueOrderBySortOrderAsc();
                StringBuilder sb = new StringBuilder("❌ 找不到崗位「" + positionName + "」。\n\n可用崗位：\n");
                for (com.example.helloworld.entity.church.Position p : allPositions) {
                    sb.append("• ").append(p.getPositionName()).append("\n");
                }
                return sb.toString();
            }
            com.example.helloworld.entity.church.Position position = positionOpt.get();

            // 3. 驗證人員
            // 先找人員實體
            Optional<com.example.helloworld.entity.church.Person> personOpt = personRepository.findByPersonName(personName);
            if (!personOpt.isPresent()) {
                // 如果找不到該人員，列出該崗位在該時段（週六/週日）的可用人員
                List<com.example.helloworld.entity.church.PositionPerson> availablePersons = 
                    positionPersonRepository.findByPositionIdAndDayTypeOrdered(position.getId(), dayType);
                
                StringBuilder sb = new StringBuilder("❌ 系統中找不到人員「" + personName + "」。\n\n");
                sb.append("該崗位在").append(dayType.equals("saturday") ? "週六" : "週日").append("的可用人員：\n");
                
                if (availablePersons.isEmpty()) {
                    sb.append("(無可用人員)");
                } else {
                    for (com.example.helloworld.entity.church.PositionPerson pp : availablePersons) {
                        com.example.helloworld.entity.church.Person p = pp.getPerson();
                        // 優先顯示 displayName，如果為 null 則顯示 personName
                        String displayName = p.getDisplayName();
                        String personNameValue = p.getPersonName();
                        String showName = displayName != null && !displayName.trim().isEmpty() ? displayName : personNameValue;
                        sb.append("• ").append(showName);
                        // 如果 displayName 和 personName 不同，同時顯示兩者
                        if (displayName != null && !displayName.trim().isEmpty() && !displayName.equals(personNameValue)) {
                            sb.append(" (").append(personNameValue).append(")");
                        }
                        sb.append("\n");
                    }
                }
                return sb.toString();
            }
            com.example.helloworld.entity.church.Person person = personOpt.get();

            // 檢查該人員是否屬於該崗位且符合日期類型（六/日）
            Optional<com.example.helloworld.entity.church.PositionPerson> ppOpt = 
                positionPersonRepository.findByPositionIdAndPersonIdAndDayType(position.getId(), person.getId(), dayType);
            
            if (!ppOpt.isPresent()) {
                // 如果人員不在此崗位的此時段配置中，列出該崗位在該時段的可用人員
                List<com.example.helloworld.entity.church.PositionPerson> availablePersons = 
                    positionPersonRepository.findByPositionIdAndDayTypeOrdered(position.getId(), dayType);
                
                StringBuilder sb = new StringBuilder("❌ 人員「" + personName + "」未被分配到「" + positionName + "」的" + (dayType.equals("saturday") ? "週六" : "週日") + "列表。\n\n");
                sb.append("該崗位在").append(dayType.equals("saturday") ? "週六" : "週日").append("的可用人員：\n");
                
                if (availablePersons.isEmpty()) {
                    
                    sb.append("(無可用人員)");
                } else {
                    for (com.example.helloworld.entity.church.PositionPerson pp : availablePersons) {
                        sb.append("• ").append(pp.getPerson().getPersonName()).append("\n");
                    }
                }
                return sb.toString();
            }

            // 4. 執行更新
            // 查找該日期的服事表
            Optional<com.example.helloworld.entity.church.ServiceScheduleDate> scheduleDateOpt = serviceScheduleDateRepository.findByDate(date);
            if (!scheduleDateOpt.isPresent()) {
                return "❌ 找不到 " + dateStr + " 的服事表，請先在後台建立該年度的服事表。";
            }
            com.example.helloworld.entity.church.ServiceScheduleDate scheduleDate = scheduleDateOpt.get();

            // 查找或創建該崗位的配置
            com.example.helloworld.entity.church.ServiceSchedulePositionConfig config;
            Optional<com.example.helloworld.entity.church.ServiceSchedulePositionConfig> configOpt = 
                serviceSchedulePositionConfigRepository.findByServiceScheduleDateAndPosition(scheduleDate, position);
            
            String originalPersonName = "(無)";
            if (configOpt.isPresent()) {
                config = configOpt.get();
                
                // 獲取原始分配的人員，用於顯示變更前資訊
                List<com.example.helloworld.entity.church.ServiceScheduleAssignment> assignments = 
                    serviceScheduleAssignmentRepository.findByServiceSchedulePositionConfig(config);
                if (!assignments.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < assignments.size(); i++) {
                        if (i > 0) sb.append("、");
                        sb.append(assignments.get(i).getPerson().getPersonName());
                    }
                    originalPersonName = sb.toString();
                }
                
                // 清除舊的分配（使用 deleteAll 批量刪除，更高效）
                serviceScheduleAssignmentRepository.deleteAll(assignments);
            } else {
                config = new com.example.helloworld.entity.church.ServiceSchedulePositionConfig();
                config.setServiceScheduleDate(scheduleDate);
                config.setPosition(position);
                config.setPersonCount(1);
                config = serviceSchedulePositionConfigRepository.save(config);
            }

            // 創建新的分配
            com.example.helloworld.entity.church.ServiceScheduleAssignment assignment = new com.example.helloworld.entity.church.ServiceScheduleAssignment();
            assignment.setServiceSchedulePositionConfig(config);
            assignment.setPerson(person);
            assignment.setSortOrder(0);
            serviceScheduleAssignmentRepository.save(assignment);

            // 更新配置的人數
            config.setPersonCount(1); // 目前只支援單人更新，若需多人需修改指令格式
            serviceSchedulePositionConfigRepository.save(config);

            return String.format("✅ 服事更新成功！\n\n日期：%s (%s)\n崗位：%s\n變更前：%s\n變更後：%s", 
                date.toString(), 
                dayType.equals("saturday") ? "週六" : "週日",
                positionName,
                originalPersonName,
                personName);

        } catch (Exception e) {
            log.error("❌ 處理服事更新失敗", e);
            return "❌ 更新失敗，系統發生錯誤：" + e.getMessage();
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
     * 發送群組訊息（使用 Push API 直接發送到群組 ID）
     * 
     * 使用 LINE Bot API 的 Push Message API 直接發送到群組。
     * 注意：Bot 必須已經加入該群組才能發送訊息。
     * 
     * @param groupId 群組 ID
     * @param message 訊息內容
     */
    public void sendGroupMessageByPush(String groupId, String message) {
        try {
            if (groupId == null || groupId.trim().isEmpty()) {
                log.error("❌ [群組通知-Push] 群組 ID 為空，無法發送群組訊息");
                return;
            }

            log.info("📤 [群組通知-Push] 準備發送訊息到群組: {}", groupId);
            log.info("📝 [群組通知-Push] 訊息內容預覽: {}", message.length() > 100 ? message.substring(0, 100) + "..." : message);

            // 檢查群組是否存在且啟用
            Optional<LineGroup> groupOpt = lineGroupRepository.findByGroupId(groupId);
            if (!groupOpt.isPresent()) {
                log.warn("⚠️ [群組通知-Push] 群組 {} 不存在，無法發送訊息", groupId);
                return;
            }
            
            LineGroup group = groupOpt.get();
            if (!group.getIsActive()) {
                log.info("ℹ️ [群組通知-Push] 群組 {} 已停用，不發送訊息", groupId);
                return;
            }

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
                log.info("✅ [群組通知-Push] 已成功發送訊息到群組: {}", groupId);
                log.info("✅ [群組通知-Push] 響應狀態: {}", response.getStatusCode());
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

            // 檢查群組是否存在且啟用
            Optional<LineGroup> groupOpt = lineGroupRepository.findByGroupId(groupId);
            if (!groupOpt.isPresent()) {
                log.warn("⚠️ [群組通知] 群組 {} 不存在，無法發送訊息", groupId);
                return;
            }
            
            LineGroup group = groupOpt.get();
            if (!group.getIsActive()) {
                log.info("ℹ️ [群組通知] 群組 {} 已停用，不發送訊息", groupId);
                return;
            }

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
     * 更新群組成員計數（只計算啟用的成員）
     */
    private void updateGroupMemberCount(LineGroup group) {
        try {
            long count = lineGroupMemberRepository.countByLineGroupAndIsActiveTrue(group);
            group.setMemberCount((int) count);
            lineGroupRepository.save(group);
        } catch (Exception e) {
            log.error("❌ 更新群組成員計數失敗: {}", e.getMessage());
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
            // 1. 檢查並更新群組資訊
            Optional<LineGroup> groupOpt = lineGroupRepository.findByGroupId(groupId);
            LineGroup group;
            
            if (!groupOpt.isPresent()) {
                log.warn("⚠️ [群組訊息] 群組不存在，自動記錄: {}", groupId);
                // 自動記錄新群組（用於發送通知）
                try {
                    LineGroup newGroup = new LineGroup();
                    newGroup.setGroupId(groupId);
                    newGroup.setGroupName("未命名群組");
                    newGroup.setIsActive(true); // 預設啟用
                    newGroup.setMemberCount(1); // 初始只有發送者
                    group = lineGroupRepository.save(newGroup);
                    log.info("✅ [群組訊息] 已自動記錄新群組: {}", groupId);
                } catch (Exception e) {
                    log.error("❌ [群組訊息] 自動記錄群組失敗", e);
                    return; // 無法記錄群組，無法繼續處理成員
                }
            } else {
                group = groupOpt.get();
                log.info("✅ [群組訊息] 群組已存在: {}", groupId);
                
                // 檢查群組是否啟用
                if (!group.getIsActive()) {
                    log.info("ℹ️ [群組訊息] 群組 {} 已停用，不處理訊息", groupId);
                    return;
                }
            }

            // 2. 檢查並更新成員資訊
            if (userId != null && !userId.isEmpty()) {
                try {
                    Optional<LineGroupMember> memberOpt = lineGroupMemberRepository.findByLineGroupAndUserId(group, userId);
                    if (!memberOpt.isPresent()) {
                        log.info("👤 [群組訊息] 記錄新成員: {}", userId);
                        LineGroupMember newMember = new LineGroupMember();
                        newMember.setLineGroup(group);
                        newMember.setUserId(userId);
                        newMember.setIsAdmin(false); // 預設非管理員
                        newMember.setIsActive(true); // 預設啟用
                        // 嘗試獲取顯示名稱（如果有的話，這裡暫時沒有，後續可以通過 Profile API 獲取）
                        newMember.setDisplayName("Line User"); 
                        lineGroupMemberRepository.save(newMember);
                        
                        // 更新群組人數
                        updateGroupMemberCount(group);
                    } else {
                        // 更新最後活躍時間
                        LineGroupMember member = memberOpt.get();
                        // 如果成員之前已離開（isActive = false），現在重新啟用
                        if (!member.getIsActive()) {
                            log.info("👤 [群組訊息] 成員 {} 重新加入群組", userId);
                            member.setIsActive(true);
                        }
                        // 可以在這裡更新 displayName 如果有變更
                        lineGroupMemberRepository.save(member); // 觸發 updatedAt 更新
                    }
                } catch (Exception e) {
                    log.error("❌ [群組訊息] 記錄成員失敗: {}", e.getMessage());
                }
            }

            // 獲取群組代碼
            String groupCode = group.getGroupCode();
            
            // 檢查是否為群組 ID 查詢（LINE 群組 ID 通常以 C 開頭，長度約 33 個字符）
            if (messageText.startsWith("C") && messageText.length() >= 30 && messageText.length() <= 40) {
                // 用戶輸入群組 ID，查詢並返回群組資訊
                String groupInfo = getGroupInfo(messageText);
                sendReplyMessage(replyToken, groupInfo);
                log.info("✅ [群組訊息] 已回應群組 ID 查詢");
                return;
            }

            // 3. 處理群組訊息指令
            if (userId != null && !userId.isEmpty()) {
                // 檢查是否為管理員，並確認成員是否啟用
                Optional<LineGroupMember> memberOpt = lineGroupMemberRepository.findByLineGroupAndUserId(group, userId);
                boolean isAdmin = memberOpt.isPresent() && memberOpt.get().getIsAdmin() && memberOpt.get().getIsActive();
                boolean isActiveMember = memberOpt.isPresent() && memberOpt.get().getIsActive();
                
                // 對於教會技術控制群組，某些指令（如「本週服事表」）允許所有成員執行
                // 其他指令（如修改崗位人員）需要管理員權限
                boolean canProcessMessage = false;
                
                // 只有啟用的成員才能執行指令
                if (!isActiveMember) {
                    log.info("ℹ️ [群組訊息] 成員 {} 未啟用，無法執行指令", userId);
                    return;
                }
                
                if ("CHURCH_TECH_CONTROL".equals(groupCode)) {
                    // 教會技術控制群組：所有成員可以執行「本週服事表」和「幫助」指令
                    String lowerMessage = messageText.toLowerCase();
                    if ("本周服事表".equals(messageText) || "本週服事表".equals(messageText) ||
                        "help".equals(lowerMessage) || "幫助".equals(messageText)) {
                        canProcessMessage = true;
                    } else {
                        // 其他指令需要管理員權限
                        canProcessMessage = isAdmin;
                    }
                } else {
                    // 其他群組：只有管理員可以執行指令
                    canProcessMessage = isAdmin;
                }
                
                if (canProcessMessage) {
                    log.info("🛡️ [群組訊息] 用戶 {} {}，開始解析訊息，群組代碼: {}", 
                        userId, isAdmin ? "是管理員" : "有權限", groupCode);
                    
                    // 嘗試解析訊息（使用與個人訊息相同的處理邏輯）
                    // 但需要先獲取 User 物件（用於記錄創建者）
                    Optional<User> userOpt = userRepository.findByLineUserId(userId);
                    if (userOpt.isPresent()) {
                        // 傳遞群組代碼給 processMessage
                        String response = processMessage(messageText, userOpt.get(), groupCode);
                        if (response != null && !response.isEmpty()) {
                            // 在群組中，如果是無法識別的指令，不發送回覆訊息
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
                        // 可以選擇發送提示，或者忽略
                        // sendReplyMessage(replyToken, "⚠️ 您尚未綁定系統帳號，請先進行綁定。");
                    }
                } else {
                    log.info("ℹ️ [群組訊息] 用戶 {} 沒有權限執行此指令", userId);
                }
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
     * 獲取幫助訊息（個人費用記錄）
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
     * 獲取教會群組幫助訊息
     */
    private String getChurchHelpMessage() {
        return "💡 教會服事表管理 LINE Bot 使用說明：\n\n" +
               "📝 可用指令：\n\n" +
               "1️⃣ 修改崗位人員\n" +
               "格式：日期(yyyyMMdd),崗位,人員\n" +
               "範例：20260101,音控,家偉\n\n" +
               "說明：\n" +
               "• 日期必須是未來的週六或週日\n" +
               "• 崗位名稱必須與系統中的崗位名稱一致\n" +
               "• 人員名稱必須與系統中的人員名稱一致\n" +
               "• 人員必須屬於該崗位的對應時段（週六/週日）\n\n" +
               "2️⃣ 本週服事表\n" +
               "指令：本週服事表 或 本周服事表\n\n" +
               "說明：\n" +
               "• 發送本週的服事表通知到群組\n" +
               "• 系統會自動查詢並發送本週的服事安排\n\n" +
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

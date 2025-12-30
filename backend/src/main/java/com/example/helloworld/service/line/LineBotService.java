package com.example.helloworld.service.line;

import com.example.helloworld.dto.church.admin.GoogleSyncResult;
import com.example.helloworld.dto.church.admin.ReplyResult;
import com.example.helloworld.dto.church.admin.ServiceUpdatePayload;
import com.example.helloworld.entity.personal.Expense;
import com.example.helloworld.entity.personal.LineGroup;
import com.example.helloworld.entity.personal.LineGroupMember;
import com.example.helloworld.entity.personal.User;
import com.example.helloworld.repository.personal.LineGroupRepository;
import com.example.helloworld.repository.personal.UserRepository;
import com.example.helloworld.service.church.GoogleSheetsRosterService;
import com.example.helloworld.service.personal.ExpenseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
    private GoogleSheetsRosterService googleSheetsRosterService;

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
    private static final Pattern EXPENSE_PATTERN = Pattern.compile(
            "(支出|收入)\\s+([^\\d\\s]+)(?:\\s+([^\\d\\s]+))?\\s+(\\d+(?:\\.\\d{1,2})?)(?:\\s+(.*))?",
            Pattern.CASE_INSENSITIVE
    );

    // 服事更新格式：日期(yyyyMMdd),崗位,人員
    private static final Pattern SERVICE_UPDATE_PATTERN = Pattern.compile("^(\\d{8}),([^,]+),(.+)$");

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
        boolean isChurchTechControl = "CHURCH_TECH_CONTROL".equals(groupCode);

        if (isPersonal) {
            Matcher matcher = EXPENSE_PATTERN.matcher(messageText);
            if (matcher.find()) {
                return processExpenseMessage(matcher, user);
            }
        }

        // ⚠️ 注意：這裡仍回純文字（因為 processMessage 沒 replyToken，不能在這裡送 Flex）
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
            //TODO 因 Line 回覆次數限制，故此先不回覆
//            case "help":
//            case "幫助":
//                if (isChurchTechControl) {
//                    return getChurchHelpMessage();
//                } else {
//                    return getHelpMessage();
//                }
//
//            case "status":
//            case "狀態":
//                if (isPersonal) {
//                    return getStatusMessage(user);
//                }
//                break;
//
//            case "today":
//            case "今天":
//                if (isPersonal) {
//                    return getTodayExpensesMessage(user);FFF
//                }
//                break;

            case "本周服事表":
            case "本週服事表":
                if (isChurchTechControl) {
                    new Thread(() -> {
                        try {
                            log.info("🔔 [LINE Bot] 用戶 {} 請求發送本週服事表通知", user.getUid());
                            serviceScheduleNotificationScheduler.sendWeeklyServiceNotification(groupId, replyToken);
                        } catch (Exception e) {
                            log.error("❌ [LINE Bot] 執行本週服事表通知失敗", e);
                            sendPushMessage(user.getLineUserId(), "❌ 發送通知失敗：" + e.getMessage());
                        }
                    }).start();
                    //TODO 因 Line 回覆次數限制，故此先不回覆
//                    return "✅ 已觸發本週服事表通知任務，請稍候...";
                    return null;
                }
                break;

            default:
                //TODO 因 Line 回覆次數限制，故此先不回覆
//                if (isChurchTechControl) {
//                    return "❓ 無法識別的指令。請輸入 '幫助' 查看可用指令。";
//                } else {
//                    return "❓ 無法識別的指令。請輸入 '幫助' 查看可用指令，或使用格式：\n" +
//                            "'支出 主類別 細項 金額 描述'\n" +
//                            "例如：'支出 食 外食 150 午餐' 或 '收入 薪資 本薪 50000'";
//                }
                return null;
        }

        return "❓ 無法識別的指令。請輸入 '幫助' 查看可用指令。";
    }

    /**
     * 處理服事更新訊息
     * 格式：日期(yyyyMMdd),崗位,人員
     */
    private ReplyResult processServiceUpdateMessage(Matcher matcher) {
        try {
            String dateStr = matcher.group(1);
            String positionName = matcher.group(2).trim();
            String personName = matcher.group(3).trim();

            // 1) 驗證日期
            LocalDate date;
            try {
                date = LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
            } catch (Exception e) {
                return ReplyResult.fail("❌ 日期格式錯誤，請使用 yyyyMMdd 格式（例如：20260101）。");
            }

            if (date.isBefore(LocalDate.now())) {
                return ReplyResult.fail("❌ 無法更新過去的服事表，請輸入未來的日期。");
            }

            java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
            String dayType;
            String dayText;
            if (dayOfWeek == java.time.DayOfWeek.SATURDAY) {
                dayType = "saturday";
                dayText = "週六";
            } else if (dayOfWeek == java.time.DayOfWeek.SUNDAY) {
                dayType = "sunday";
                dayText = "週日";
            } else {
                return ReplyResult.fail("❌ 該日期不是週六或週日，請輸入週末的日期。");
            }

            // 2) 驗證崗位
            Optional<com.example.helloworld.entity.church.Position> positionOpt =
                    positionRepository.findByPositionName(positionName);
            if (!positionOpt.isPresent()) {
                List<com.example.helloworld.entity.church.Position> allPositions =
                        positionRepository.findByIsActiveTrueOrderBySortOrderAsc();
                StringBuilder sb = new StringBuilder("❌ 找不到崗位「" + positionName + "」。\n\n可用崗位：\n");
                for (com.example.helloworld.entity.church.Position p : allPositions) {
                    sb.append("• ").append(p.getPositionName()).append("\n");
                }
                return ReplyResult.fail(sb.toString());
            }
            com.example.helloworld.entity.church.Position position = positionOpt.get();

            // 3) 驗證人員
            Optional<com.example.helloworld.entity.church.Person> personOpt =
                    personRepository.findByPersonName(personName);

            if (!personOpt.isPresent()) {
                List<com.example.helloworld.entity.church.PositionPerson> availablePersons =
                        positionPersonRepository.findByPositionIdAndDayTypeOrdered(position.getId(), dayType);

                StringBuilder sb = new StringBuilder("❌ 系統中找不到人員「" + personName + "」。\n\n");
                sb.append("該崗位在").append(dayText).append("的可用人員：\n");

                if (availablePersons.isEmpty()) {
                    sb.append("(無可用人員)");
                } else {
                    for (com.example.helloworld.entity.church.PositionPerson pp : availablePersons) {
                        com.example.helloworld.entity.church.Person p = pp.getPerson();
                        String displayName = p.getDisplayName();
                        String personNameValue = p.getPersonName();
                        String showName = (displayName != null && !displayName.trim().isEmpty())
                                ? displayName
                                : personNameValue;
                        sb.append("• ").append(showName);
                        if (displayName != null && !displayName.trim().isEmpty() && !displayName.equals(personNameValue)) {
                            sb.append(" (").append(personNameValue).append(")");
                        }
                        sb.append("\n");
                    }
                }
                return ReplyResult.fail(sb.toString());
            }

            com.example.helloworld.entity.church.Person person = personOpt.get();

            Optional<com.example.helloworld.entity.church.PositionPerson> ppOpt =
                    positionPersonRepository.findByPositionIdAndPersonIdAndDayType(position.getId(), person.getId(), dayType);

            if (!ppOpt.isPresent()) {
                List<com.example.helloworld.entity.church.PositionPerson> availablePersons =
                        positionPersonRepository.findByPositionIdAndDayTypeOrdered(position.getId(), dayType);

                StringBuilder sb = new StringBuilder("❌ 人員「" + personName + "」未被分配到「" + positionName + "」的" + dayText + "列表。\n\n");
                sb.append("該崗位在").append(dayText).append("的可用人員：\n");

                if (availablePersons.isEmpty()) {
                    sb.append("(無可用人員)");
                } else {
                    for (com.example.helloworld.entity.church.PositionPerson pp : availablePersons) {
                        sb.append("• ").append(pp.getPerson().getPersonName()).append("\n");
                    }
                }
                return ReplyResult.fail(sb.toString());
            }

            // 4) 執行更新
            Optional<com.example.helloworld.entity.church.ServiceScheduleDate> scheduleDateOpt =
                    serviceScheduleDateRepository.findByDate(date);
            if (!scheduleDateOpt.isPresent()) {
                return ReplyResult.fail("❌ 找不到 " + dateStr + " 的服事表，請先在後台建立該年度的服事表。");
            }
            com.example.helloworld.entity.church.ServiceScheduleDate scheduleDate = scheduleDateOpt.get();

            Optional<com.example.helloworld.entity.church.ServiceSchedulePositionConfig> configOpt =
                    serviceSchedulePositionConfigRepository.findByServiceScheduleDateAndPosition(scheduleDate, position);

            com.example.helloworld.entity.church.ServiceSchedulePositionConfig config;

            String originalPersonName = "(無)";
            if (configOpt.isPresent()) {
                config = configOpt.get();

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

                serviceScheduleAssignmentRepository.deleteAll(assignments);
            } else {
                config = new com.example.helloworld.entity.church.ServiceSchedulePositionConfig();
                config.setServiceScheduleDate(scheduleDate);
                config.setPosition(position);
                config.setPersonCount(1);
                config = serviceSchedulePositionConfigRepository.save(config);
            }

            com.example.helloworld.entity.church.ServiceScheduleAssignment assignment =
                    new com.example.helloworld.entity.church.ServiceScheduleAssignment();
            assignment.setServiceSchedulePositionConfig(config);
            assignment.setPerson(person);
            assignment.setSortOrder(0);
            serviceScheduleAssignmentRepository.save(assignment);

            config.setPersonCount(1);
            serviceSchedulePositionConfigRepository.save(config);

            // 顯示名：displayName 優先
            String showName = (person.getDisplayName() != null && !person.getDisplayName().trim().isEmpty())
                    ? person.getDisplayName().trim()
                    : person.getPersonName().trim();

            // ✅ Google 同步：只送出背景任務，不阻塞交易、不 .get() 等待
            GoogleSyncResult googleSyncMsg;
            try {
                googleSyncMsg = googleSheetsRosterService.syncWithRetry(date, positionName, showName); // ✅ 同步等結果（含重試）
            } catch (Exception e) {
                log.error("Google Sheet sync error", e);
                googleSyncMsg = GoogleSyncResult.fail("⚠️ Google PLC 服事表 同步失敗：" + e.getMessage());
            }


            String dateText = date.toString();

            String resultText = String.format(
                    "✅ 服事更新成功\n\n" +
                            "📅 日期：%s（%s）\n" +
                            "🎯 崗位：%s\n" +
                            "🔁 變更：%s → %s\n\n" +
                            "%s",
                    dateText,
                    dayText,
                    positionName,
                    originalPersonName,
                    showName,
                    googleSyncMsg.getMessage()
            );

            ServiceUpdatePayload payload = new ServiceUpdatePayload(
                    dateText,
                    dayText,
                    positionName,
                    originalPersonName,
                    showName,
                    googleSyncMsg.getMessage(),
                    googleSheetsRosterService.isTestMode()

            );

            return ReplyResult.ok(resultText, "SERVICE_UPDATE", payload);

        } catch (Exception e) {
            log.error("❌ 處理服事更新失敗", e);
            return ReplyResult.fail("❌ 更新失敗，系統發生錯誤：" + e.getMessage());
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
            log.error("❌ 處理群組加入事件失敗", e);
        }
    }

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
            log.error("❌ 處理群組離開事件失敗", e);
        }
    }

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
     * ✅ 群組訊息事件（這裡才有 replyToken，所以 Flex 分流寫在這裡）
     */
    public void handleGroupMessageEvent(String replyToken, String groupId, String userId, String messageText) {
        messageText = messageText.trim();

        log.info("📨 [群組訊息] 收到群組訊息: {}", messageText);
        log.info("📨 [群組訊息] 群組 ID: {}", groupId);
        log.info("📨 [群組訊息] 用戶 ID: {}", userId);
        log.info("📨 [群組訊息] Reply Token: {}", (replyToken != null ? replyToken.substring(0, Math.min(20, replyToken.length())) + "..." : "null"));
        try {
            Optional<LineGroup> groupOpt = lineGroupRepository.findByGroupId(groupId);
            LineGroup group;

            if (!groupOpt.isPresent()) {
                log.warn("⚠️ [群組訊息] 群組不存在，自動記錄: {}", groupId);
                try {
                    LineGroup newGroup = new LineGroup();
                    newGroup.setGroupId(groupId);
                    newGroup.setGroupName("未命名群組");
                    newGroup.setIsActive(true);
                    newGroup.setMemberCount(1);
                    group = lineGroupRepository.save(newGroup);
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
                    Optional<LineGroupMember> memberOpt = lineGroupMemberRepository.findByLineGroupAndUserId(group, userId);
                    if (!memberOpt.isPresent()) {
                        log.info("👤 [群組訊息] 記錄新成員: {}", userId);
                        LineGroupMember newMember = new LineGroupMember();
                        newMember.setLineGroup(group);
                        newMember.setUserId(userId);
                        newMember.setIsAdmin(false);
                        newMember.setIsActive(true);
                        newMember.setDisplayName("Line User");
                        lineGroupMemberRepository.save(newMember);

                        updateGroupMemberCount(group);
                    } else {
                        LineGroupMember member = memberOpt.get();
                        if (!member.getIsActive()) {
                            log.info("👤 [群組訊息] 成員 {} 重新加入群組", userId);
                            member.setIsActive(true);
                        }
                        lineGroupMemberRepository.save(member);
                    }
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
                Optional<LineGroupMember> memberOpt = lineGroupMemberRepository.findByLineGroupAndUserId(group, userId);
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

                    Optional<User> userOpt = userRepository.findByLineUserId(userId);
                    if (userOpt.isPresent()) {

                        // ✅ 先攔截「服事更新」：先回覆「處理中」，再用 Push 發送最終結果（ReplyToken 只能用一次）
                        if ("CHURCH_TECH_CONTROL".equals(groupCode)) {
                            Matcher serviceMatcher = SERVICE_UPDATE_PATTERN.matcher(messageText.trim());
                            if (serviceMatcher.find()) {
                                //TODO 因 Line 回覆次數限制，故此先不回覆
                                // 1) 先快速回覆（立即讓使用者知道已收到）
                                //sendReplyMessage(replyToken, "✅ 已接收到變更崗位通知，處理中…");

                                // 2) 真正處理（更新 DB + 同步 Google Sheets），完成後以 Push 再送一次「結果」
                                try {
                                    ReplyResult rr = processServiceUpdateMessage(serviceMatcher);

                                    if (rr.isOk() && "SERVICE_UPDATE".equals(rr.getType()) && rr.getPayload() instanceof ServiceUpdatePayload) {
//                                        sendServiceUpdateFlexToGroupByPush(groupId, rr.getText(), (ServiceUpdatePayload) rr.getPayload());
                                        sendServiceUpdateFlexToGroupByReply(replyToken, rr.getText(), (ServiceUpdatePayload) rr.getPayload());
                                    } else {
                                        // 失敗或不是預期 payload：直接用文字 Push
//                                        sendGroupMessageByPush(groupId, rr.getText());
                                        sendReplyMessage(replyToken, rr.getText());
                                    }
                                } catch (Exception ex) {
                                    log.error("❌ [群組訊息] 服事更新處理失敗", ex);
                                    sendGroupMessageByPush(groupId, "❌ 更新失敗，系統發生錯誤：" + ex.getMessage());
                                }
                                return;
                            }
                        }


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
            Optional<LineGroup> groupOpt = lineGroupRepository.findByGroupId(groupId);

            if (groupOpt.isPresent()) {
                LineGroup group = groupOpt.get();
                String groupName = group.getGroupName() != null && !group.getGroupName().trim().isEmpty()
                        ? group.getGroupName()
                        : "未命名群組";
                String status = group.getIsActive() ? "✅ 啟用" : "❌ 停用";

                return String.format("📋 群組資訊：\n\n群組 ID：\n%s\n\n群組名稱：%s\n\n狀態：%s", groupId, groupName, status);
            } else {
                return String.format("❓ 找不到群組資訊\n\n群組 ID：%s\n\n💡 提示：請確認 Bot 已經加入該群組，或該群組 ID 是否正確。", groupId);
            }
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

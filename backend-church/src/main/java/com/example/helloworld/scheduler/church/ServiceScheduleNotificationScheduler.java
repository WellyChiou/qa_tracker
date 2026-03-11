package com.example.helloworld.scheduler.church;

import com.example.helloworld.entity.church.ServiceSchedule;
import com.example.helloworld.entity.church.ServiceScheduleDate;
import com.example.helloworld.entity.church.ServiceSchedulePositionConfig;
import com.example.helloworld.entity.church.ServiceScheduleAssignment;
import com.example.helloworld.entity.church.Person;
import com.example.helloworld.entity.church.Position;
import com.example.helloworld.repository.church.PositionPersonRepository;
import com.example.helloworld.service.common.ChurchNotificationGroupGateway;
import com.example.helloworld.service.common.NotificationTargetGroup;
import com.example.helloworld.service.church.ServiceScheduleService;
import com.example.helloworld.service.line.LineBotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Optional;

/**
 * 教會服事人員通知排程器
 * 每週二早上 10:00 查詢本周六日的服事人員，發送 LINE 通知
 */
@Component
public class ServiceScheduleNotificationScheduler {
    private static final Logger log = LoggerFactory.getLogger(ServiceScheduleNotificationScheduler.class);

    @Autowired
    private ServiceScheduleService serviceScheduleService;

    @Autowired
    @Lazy
    private LineBotService lineBotService;

    @Autowired
    private ChurchNotificationGroupGateway churchNotificationGroupGateway;

    @Autowired
    private PositionPersonRepository positionPersonRepository;

    /**
     * 週二服事人員通知任務
     * 查詢本周六日的服事人員，發送 LINE 通知
     */
    public static class WeeklyServiceNotificationJob implements Runnable {
        private final ServiceScheduleNotificationScheduler scheduler;

        public WeeklyServiceNotificationJob(ServiceScheduleNotificationScheduler scheduler) {
            this.scheduler = scheduler;
        }

        @Override
        public void run() {
            scheduler.sendWeeklyServiceNotification();
        }
    }

    /**
     * 發送週服事人員通知
     * 使用 @Transactional 確保在事務內訪問數據，避免懶加載異常
     */
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public void sendWeeklyServiceNotification() {
        sendWeeklyServiceNotification(null, null);
    }

    /**
     * 發送週服事人員通知
     *
     * @param targetGroupId 指定發送的群組 ID，如果為 null 則發送到所有啟用群組
     */
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public void sendWeeklyServiceNotification(String targetGroupId, String replyToken) {
        try {
            log.info("📅 [教會排程] 開始查詢本周六日服事人員...");

            // 獲取當前日期
            LocalDate today = LocalDate.now();
            DayOfWeek todayDayOfWeek = today.getDayOfWeek();
            int todayValue = todayDayOfWeek.getValue(); // 1=MONDAY, 7=SUNDAY

            // 計算本周六和週日
            // 週六的值是 6，週日的值是 7
            // 使用簡單的計算方法：計算到本週六/週日的天數

            // 計算到本週六的天數
            int daysUntilSaturday;
            if (todayValue <= DayOfWeek.SATURDAY.getValue()) {
                // 如果今天在週一到週六之間，本週六是未來幾天
                daysUntilSaturday = DayOfWeek.SATURDAY.getValue() - todayValue;
            } else {
                // 如果今天是週日，本週六已經過了，應該通知下週六（未來 6 天）
                daysUntilSaturday = 6;
            }
            LocalDate saturday = today.plusDays(daysUntilSaturday);

            // 計算到本週日的天數
            int daysUntilSunday;
            if (todayValue < DayOfWeek.SUNDAY.getValue()) {
                // 如果今天在週一到週六之間，本週日是未來幾天
                daysUntilSunday = DayOfWeek.SUNDAY.getValue() - todayValue;
            } else {
                // 如果今天是週日，本週日是今天，但通知應該是下週日（未來 7 天）
                daysUntilSunday = 7;
            }
            LocalDate sunday = today.plusDays(daysUntilSunday);

            // 調試日誌：輸出計算結果
            log.info("📅 [教會排程] 今天是 {} ({})", todayDayOfWeek, today);
            log.info("📅 [教會排程] 計算出的週六: {} (距離今天 {} 天)", saturday, daysUntilSaturday);
            log.info("📅 [教會排程] 計算出的週日: {} (距離今天 {} 天)", sunday, daysUntilSunday);

            // 獲取所有服事表（使用完整數據載入方法，避免懶加載異常）
            List<ServiceSchedule> schedules = serviceScheduleService.getAllSchedulesWithFullData();

            // 查找包含本周六或週日的服事表
            Map<LocalDate, List<Map<String, Object>>> serviceInfo = new HashMap<>();
            // 記錄週六和週日是否有服事表日期記錄（不管有沒有崗位配置）
            boolean hasSaturdayDate = false;
            boolean hasSundayDate = false;

            for (ServiceSchedule schedule : schedules) {
                // 獲取服事表的日期（需要手動載入）
                List<ServiceScheduleDate> dates = schedule.getDates();
                if (dates == null || dates.isEmpty()) {
                    continue;
                }

                for (ServiceScheduleDate date : dates) {
                    LocalDate scheduleDate = date.getDate();
                    if (scheduleDate.equals(saturday)) {
                        hasSaturdayDate = true;
                        // 獲取該日期的服事人員
                        List<Map<String, Object>> persons = getServicePersons(date);
                        if (!persons.isEmpty()) {
                            serviceInfo.put(scheduleDate, persons);
                        }
                    } else if (scheduleDate.equals(sunday)) {
                        hasSundayDate = true;
                        // 獲取該日期的服事人員
                        List<Map<String, Object>> persons = getServicePersons(date);
                        if (!persons.isEmpty()) {
                            serviceInfo.put(scheduleDate, persons);
                        }
                    }
                }
            }
// ✅ 構建通知內容：Flex 卡片（主要） + 純文字（降級備援）
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.TRADITIONAL_CHINESE);

            boolean hasSaturday = serviceInfo.containsKey(saturday);
            boolean hasSunday = serviceInfo.containsKey(sunday);

            List<Map<String, Object>> saturdayPersons = serviceInfo.getOrDefault(saturday, Collections.emptyList());
            List<Map<String, Object>> sundayPersons = serviceInfo.getOrDefault(sunday, Collections.emptyList());

// --- 純文字（降級備援）---
            String fallbackText = buildWeeklyServiceText(
                    saturday, sunday,
                    hasSaturdayDate, hasSundayDate,
                    hasSaturday, hasSunday,
                    saturdayPersons, sundayPersons,
                    formatter
            );

// --- Flex（主要）---
            Map<String, Object> flexContents = buildWeeklyServiceCarousel(
                    saturday, sunday,
                    hasSaturdayDate, hasSundayDate,
                    hasSaturday, hasSunday,
                    saturdayPersons, sundayPersons,
                    formatter
            );

            String altText = "🔔 本週服事表";

// 如果指定了目標群組，只發送到該群組
            if (targetGroupId != null && !targetGroupId.trim().isEmpty()) {
                log.info("📤 [教會排程] 指定發送通知到群組: {}", targetGroupId);
                try {
                    if (replyToken != null) {
                        lineBotService.sendReplyFlexMessage(replyToken, altText, flexContents);
                    } else {
                        lineBotService.sendGroupFlexMessageByPush(targetGroupId, altText, flexContents);
                    }
                    log.info("✅ [教會排程] 已發送服事表（Flex）到指定群組: {}", targetGroupId);
                } catch (Exception e) {
                    log.error("❌ [教會排程] 發送 Flex 到指定群組 {} 失敗: {}", targetGroupId, e.getMessage(), e);
                    // 降級：純文字
                    if (replyToken != null) {
                        lineBotService.sendReplyMessage(replyToken, fallbackText);
                    } else {
                        lineBotService.sendGroupMessageByPush(targetGroupId, fallbackText);
                    }
                }
                return;
            }


            // 查詢資料庫中啟用的教會群組（group_code = 'CHURCH_TECH_CONTROL'）
            List<NotificationTargetGroup> activeGroups = churchNotificationGroupGateway.getActiveChurchGroups();

            if (activeGroups.isEmpty()) {
                log.warn("⚠️ [教會排程] church 資料庫中沒有啟用的教會群組（group_code = 'CHURCH_TECH_CONTROL'），跳過通知");
                log.info("💡 [教會排程] 提示：請在 church 資料庫的 line_groups 表中建立 group_code = 'CHURCH_TECH_CONTROL' 的群組");
                return;
            }

            int successCount = 0;
            int errorCount = 0;
            for (NotificationTargetGroup group : activeGroups) {
                try {
                    log.info("📤 [教會排程] 發送服事表（Flex）到群組: {} ({})", group.getGroupId(), group.getGroupName());
                    try {
                        lineBotService.sendGroupFlexMessageByPush(group.getGroupId(), altText, flexContents);
                    } catch (Exception flexEx) {
                        log.error("❌ [教會排程] 發送 Flex 失敗，降級純文字: {}", flexEx.getMessage());
                        lineBotService.sendGroupMessageByPush(group.getGroupId(), fallbackText);
                    }
                    successCount++;
                } catch (Exception e) {
                    errorCount++;
                    log.error("❌ [教會排程] 發送通知到群組 {} 失敗: {}", group.getGroupId(), e.getMessage(), e);
                }
            }

            log.info("✅ [教會排程] 已發送服事表通知到 {} 個群組", successCount);

            if (errorCount > 0) {
                log.warn("⚠️ [教會排程] 發送通知時發生 {} 個錯誤", errorCount);
                throw new RuntimeException("發送通知時發生 " + errorCount + " 個錯誤，詳見日誌");
            }
        } catch (Exception e) {
            log.error("❌ [教會排程] 發送服事人員通知失敗: {}", e.getMessage(), e);
            // 重新拋出異常，確保 Job 狀態標記為 FAILED
            throw new RuntimeException("發送服事人員通知失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 獲取指定日期的服事人員
     * 包含所有有配置的崗位，即使未分配人員也會顯示
     * 只處理週六和週日的服事安排
     */
    private List<Map<String, Object>> getServicePersons(ServiceScheduleDate date) {
        List<Map<String, Object>> persons = new ArrayList<>();

        // 檢查日期是否為週六或週日，如果不是則直接返回
        Integer dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == null) {
            // 從 LocalDate 計算
            java.time.DayOfWeek javaDayOfWeek = date.getDate().getDayOfWeek();
            int javaValue = javaDayOfWeek.getValue(); // 1=MONDAY, 7=SUNDAY
            dayOfWeek = (javaValue == 7) ? 1 : javaValue + 1; // 1=SUNDAY, 7=SATURDAY
            log.warn("  ⚠️ [教會排程] dayOfWeek 為 null，從 date 計算: {} (javaValue={}) -> {}", javaDayOfWeek, javaValue, dayOfWeek);
        }

        // 只處理週六（7）和週日（1），其他日期直接返回空列表
        if (dayOfWeek == null || (dayOfWeek != 1 && dayOfWeek != 7)) {
            log.warn("⚠️ [教會排程] 日期 {} 不是週六或週日（dayOfWeek={}），跳過處理", date.getDate(), dayOfWeek);
            return persons;
        }

        // dayOfWeek 變數將在後續邏輯中使用

        List<ServiceSchedulePositionConfig> configs = date.getPositionConfigs();
        if (configs == null || configs.isEmpty()) {
            log.warn("⚠️ [教會排程] 日期 {} 沒有崗位配置", date.getDate());
            return persons;
        }

        log.info("📋 [教會排程] 日期 {} 有 {} 個崗位配置", date.getDate(), configs.size());

        for (ServiceSchedulePositionConfig config : configs) {
            Position position = config.getPosition();
            if (position == null) {
                log.warn("⚠️ [教會排程] 崗位配置 ID {} 沒有關聯的崗位", config.getId());
                continue;
            }

            String positionName = position.getPositionName();
            log.info("🔍 [教會排程] 檢查崗位: {} (配置 ID: {})", positionName, config.getId());

            // 強制初始化 assignments 集合（確保從 Session 中載入）
            List<ServiceScheduleAssignment> assignments = config.getAssignments();
            if (assignments != null) {
                // 觸發初始化，確保資料已載入
                int assignmentCount = assignments.size();
                log.info("  📝 [教會排程] 崗位 {} 有 {} 個分配記錄", positionName, assignmentCount);

                // 處理每個 assignment（支援多人，用 "/" 串接）
                boolean hasAssignedPerson = false;
                // 判斷是週六還是週日（1=週日, 7=週六）
                // dayOfWeek 已在方法開始時計算，這裡直接使用
                String dayType = (dayOfWeek == 7) ? "saturday" : "sunday";
                String dayOfWeekText = (dayOfWeek == 7) ? "週六" : "週日";
                log.info("  📅 [教會排程] 日期 {} 是 {} (dayOfWeek={}, dayType={})", date.getDate(), dayOfWeekText, dayOfWeek, dayType);

                // 收集該崗位的所有人員名稱
                List<String> personNames = new ArrayList<>();

                // 按 sortOrder 排序 assignments
                List<ServiceScheduleAssignment> sortedAssignments = assignments.stream()
                        .sorted(Comparator.comparing(ServiceScheduleAssignment::getSortOrder))
                        .collect(Collectors.toList());

                for (ServiceScheduleAssignment assignment : sortedAssignments) {
                    Person person = assignment.getPerson();
                    if (person != null) {
                        // 獲取人員名稱，優先使用 displayName，如果為 null 則使用 personName
                        String displayName = person.getDisplayName();
                        String personNameValue = person.getPersonName();
                        String personName = displayName != null && !displayName.trim().isEmpty() ? displayName : personNameValue;

                        // 詳細的調試日誌：輸出 person 對象的完整信息
                        log.debug("  🔍 [教會排程] 檢查人員 - Person ID: {}, displayName: {}, personName: {}, 最終 personName: {}, 崗位: {}",
                                person.getId(),
                                displayName != null ? "'" + displayName + "'" : "null",
                                personNameValue != null ? "'" + personNameValue + "'" : "null",
                                personName != null ? "'" + personName + "'" : "null",
                                positionName);

                        // 檢查 personName 是否為 null 或空字符串
                        if (personName == null || personName.trim().isEmpty()) {
                            log.warn("  ⚠️ [教會排程] 警告：崗位 {} 的分配記錄 ID {} 關聯的 Person ID {} 的名稱為空！ (displayName={}, personName={})",
                                    positionName, assignment.getId(), person.getId(),
                                    displayName != null ? "'" + displayName + "'" : "null",
                                    personNameValue != null ? "'" + personNameValue + "'" : "null");
                            // 跳過這個 assignment，因為沒有有效的人員名稱
                            continue;
                        }

                        // 檢查該人員是否參與自動分配
                        try {
                            Optional<com.example.helloworld.entity.church.PositionPerson> positionPersonOpt =
                                    positionPersonRepository.findByPositionIdAndPersonIdAndDayType(position.getId(), person.getId(), dayType);

                            if (positionPersonOpt.isPresent()) {
                                com.example.helloworld.entity.church.PositionPerson pp = positionPersonOpt.get();
                                Boolean includeInAutoSchedule = pp.getIncludeInAutoSchedule();
                                log.info("  📋 [教會排程] 找到 position_persons 記錄，includeInAutoSchedule={}", includeInAutoSchedule);

                                // 暫時關閉這邏輯
                                // if (includeInAutoSchedule != null && !includeInAutoSchedule) {
                                //     log.warn("  ⚠️ [教會排程] 崗位 {} 分配給: {}，但該人員不參與自動分配，跳過通知", positionName, personName);
                                //     continue;
                                // }
                            } else {
                                log.info("  ℹ️ [教會排程] 未找到 position_persons 記錄，默認為參與自動分配");
                            }
                        } catch (Exception e) {
                            log.error("  ❌ [教會排程] 查詢 position_persons 時發生錯誤: {}", e.getMessage(), e);
                            // 發生錯誤時，默認為參與自動分配，避免漏掉通知
                        }

                        log.info("  ✅ [教會排程] 崗位 {} 分配給: {}", positionName, personName);
                        personNames.add(personName);
                        hasAssignedPerson = true;
                    } else {
                        log.warn("  ⚠️ [教會排程] 崗位 {} 的分配記錄 ID {} 沒有關聯的人員", positionName, assignment.getId());
                    }
                }

                // 如果有分配人員，創建一個 personInfo，用 "/" 串接多人
                if (hasAssignedPerson && !personNames.isEmpty()) {
                    String personsString = String.join("/", personNames);
                    Map<String, Object> personInfo = new HashMap<>();
                    personInfo.put("position", positionName);
                    personInfo.put("person", personsString);
                    persons.add(personInfo);
                }

                // 如果有 assignment 記錄但沒有分配人員，也顯示崗位
                if (!hasAssignedPerson && assignmentCount > 0) {
                    log.warn("  ⚠️ [教會排程] 崗位 {} 有分配記錄但沒有人員，標記為無安排人員", positionName);
                    Map<String, Object> personInfo = new HashMap<>();
                    personInfo.put("position", positionName);
                    personInfo.put("person", "無安排人員");
                    persons.add(personInfo);
                } else if (assignmentCount == 0) {
                    log.warn("  ⚠️ [教會排程] 崗位 {} 沒有分配記錄，標記為無安排人員", positionName);
                    Map<String, Object> personInfo = new HashMap<>();
                    personInfo.put("position", positionName);
                    personInfo.put("person", "無安排人員");
                    persons.add(personInfo);
                }
            } else {
                log.warn("  ⚠️ [教會排程] 崗位 {} 的 assignments 為 null，標記為無安排人員", positionName);
                Map<String, Object> personInfo = new HashMap<>();
                personInfo.put("position", positionName);
                personInfo.put("person", "無安排人員");
                persons.add(personInfo);
            }
        }

        log.info("📊 [教會排程] 日期 {} 總共找到 {} 個服事人員記錄", date.getDate(), persons.size());
        return persons;
    }

    /**
     * 建立本週服事表純文字（降級備援）
     */
    private String buildWeeklyServiceText(
            LocalDate saturday,
            LocalDate sunday,
            boolean hasSaturdayDate,
            boolean hasSundayDate,
            boolean hasSaturday,
            boolean hasSunday,
            List<Map<String, Object>> saturdayPersons,
            List<Map<String, Object>> sundayPersons,
            DateTimeFormatter formatter
    ) {
        StringBuilder message = new StringBuilder();
        message.append("🔔 本週服事人員通知\n\n");

        // 週六
        message.append("📆 ")
                .append(saturday.format(formatter))
                .append(" (週六)\n\n");

        if (!hasSaturdayDate && !hasSaturday) {
            message.append("  本日無安排服事\n\n");
        } else {
            boolean allUnassignedSaturday = saturdayPersons == null || saturdayPersons.isEmpty()
                    || saturdayPersons.stream().allMatch(p -> "無安排人員".equals(String.valueOf(p.get("person"))));
            if (allUnassignedSaturday) {
                message.append("  本日無安排服事\n\n");
            } else {
                message.append(buildPersonList(saturdayPersons)).append("\n\n");
            }
        }

        // 週日
        message.append("📆 ")
                .append(sunday.format(formatter))
                .append(" (週日)\n\n");

        if (!hasSundayDate && !hasSunday) {
            message.append("  本日無安排服事\n");
        } else {
            boolean allUnassignedSunday = sundayPersons == null || sundayPersons.isEmpty()
                    || sundayPersons.stream().allMatch(p -> "無安排人員".equals(String.valueOf(p.get("person"))));
            if (allUnassignedSunday) {
                message.append("  本日無安排服事\n");
            } else {
                message.append(buildPersonList(sundayPersons));
            }
        }

        return message.toString();
    }


    /**
     * 建立本週服事表 Flex Carousel
     */
    private Map<String, Object> buildWeeklyServiceCarousel(
            LocalDate saturday,
            LocalDate sunday,
            boolean hasSaturdayDate,
            boolean hasSundayDate,
            boolean hasSaturday,
            boolean hasSunday,
            List<Map<String, Object>> saturdayPersons,
            List<Map<String, Object>> sundayPersons,
            DateTimeFormatter formatter
    ) {
        List<Map<String, Object>> bubbles = new ArrayList<>();

        // 週六 Bubble（若不存在也顯示「無安排」）
        bubbles.add(buildDayBubble(
                "本週服事表",
                saturday.format(formatter) + "（週六）",
                hasSaturdayDate || hasSaturday,
                saturdayPersons
        ));

        // 週日 Bubble（若不存在也顯示「無安排」）
        bubbles.add(buildDayBubble(
                "本週服事表",
                sunday.format(formatter) + "（週日）",
                hasSundayDate || hasSunday,
                sundayPersons
        ));

        Map<String, Object> carousel = new LinkedHashMap<>();
        carousel.put("type", "carousel");
        carousel.put("contents", bubbles);
        return carousel;
    }

    private Map<String, Object> buildDayBubble(String title, String subtitle, boolean hasSchedule, List<Map<String, Object>> persons) {
        Map<String, Object> bubble = new LinkedHashMap<>();
        bubble.put("type", "bubble");
        bubble.put("size", "mega");

        Map<String, Object> header = Map.of(
                "type", "box",
                "layout", "vertical",
                "paddingAll", "12px",
                "backgroundColor", "#1F2D3D",
                "contents", List.of(
                        Map.of("type", "text", "text", title, "color", "#FFFFFF", "weight", "bold", "size", "lg"),
                        Map.of("type", "text", "text", subtitle, "color", "#FFFFFF", "size", "sm")
                )
        );

        List<Map<String, Object>> bodyContents = new ArrayList<>();

        if (!hasSchedule) {
            bodyContents.add(Map.of(
                    "type", "box",
                    "layout", "vertical",
                    "backgroundColor", "#F7F7F7",
                    "paddingAll", "12px",
                    "cornerRadius", "10px",
                    "contents", List.of(
                            Map.of("type", "text", "text", "本日無安排服事", "weight", "bold", "size", "md", "wrap", true),
                            Map.of("type", "text", "text", "（尚未建立服事表日期或未配置崗位）", "size", "sm", "color", "#777777", "wrap", true)
                    )
            ));
        } else {
            boolean allUnassigned = persons == null || persons.isEmpty() ||
                    persons.stream().allMatch(p -> "無安排人員".equals(p.get("person")));

            if (allUnassigned) {
                bodyContents.add(Map.of(
                        "type", "box",
                        "layout", "vertical",
                        "backgroundColor", "#F7F7F7",
                        "paddingAll", "12px",
                        "cornerRadius", "10px",
                        "contents", List.of(
                                Map.of("type", "text", "text", "本日無安排服事", "weight", "bold", "size", "md", "wrap", true)
                        )
                ));
            } else {
                // 每個崗位一列
                for (Map<String, Object> p : persons) {
                    String position = String.valueOf(p.getOrDefault("position", ""));
                    String person = String.valueOf(p.getOrDefault("person", ""));

                    bodyContents.add(Map.of(
                            "type", "box",
                            "layout", "baseline",
                            "spacing", "sm",
                            "contents", List.of(
                                    Map.of("type", "text", "text", position, "size", "sm", "color", "#555555", "flex", 3, "wrap", true),
                                    Map.of("type", "text", "text", person, "size", "sm", "flex", 7, "wrap", true)
                            )
                    ));
                }
            }
        }

        Map<String, Object> body = Map.of(
                "type", "box",
                "layout", "vertical",
                "paddingAll", "14px",
                "spacing", "md",
                "contents", bodyContents
        );

        bubble.put("header", header);
        bubble.put("body", body);
        return bubble;
    }

    /**
     * 構建人員列表訊息
     */
    private String buildPersonList(List<Map<String, Object>> persons) {
        if (persons.isEmpty()) {
            return "暫無服事人員";
        }

        // 按崗位分組
        Map<String, List<String>> positionGroups = new LinkedHashMap<>();
        for (Map<String, Object> personInfo : persons) {
            String position = (String) personInfo.get("position");
            String person = (String) personInfo.get("person");

            positionGroups.computeIfAbsent(position, k -> new ArrayList<>()).add(person);
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : positionGroups.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(": ");
            // 如果只有一個人員且是「無安排人員」，直接顯示
            // 否則用「、」連接多個人員
            List<String> personList = entry.getValue();
            if (personList.size() == 1 && "無安排人員".equals(personList.get(0))) {
                sb.append(personList.get(0));
            } else {
                sb.append(String.join("、", personList));
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 獲取週二服事人員通知任務執行器
     */
    public Runnable getWeeklyServiceNotificationJob() {
        return new WeeklyServiceNotificationJob(this);
    }
}

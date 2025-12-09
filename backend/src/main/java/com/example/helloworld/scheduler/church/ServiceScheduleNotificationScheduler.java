package com.example.helloworld.scheduler.church;

import com.example.helloworld.entity.church.ServiceSchedule;
import com.example.helloworld.entity.church.ServiceScheduleDate;
import com.example.helloworld.entity.church.ServiceSchedulePositionConfig;
import com.example.helloworld.entity.church.ServiceScheduleAssignment;
import com.example.helloworld.entity.church.Person;
import com.example.helloworld.entity.church.Position;
import com.example.helloworld.config.LineBotConfig;
import com.example.helloworld.entity.personal.LineGroup;
import com.example.helloworld.repository.personal.LineGroupRepository;
import com.example.helloworld.repository.church.PositionPersonRepository;
import com.example.helloworld.service.church.ServiceScheduleService;
import com.example.helloworld.service.church.ChurchLineBotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    @Autowired
    private ServiceScheduleService serviceScheduleService;

    @Autowired
    private ChurchLineBotService churchLineBotService;

    @Autowired
    private LineGroupRepository lineGroupRepository;

    @Autowired
    private LineBotConfig lineBotConfig;

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
     */
    public void sendWeeklyServiceNotification() {
        try {
            System.out.println("📅 [教會排程] 開始查詢本周六日服事人員...");

            // 獲取當前日期
            LocalDate today = LocalDate.now();
            
            // 計算本周六和週日
            LocalDate saturday = today.with(DayOfWeek.SATURDAY);
            LocalDate sunday = today.with(DayOfWeek.SUNDAY);
            
            // 如果今天是週二，本周六日還在未來
            // 如果今天是週三或之後，需要計算下週六日
            if (today.getDayOfWeek().getValue() > DayOfWeek.TUESDAY.getValue()) {
                saturday = saturday.plusWeeks(1);
                sunday = sunday.plusWeeks(1);
            }

            // 獲取所有服事表（使用完整數據載入方法，避免懶加載異常）
            List<ServiceSchedule> schedules = serviceScheduleService.getAllSchedulesWithFullData();
            
            // 查找包含本周六或週日的服事表
            Map<LocalDate, List<Map<String, Object>>> serviceInfo = new HashMap<>();
            
            for (ServiceSchedule schedule : schedules) {
                // 獲取服事表的日期（需要手動載入）
                List<ServiceScheduleDate> dates = schedule.getDates();
                if (dates == null || dates.isEmpty()) {
                    continue;
                }

                for (ServiceScheduleDate date : dates) {
                    LocalDate scheduleDate = date.getDate();
                    if (scheduleDate.equals(saturday) || scheduleDate.equals(sunday)) {
                        // 獲取該日期的服事人員
                        List<Map<String, Object>> persons = getServicePersons(date);
                        if (!persons.isEmpty()) {
                            serviceInfo.put(scheduleDate, persons);
                        }
                    }
                }
            }

            // 構建通知訊息
            StringBuilder message = new StringBuilder();
            message.append("📅 本周服事人員通知\n\n");
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 (E)", Locale.TRADITIONAL_CHINESE);
            
            if (serviceInfo.containsKey(saturday)) {
                message.append("📆 ").append(saturday.format(formatter)).append("\n");
                message.append(buildPersonList(serviceInfo.get(saturday)));
                message.append("\n");
            }
            
            if (serviceInfo.containsKey(sunday)) {
                message.append("📆 ").append(sunday.format(formatter)).append("\n");
                message.append(buildPersonList(serviceInfo.get(sunday)));
                message.append("\n");
            }

            if (serviceInfo.isEmpty()) {
                message.append("本週六日暫無服事安排。");
            }

            // 發送 LINE 通知到教會群組
            String churchGroupId = lineBotConfig.getChurchGroupId();
            
            // 如果配置了群組 ID，優先使用配置的群組 ID
            if (churchGroupId != null && !churchGroupId.trim().isEmpty()) {
                System.out.println("📤 [教會排程] 使用配置的群組 ID: " + churchGroupId);
                try {
                    churchLineBotService.sendGroupMessage(churchGroupId, message.toString());
                    System.out.println("✅ [教會排程] 已發送服事人員通知到群組: " + churchGroupId);
                } catch (Exception e) {
                    System.err.println("❌ [教會排程] 發送通知到群組失敗: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                // 如果沒有配置群組 ID，查找資料庫中啟用的群組
                List<LineGroup> activeGroups = lineGroupRepository.findByIsActiveTrue();
                
                if (activeGroups.isEmpty()) {
                    System.out.println("⚠️ [教會排程] 沒有配置群組 ID 且資料庫中沒有啟用的群組，跳過通知");
                    System.out.println("💡 [教會排程] 提示：請設置環境變數 LINE_BOT_CHURCH_GROUP_ID 或在資料庫中啟用 LINE 群組");
                    return;
                }

                int successCount = 0;
                for (LineGroup group : activeGroups) {
                    try {
                        System.out.println("📤 [教會排程] 發送通知到群組: " + group.getGroupId() + " (" + group.getGroupName() + ")");
                        churchLineBotService.sendGroupMessage(group.getGroupId(), message.toString());
                        successCount++;
                    } catch (Exception e) {
                        System.err.println("❌ [教會排程] 發送通知到群組 " + group.getGroupId() + " 失敗: " + e.getMessage());
                    }
                }

                System.out.println("✅ [教會排程] 已發送服事人員通知到 " + successCount + " 個群組");
            }
        } catch (Exception e) {
            System.err.println("❌ [教會排程] 發送服事人員通知失敗: " + e.getMessage());
            e.printStackTrace();
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
            System.out.println("  ⚠️ [教會排程] dayOfWeek 為 null，從 date 計算: " + javaDayOfWeek + " (javaValue=" + javaValue + ") -> " + dayOfWeek);
        }
        
        // 只處理週六（7）和週日（1），其他日期直接返回空列表
        if (dayOfWeek == null || (dayOfWeek != 1 && dayOfWeek != 7)) {
            System.out.println("⚠️ [教會排程] 日期 " + date.getDate() + " 不是週六或週日（dayOfWeek=" + dayOfWeek + "），跳過處理");
            return persons;
        }
        
        // dayOfWeek 變數將在後續邏輯中使用
        
        List<ServiceSchedulePositionConfig> configs = date.getPositionConfigs();
        if (configs == null || configs.isEmpty()) {
            System.out.println("⚠️ [教會排程] 日期 " + date.getDate() + " 沒有崗位配置");
            return persons;
        }

        System.out.println("📋 [教會排程] 日期 " + date.getDate() + " 有 " + configs.size() + " 個崗位配置");

        for (ServiceSchedulePositionConfig config : configs) {
            Position position = config.getPosition();
            if (position == null) {
                System.out.println("⚠️ [教會排程] 崗位配置 ID " + config.getId() + " 沒有關聯的崗位");
                continue;
            }

            String positionName = position.getPositionName();
            System.out.println("🔍 [教會排程] 檢查崗位: " + positionName + " (配置 ID: " + config.getId() + ")");

            // 強制初始化 assignments 集合（確保從 Session 中載入）
            List<ServiceScheduleAssignment> assignments = config.getAssignments();
            if (assignments != null) {
                // 觸發初始化，確保資料已載入
                int assignmentCount = assignments.size();
                System.out.println("  📝 [教會排程] 崗位 " + positionName + " 有 " + assignmentCount + " 個分配記錄");
                
                // 處理每個 assignment
                boolean hasAssignedPerson = false;
                // 判斷是週六還是週日（1=週日, 7=週六）
                // dayOfWeek 已在方法開始時計算，這裡直接使用
                String dayType = (dayOfWeek == 7) ? "saturday" : "sunday";
                String dayOfWeekText = (dayOfWeek == 7) ? "週六" : "週日";
                System.out.println("  📅 [教會排程] 日期 " + date.getDate() + " 是 " + dayOfWeekText + " (dayOfWeek=" + dayOfWeek + ", dayType=" + dayType + ")");
                
                for (ServiceScheduleAssignment assignment : assignments) {
                    Person person = assignment.getPerson();
                    if (person != null) {
                        // 獲取人員名稱，優先使用 displayName，如果為 null 則使用 personName
                        String displayName = person.getDisplayName();
                        String personNameValue = person.getPersonName();
                        String personName = displayName != null && !displayName.trim().isEmpty() ? displayName : personNameValue;
                        
                        // 詳細的調試日誌：輸出 person 對象的完整信息
                        System.out.println("  🔍 [教會排程] 檢查人員 - Person ID: " + person.getId() + 
                                         ", displayName: " + (displayName != null ? "'" + displayName + "'" : "null") + 
                                         ", personName: " + (personNameValue != null ? "'" + personNameValue + "'" : "null") + 
                                         ", 最終 personName: " + (personName != null ? "'" + personName + "'" : "null") + 
                                         ", 崗位: " + positionName + " (ID: " + position.getId() + "), dayType: " + dayType);
                        
                        // 檢查 personName 是否為 null 或空字符串
                        if (personName == null || personName.trim().isEmpty()) {
                            System.err.println("  ⚠️ [教會排程] 警告：崗位 " + positionName + " 的分配記錄 ID " + assignment.getId() + 
                                             " 關聯的 Person ID " + person.getId() + " 的名稱為空！" +
                                             " (displayName=" + (displayName != null ? "'" + displayName + "'" : "null") + 
                                             ", personName=" + (personNameValue != null ? "'" + personNameValue + "'" : "null") + ")");
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
                                System.out.println("  📋 [教會排程] 找到 position_persons 記錄，includeInAutoSchedule=" + includeInAutoSchedule);
                                
                                if (includeInAutoSchedule != null && !includeInAutoSchedule) {
                                    System.out.println("  ⚠️ [教會排程] 崗位 " + positionName + " 分配給: " + personName + "，但該人員不參與自動分配，跳過通知");
                                    continue;
                                }
                            } else {
                                System.out.println("  ℹ️ [教會排程] 未找到 position_persons 記錄，默認為參與自動分配");
                            }
                        } catch (Exception e) {
                            System.err.println("  ❌ [教會排程] 查詢 position_persons 時發生錯誤: " + e.getMessage());
                            e.printStackTrace();
                            // 發生錯誤時，默認為參與自動分配，避免漏掉通知
                        }
                        
                        System.out.println("  ✅ [教會排程] 崗位 " + positionName + " 分配給: " + personName);
                        Map<String, Object> personInfo = new HashMap<>();
                        personInfo.put("position", positionName);
                        personInfo.put("person", personName);
                        persons.add(personInfo);
                        hasAssignedPerson = true;
                    } else {
                        System.out.println("  ⚠️ [教會排程] 崗位 " + positionName + " 的分配記錄 ID " + assignment.getId() + " 沒有關聯的人員");
                    }
                }
                
                // 如果有 assignment 記錄但沒有分配人員，也顯示崗位
                if (!hasAssignedPerson && assignmentCount > 0) {
                    System.out.println("  ⚠️ [教會排程] 崗位 " + positionName + " 有分配記錄但沒有人員，標記為未分配");
                    Map<String, Object> personInfo = new HashMap<>();
                    personInfo.put("position", positionName);
                    personInfo.put("person", "未分配");
                    persons.add(personInfo);
                } else if (assignmentCount == 0) {
                    System.out.println("  ⚠️ [教會排程] 崗位 " + positionName + " 沒有分配記錄，標記為未分配");
                    Map<String, Object> personInfo = new HashMap<>();
                    personInfo.put("position", positionName);
                    personInfo.put("person", "未分配");
                    persons.add(personInfo);
                }
            } else {
                System.out.println("  ⚠️ [教會排程] 崗位 " + positionName + " 的 assignments 為 null，標記為未分配");
                Map<String, Object> personInfo = new HashMap<>();
                personInfo.put("position", positionName);
                personInfo.put("person", "未分配");
                persons.add(personInfo);
            }
        }

        System.out.println("📊 [教會排程] 日期 " + date.getDate() + " 總共找到 " + persons.size() + " 個服事人員記錄");
        return persons;
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
            sb.append(String.join("、", entry.getValue()));
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


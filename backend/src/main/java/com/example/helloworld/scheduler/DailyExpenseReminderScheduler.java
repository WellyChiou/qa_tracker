package com.example.helloworld.scheduler;

import com.example.helloworld.config.LineBotConfig;
import com.example.helloworld.entity.Expense;
import com.example.helloworld.entity.User;
import com.example.helloworld.repository.UserRepository;
import com.example.helloworld.service.ExpenseService;
import com.example.helloworld.service.LineBotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class DailyExpenseReminderScheduler {

    @Autowired
    private LineBotConfig lineBotConfig;

    @Autowired
    private LineBotService lineBotService;

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private UserRepository userRepository;

    /**
     * 每日費用記錄提醒任務
     * 每天晚上 8 點檢查用戶是否已記錄今日費用
     */
    @Scheduled(cron = "${line.bot.daily-reminder-cron:0 0 20 * * ?}")
    public void sendDailyExpenseReminder() {
        if (!lineBotConfig.isDailyReminderEnabled()) {
            System.out.println("⏰ 每日費用提醒功能已關閉");
            return;
        }

        System.out.println("⏰ 開始執行每日費用記錄提醒任務...");

        try {
            // 獲取所有已綁定 LINE 的用戶
            List<User> lineUsers = userRepository.findAll().stream()
                .filter(user -> user.getLineUserId() != null && !user.getLineUserId().trim().isEmpty())
                .toList();

            System.out.println("👥 找到 " + lineUsers.size() + " 個已綁定 LINE 的用戶");

            LocalDate today = LocalDate.now();
            int reminderCount = 0;

            for (User user : lineUsers) {
                try {
                    // 檢查用戶今日是否已記錄費用
                    if (!hasRecordedExpenseToday(user, today)) {
                        // 發送提醒訊息
                        sendExpenseReminder(user);
                        reminderCount++;
                        System.out.println("📤 已發送費用記錄提醒給用戶: " + user.getDisplayName());
                    } else {
                        System.out.println("✅ 用戶 " + user.getDisplayName() + " 今日已記錄費用，跳過提醒");
                    }
                } catch (Exception e) {
                    System.err.println("❌ 處理用戶 " + user.getDisplayName() + " 的提醒時發生錯誤: " + e.getMessage());
                }
            }

            System.out.println("✅ 每日費用提醒任務完成，共發送 " + reminderCount + " 個提醒");

        } catch (Exception e) {
            System.err.println("❌ 執行每日費用提醒任務時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 檢查用戶今日是否已記錄費用
     */
    private boolean hasRecordedExpenseToday(User user, LocalDate date) {
        try {
            // 獲取用戶今日的所有費用記錄
            List<Expense> todayExpenses = expenseService.getAllExpenses(
                date.getYear(),
                date.getMonthValue(),
                user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(),
                null, null
            );

            // 檢查是否有今日的記錄
            return todayExpenses.stream()
                .anyMatch(expense -> expense.getDate().equals(date));

        } catch (Exception e) {
            System.err.println("❌ 檢查用戶今日費用記錄時發生錯誤: " + e.getMessage());
            return false; // 發生錯誤時假設沒有記錄，發送提醒
        }
    }

    /**
     * 發送費用記錄提醒
     */
    private void sendExpenseReminder(User user) {
        String message = String.format(
            "💰 提醒：%s，您今天還沒有記錄任何費用哦！\n\n" +
            "📝 您可以直接在 LINE 中輸入：\n" +
            "支出 餐費 150 午餐\n" +
            "收入 薪水 50000\n\n" +
            "或者登入網頁應用來記錄詳細的費用。",
            user.getDisplayName() != null ? user.getDisplayName() : user.getUsername()
        );

        lineBotService.sendPushMessage(user.getLineUserId(), message);
    }

    /**
     * 每日費用統計報告任務
     * 每天晚上 9 點發送今日費用統計給用戶
     */
    @Scheduled(cron = "0 0 21 * * ?")
    public void sendDailyExpenseReport() {
        if (!lineBotConfig.isDailyReminderEnabled()) {
            return;
        }

        System.out.println("📊 開始執行每日費用統計報告任務...");

        try {
            List<User> lineUsers = userRepository.findAll().stream()
                .filter(user -> user.getLineUserId() != null && !user.getLineUserId().trim().isEmpty())
                .toList();

            LocalDate today = LocalDate.now();
            int reportCount = 0;

            for (User user : lineUsers) {
                try {
                    String report = generateDailyExpenseReport(user, today);
                    if (report != null) {
                        lineBotService.sendPushMessage(user.getLineUserId(), report);
                        reportCount++;
                    }
                } catch (Exception e) {
                    System.err.println("❌ 發送統計報告給用戶 " + user.getDisplayName() + " 時發生錯誤: " + e.getMessage());
                }
            }

            System.out.println("✅ 每日費用統計報告任務完成，共發送 " + reportCount + " 個報告");

        } catch (Exception e) {
            System.err.println("❌ 執行每日費用統計報告任務時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 生成每日費用統計報告
     */
    private String generateDailyExpenseReport(User user, LocalDate date) {
        try {
            List<Expense> todayExpenses = expenseService.getAllExpenses(
                date.getYear(),
                date.getMonthValue(),
                user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(),
                null, null
            );

            // 過濾今日費用
            List<Expense> todaysRecords = todayExpenses.stream()
                .filter(expense -> expense.getDate().equals(date))
                .toList();

            if (todaysRecords.isEmpty()) {
                return null; // 沒有記錄，不發送報告
            }

            // 計算統計
            double totalIncome = todaysRecords.stream()
                .filter(e -> "收入".equals(e.getType()))
                .mapToDouble(e -> e.getAmount().doubleValue())
                .sum();

            double totalExpense = todaysRecords.stream()
                .filter(e -> "支出".equals(e.getType()))
                .mapToDouble(e -> e.getAmount().doubleValue())
                .sum();

            StringBuilder report = new StringBuilder();
            report.append(String.format("📊 %s 的今日費用統計\n\n", date.toString()));

            if (totalIncome > 0) {
                report.append(String.format("💰 收入：%.2f 元\n", totalIncome));
            }

            if (totalExpense > 0) {
                report.append(String.format("💸 支出：%.2f 元\n", totalExpense));
            }

            double netAmount = totalIncome - totalExpense;
            report.append(String.format("📈 淨額：%.2f 元\n\n", netAmount));

            report.append("📝 詳細記錄：\n");
            for (Expense expense : todaysRecords) {
                report.append(String.format("• %s %s %.2f 元",
                    expense.getMainCategory(),
                    expense.getType(),
                    expense.getAmount().doubleValue()
                ));

                if (expense.getDescription() != null && !expense.getDescription().trim().isEmpty()) {
                    report.append(" - ").append(expense.getDescription());
                }
                report.append("\n");
            }

            return report.toString();

        } catch (Exception e) {
            System.err.println("❌ 生成費用統計報告時發生錯誤: " + e.getMessage());
            return null;
        }
    }
}

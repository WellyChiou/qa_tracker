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
     * 通過動態排程器系統調度執行
     */
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
            "📝 記錄格式：\n" +
            "支出 [細項] [金額] [備註]     ← 推薦\n" +
            "支出 [主類別] [細項] [金額] [備註]  ← 完整格式\n\n" +
            "💡 範例：\n" +
            "• 支出 外食 150 早餐     ← 系統自動識別為「食 > 外食」\n" +
            "• 支出 行 交通費 50 公車  ← 完整指定類別\n" +
            "• 收入 本薪 50000 月薪\n\n" +
            "📊 智慧辨識：直接輸入細項，系統會自動找到對應的主類別！\n\n" +
            "🔍 輸入「幫助」查看所有支援的細項。",
            user.getDisplayName() != null ? user.getDisplayName() : user.getUsername()
        );

        lineBotService.sendPushMessage(user.getLineUserId(), message);
    }

    /**
     * 每日費用檢查任務
     * 每天晚上 9 點檢查用戶是否已記錄今日費用
     * - 如果沒有記錄，發送提醒通知
     * - 如果有記錄，發送統計報告
     * 通過動態排程器系統調度執行
     */
    public void checkAndNotifyDailyExpense() {
        if (!lineBotConfig.isDailyReminderEnabled()) {
            System.out.println("⏰ 每日費用提醒功能已關閉");
            return;
        }

        System.out.println("⏰ 開始執行每日費用檢查任務（晚上 9 點）...");

        try {
            // 獲取所有已綁定 LINE 的用戶
            List<User> lineUsers = userRepository.findAll().stream()
                .filter(user -> user.getLineUserId() != null && !user.getLineUserId().trim().isEmpty())
                .toList();

            System.out.println("👥 找到 " + lineUsers.size() + " 個已綁定 LINE 的用戶");

            LocalDate today = LocalDate.now();
            int reminderCount = 0;
            int reportCount = 0;

            for (User user : lineUsers) {
                try {
                    // 檢查用戶今日是否已記錄費用
                    if (!hasRecordedExpenseToday(user, today)) {
                        // 沒有記錄，發送提醒通知
                        sendExpenseReminder(user);
                        reminderCount++;
                        System.out.println("📤 已發送費用記錄提醒給用戶: " + user.getDisplayName());
                    } else {
                        // 有記錄，發送統計報告
                        String report = generateDailyExpenseReport(user, today);
                        if (report != null) {
                            lineBotService.sendPushMessage(user.getLineUserId(), report);
                            reportCount++;
                            System.out.println("📊 已發送費用統計報告給用戶: " + user.getDisplayName());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("❌ 處理用戶 " + user.getDisplayName() + " 時發生錯誤: " + e.getMessage());
                }
            }

            System.out.println("✅ 每日費用檢查任務完成，共發送 " + reminderCount + " 個個人提醒，" + reportCount + " 個個人統計報告");

        } catch (Exception e) {
            System.err.println("❌ 執行每日費用檢查任務時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 生成每日費用統計報告
     */
    private String generateDailyExpenseReport(User user, LocalDate date) {
        try {
            // 根據用戶的 UID 查詢該用戶創建的所有費用記錄
            List<Expense> allUserExpenses = expenseService.getExpensesByUserUid(user.getUid());

            // 過濾出今日的記錄
            List<Expense> todaysRecords = allUserExpenses.stream()
                .filter(expense -> expense.getDate().equals(date))
                .toList();

            if (todaysRecords.isEmpty()) {
                // 沒有記錄時也發送一個提示訊息（用於手動測試）
                return String.format("📊 %s 的今日費用統計\n\n" +
                                   "💭 今日尚無費用記錄\n\n" +
                                   "💡 您可以：\n" +
                                   "• 在 LINE 中輸入費用記錄\n" +
                                   "• 登入網頁應用記錄詳細費用", date.toString());
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

    /**
     * 每日費用記錄提醒任務執行器（用於定時任務管理系統）
     */
    public static class SendDailyExpenseReminderJob implements Runnable {
        private final DailyExpenseReminderScheduler scheduler;

        public SendDailyExpenseReminderJob(DailyExpenseReminderScheduler scheduler) {
            this.scheduler = scheduler;
        }

        @Override
        public void run() {
            scheduler.sendDailyExpenseReminder();
        }
    }

    /**
     * 每日費用檢查與統計任務執行器（用於定時任務管理系統）
     */
    public static class CheckAndNotifyDailyExpenseJob implements Runnable {
        private final DailyExpenseReminderScheduler scheduler;

        public CheckAndNotifyDailyExpenseJob(DailyExpenseReminderScheduler scheduler) {
            this.scheduler = scheduler;
        }

        @Override
        public void run() {
            scheduler.checkAndNotifyDailyExpense();
        }
    }

    /**
     * 獲取每日費用記錄提醒任務執行器
     */
    public Runnable getSendDailyExpenseReminderJob() {
        return new SendDailyExpenseReminderJob(this);
    }

    /**
     * 獲取每日費用檢查與統計任務執行器
     */
    public Runnable getCheckAndNotifyDailyExpenseJob() {
        return new CheckAndNotifyDailyExpenseJob(this);
    }
}

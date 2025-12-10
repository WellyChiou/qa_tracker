-- 檢查用戶表中的用戶信息
SELECT uid, username, display_name, email FROM users;

-- 檢查費用記錄中的 member 分佈
SELECT member, COUNT(*) as count FROM expenses GROUP BY member ORDER BY count DESC;

-- 檢查今天的費用記錄
SELECT member, type, main_category, sub_category, amount, date, created_at 
FROM expenses 
WHERE DATE(date) = CURDATE() 
ORDER BY created_at DESC;

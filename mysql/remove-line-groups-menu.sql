-- 移除 LINE 群組管理菜單項
-- 執行此 SQL 以刪除資料庫中已存在的 LINE 群組管理菜單項

USE qa_tracker;

-- 刪除 LINE 群組管理菜單項
DELETE FROM menu_items 
WHERE menu_code = 'ADMIN_LINE_GROUPS';

-- 查看結果
SELECT 
    CASE 
        WHEN COUNT(*) = 0 THEN '✅ LINE 群組管理菜單項已成功刪除'
        ELSE CONCAT('⚠️ 仍有 ', COUNT(*), ' 個 LINE 群組管理菜單項存在')
    END as status
FROM menu_items 
WHERE menu_code = 'ADMIN_LINE_GROUPS';


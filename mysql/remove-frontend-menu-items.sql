-- 刪除所有前台菜單項目（僅在需要清理時使用）
-- 注意：前台網站的導航是從資料庫動態載入的，刪除後需要重新執行 create-frontend-menu-structure.sql
-- 此腳本會刪除所有 menu_type = 'frontend' 的菜單項目

USE church;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 顯示即將刪除的菜單項目（供確認）
SELECT 
    '即將刪除的前台菜單項目' AS message,
    id,
    menu_code,
    menu_name,
    menu_type,
    url,
    parent_id
FROM menu_items
WHERE menu_type = 'frontend'
ORDER BY id;

-- 刪除所有前台菜單項目
DELETE FROM menu_items
WHERE menu_type = 'frontend';

-- 顯示刪除結果
SELECT 
    CONCAT('已刪除 ', ROW_COUNT(), ' 筆前台菜單項目') AS result;

-- 確認沒有遺留的前台菜單
SELECT 
    '確認：剩餘的前台菜單項目' AS message,
    COUNT(*) AS count
FROM menu_items
WHERE menu_type = 'frontend';


-- ============================================
-- 修正菜單權限配置
-- ============================================
-- 根據實際的權限結構，修正 menu_items 表中的 required_permission
-- ============================================

USE church;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================
-- 1. 修正前台內容管理相關菜單的權限
-- ============================================
-- 這些菜單目前錯誤地使用了 SERVICE_SCHEDULE_READ，應該使用各自的專屬權限

-- 教會資訊管理 - 應該使用 CHURCH_INFO_EDIT
UPDATE menu_items 
SET required_permission = 'CHURCH_INFO_EDIT'
WHERE menu_code = 'ADMIN_CHURCH_INFO'
  AND required_permission = 'SERVICE_SCHEDULE_READ';

-- 關於我們管理 - 應該使用 ABOUT_INFO_EDIT
UPDATE menu_items 
SET required_permission = 'ABOUT_INFO_EDIT'
WHERE menu_code = 'ADMIN_ABOUT_INFO'
  AND required_permission = 'SERVICE_SCHEDULE_READ';

-- 活動管理 - 應該使用 ACTIVITY_EDIT
UPDATE menu_items 
SET required_permission = 'ACTIVITY_EDIT'
WHERE menu_code = 'ADMIN_ACTIVITIES'
  AND required_permission = 'SERVICE_SCHEDULE_READ';

-- 聯絡表單管理 - 應該使用 CONTACT_SUBMISSION_READ
UPDATE menu_items 
SET required_permission = 'CONTACT_SUBMISSION_READ'
WHERE menu_code = 'ADMIN_CONTACT_SUBMISSIONS'
  AND required_permission = 'SERVICE_SCHEDULE_READ';

-- 前台內容管理母菜單 - 應該使用 CHURCH_INFO_EDIT（因為子菜單都需要編輯權限）
UPDATE menu_items 
SET required_permission = 'CHURCH_INFO_EDIT'
WHERE menu_code = 'ADMIN_FRONTEND_CONTENT'
  AND required_permission = 'SERVICE_SCHEDULE_READ';

-- ============================================
-- 2. 修正系統設定相關菜單的權限
-- ============================================

-- 菜單管理 - 應該使用 MENU_EDIT
UPDATE menu_items 
SET required_permission = 'MENU_EDIT'
WHERE menu_code = 'ADMIN_MENUS'
  AND required_permission = 'SERVICE_SCHEDULE_READ';

-- URL 權限管理 - 應該使用 URL_PERMISSION_EDIT
UPDATE menu_items 
SET required_permission = 'URL_PERMISSION_EDIT'
WHERE menu_code = 'ADMIN_URL_PERMISSIONS'
  AND required_permission = 'SERVICE_SCHEDULE_READ';

-- ============================================
-- 3. 修正服事表管理相關菜單的權限
-- ============================================
-- 管理頁面應該使用 EDIT 權限，而不是 READ 權限

-- 服事表管理 - 應該使用 SERVICE_SCHEDULE_EDIT（管理頁面需要編輯權限）
UPDATE menu_items 
SET required_permission = 'SERVICE_SCHEDULE_EDIT'
WHERE menu_code = 'ADMIN_SERVICE_SCHEDULE'
  AND required_permission = 'SERVICE_SCHEDULE_READ';

-- 服事管理母菜單 - 應該使用 SERVICE_SCHEDULE_EDIT（因為子菜單都需要編輯權限）
UPDATE menu_items 
SET required_permission = 'SERVICE_SCHEDULE_EDIT'
WHERE menu_code = 'ADMIN_SERVICE_MANAGEMENT'
  AND required_permission = 'SERVICE_SCHEDULE_READ';

-- ============================================
-- 4. 確認主日信息管理的權限（應該已經是正確的）
-- ============================================
-- ADMIN_SUNDAY_MESSAGES 已經使用 SUNDAY_MESSAGE_EDIT，這是正確的
-- 不需要修改

-- ============================================
-- 5. 顯示更新結果
-- ============================================
SELECT 
    '菜單權限修正完成' AS message,
    menu_code,
    menu_name,
    required_permission AS '修正後的權限',
    CASE 
        -- 檢查是否還有使用 READ 權限的管理頁面（應該使用 EDIT）
        WHEN menu_type = 'admin' 
             AND required_permission LIKE '%_READ' 
             AND menu_code NOT LIKE 'FRONTEND_%' THEN '⚠️ 管理頁面應使用 EDIT 權限'
        ELSE '✅ 已修正'
    END AS status
FROM menu_items
WHERE menu_type = 'admin'
ORDER BY order_index;


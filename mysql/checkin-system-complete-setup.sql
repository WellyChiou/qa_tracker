-- ============================================
-- 教會簽到系統完整安裝腳本
-- ============================================
-- 此腳本整合了簽到系統所需的所有 SQL 配置
-- 執行順序：
--   1. 基礎資料表結構（已在 church-schema.sql 中）
--   2. 添加 member_no 欄位到 persons 表
--   3. URL 權限配置
--   4. 後台菜單配置
--   5. 更新菜單 URL
-- ============================================

USE church;

-- ============================================
-- 步驟 1: 確保 persons 表有必要的欄位
-- ============================================
-- 注意：member_no 和 birthday 欄位應該已經在 church-init.sql 中定義
-- 此處僅作為備用檢查，如果欄位不存在則添加

-- 檢查並添加 member_no 欄位（如果不存在）
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'church' 
    AND TABLE_NAME = 'persons' 
    AND COLUMN_NAME = 'member_no'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE persons ADD COLUMN member_no VARCHAR(32) UNIQUE COMMENT ''會員編號（用於簽到系統）'' AFTER display_name;',
    'SELECT "member_no 欄位已存在，跳過添加" AS message;'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 檢查並添加 birthday 欄位（如果不存在）
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'church' 
    AND TABLE_NAME = 'persons' 
    AND COLUMN_NAME = 'birthday'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE persons ADD COLUMN birthday DATE COMMENT ''生日（非必填）'' AFTER member_no;',
    'SELECT "birthday 欄位已存在，跳過添加" AS message;'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 確保 member_no 有索引（如果不存在）
SET @index_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = 'church' 
    AND TABLE_NAME = 'persons' 
    AND INDEX_NAME = 'idx_member_no'
);

SET @sql = IF(@index_exists = 0,
    'CREATE INDEX idx_member_no ON persons(member_no);',
    'SELECT "idx_member_no 索引已存在，跳過創建" AS message;'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- 步驟 2: 添加 URL 權限配置
-- ============================================
-- 確保 url_permissions 表存在（如果不存在，請先執行 church-security-tables.sql）

-- 插入簽到系統的 URL 權限配置
INSERT IGNORE INTO url_permissions (
    url_pattern, 
    http_method, 
    required_role, 
    required_permission, 
    is_public, 
    is_active, 
    order_index, 
    description
) VALUES 
-- 公開 API（會眾自助簽到，無需認證）
('/api/church/checkin/public/sessions/*', 'GET', NULL, NULL, 1, 1, 99, '取得場次資訊 - 公開訪問'),
('/api/church/checkin/public/sessions/*/token', 'GET', NULL, NULL, 1, 1, 100, '取得簽到短效 token - 公開訪問'),
('/api/church/checkin/public/sessions/*/checkin', 'POST', NULL, NULL, 1, 1, 101, '會眾自助簽到 - 公開訪問'),

-- 場次管理 API（同工後台，需要認證）
('/api/church/checkin/admin/sessions', 'GET', NULL, NULL, 0, 1, 200, '取得所有場次列表 - 需要認證'),
('/api/church/checkin/admin/sessions', 'POST', NULL, NULL, 0, 1, 202, '新增場次 - 需要認證'),
('/api/church/checkin/admin/sessions/*', 'GET', NULL, NULL, 0, 1, 201, '取得單一場次 - 需要認證'),
('/api/church/checkin/admin/sessions/*', 'PUT', NULL, NULL, 0, 1, 203, '更新場次 - 需要認證'),
('/api/church/checkin/admin/sessions/*', 'DELETE', NULL, NULL, 0, 1, 204, '刪除場次 - 需要認證'),

-- 場次查詢和統計 API
('/api/church/checkin/admin/sessions/today', 'GET', NULL, NULL, 0, 1, 210, '取得今日場次列表 - 需要認證'),
('/api/church/checkin/admin/sessions/*/stats', 'GET', NULL, NULL, 0, 1, 211, '取得場次統計 - 需要認證'),
('/api/church/checkin/admin/sessions/*/checkins', 'GET', NULL, NULL, 0, 1, 212, '取得場次簽到名單 - 需要認證'),
('/api/church/checkin/admin/sessions/*/checkins/export.csv', 'GET', NULL, NULL, 0, 1, 213, '匯出場次簽到名單 CSV - 需要認證'),
('/api/church/checkin/admin/sessions/*/checkins/export.xlsx', 'GET', NULL, NULL, 0, 1, 214, '匯出場次簽到名單 Excel - 需要認證'),
('/api/church/checkin/admin/sessions/*/checkins/*/cancel', 'PATCH', NULL, NULL, 0, 1, 215, '取消簽到記錄 - 需要認證'),
('/api/church/checkin/admin/sessions/*/checkins/*/restore', 'PATCH', NULL, NULL, 0, 1, 216, '恢復簽到記錄 - 需要認證'),
('/api/church/checkin/admin/sessions/*/checkins/*', 'DELETE', NULL, NULL, 0, 1, 217, '刪除簽到記錄 - 需要認證'),
('/api/church/checkin/admin/sessions/*/unchecked-persons', 'GET', NULL, NULL, 0, 1, 218, '取得尚未簽到人員列表 - 需要認證'),
('/api/church/checkin/admin/sessions/*/batch-checkin', 'POST', NULL, NULL, 0, 1, 219, '批量補登 - 需要認證'),

-- 補登管理 API（需要認證）
('/api/church/checkin/admin/manual-checkins', 'GET', NULL, NULL, 0, 1, 300, '取得補登稽核列表 - 需要認證'),
('/api/church/checkin/admin/manual-checkins', 'POST', NULL, NULL, 0, 1, 301, '新增補登 - 需要認證'),
('/api/church/checkin/admin/manual-checkins/*/cancel', 'PATCH', NULL, NULL, 0, 1, 302, '取消補登 - 需要認證'),
('/api/church/checkin/admin/manual-checkins/export.csv', 'GET', NULL, NULL, 0, 1, 303, '匯出補登稽核 CSV - 需要認證'),
('/api/church/checkin/admin/manual-checkins/export.xlsx', 'GET', NULL, NULL, 0, 1, 304, '匯出補登稽核 Excel - 需要認證');

-- ============================================
-- 步驟 3: 添加後台菜單配置
-- ============================================
-- 確保 menu_items 表存在（如果不存在，請先執行 church-security-tables.sql）

-- 插入主菜單項「簽到管理」（父菜單）
INSERT IGNORE INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, menu_type, required_permission, is_active)
VALUES ('ADMIN_CHECKIN', '簽到管理', '📋', NULL, NULL, 5, 'admin', NULL, 1);

-- 插入子菜單項「管理場次」
INSERT IGNORE INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, menu_type, required_permission, is_active)
SELECT 'ADMIN_CHECKIN_SESSIONS', '管理場次', '📅', '/checkin/admin/sessions',
    (SELECT id FROM menu_items WHERE menu_code = 'ADMIN_CHECKIN' LIMIT 1), 0, 'admin', NULL, 1
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE menu_code = 'ADMIN_CHECKIN_SESSIONS');

-- 插入子菜單項「補登稽核」
INSERT IGNORE INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, menu_type, required_permission, is_active)
SELECT 'ADMIN_CHECKIN_MANUAL', '補登稽核', '✏️', '/checkin/admin/manual',
    (SELECT id FROM menu_items WHERE menu_code = 'ADMIN_CHECKIN' LIMIT 1), 1, 'admin', NULL, 1
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE menu_code = 'ADMIN_CHECKIN_MANUAL');

-- ============================================
-- 步驟 4: 更新菜單 URL（確保正確）
-- ============================================
-- 更新主菜單項：將 URL 設為 NULL，使其成為父菜單
UPDATE menu_items 
SET url = NULL, menu_name = '簽到管理'
WHERE menu_code = 'ADMIN_CHECKIN';

-- 更新「管理場次」的 URL
UPDATE menu_items 
SET url = '/checkin/admin/sessions'
WHERE menu_code = 'ADMIN_CHECKIN_SESSIONS';

-- 更新「補登稽核」的 order_index
UPDATE menu_items 
SET order_index = 1
WHERE menu_code = 'ADMIN_CHECKIN_MANUAL';

-- ============================================
-- 顯示配置結果
-- ============================================

-- 顯示 URL 權限配置
SELECT 
    'URL 權限配置' AS section,
    COUNT(*) AS total_count
FROM url_permissions
WHERE url_pattern LIKE '/api/church/checkin/%'
AND is_active = 1;

-- 顯示菜單配置
SELECT 
    '菜單配置' AS section,
    COUNT(*) AS total_count
FROM menu_items
WHERE menu_code LIKE 'ADMIN_CHECKIN%'
AND is_active = 1;

-- 顯示詳細的 URL 權限列表
SELECT 
    'URL 權限詳情' AS section,
    id,
    url_pattern,
    http_method,
    CASE WHEN is_public = 1 THEN '公開' ELSE '需認證' END AS access_type,
    description,
    order_index
FROM url_permissions
WHERE url_pattern LIKE '/api/church/checkin/%'
AND is_active = 1
ORDER BY order_index, url_pattern;

-- 顯示詳細的菜單列表
SELECT 
    '菜單詳情' AS section,
    id,
    menu_code,
    menu_name,
    icon,
    url,
    parent_id,
    order_index,
    menu_type,
    is_active
FROM menu_items
WHERE menu_code LIKE 'ADMIN_CHECKIN%'
AND is_active = 1
ORDER BY order_index, menu_code;

-- ============================================
-- 完成訊息
-- ============================================
SELECT 
    '✅ 簽到系統配置完成！' AS status,
    '請確認上述配置是否正確' AS message;


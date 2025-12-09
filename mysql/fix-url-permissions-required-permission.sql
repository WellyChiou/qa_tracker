-- ============================================
-- 修復 URL 權限表中的 required_permission 字段
-- ============================================
-- 此腳本用於將 url_permissions 表中的 required_permission 從權限 ID 改回權限代碼
-- 解決 403 Forbidden 錯誤
-- ============================================

USE church;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================
-- 第一步：確保所有權限都存在
-- ============================================
-- 如果權限不存在，創建它們
INSERT IGNORE INTO permissions (permission_code, permission_name, resource, action, description) VALUES
('CHURCH_ADMIN', '教會管理員權限', 'church', 'admin', '可以存取所有教會管理功能，包括用戶、角色、權限、菜單、URL權限等管理'),
('SERVICE_SCHEDULE_EDIT', '編輯服事表', 'service_schedule', 'edit', '可以新增、修改、刪除服事表'),
('PERSON_EDIT', '編輯人員', 'person', 'edit', '可以新增、修改、刪除人員'),
('POSITION_EDIT', '編輯崗位', 'position', 'edit', '可以新增、修改、刪除崗位');

-- ============================================
-- 第二步：更新 url_permissions 表
-- ============================================
-- 將權限 ID 轉換為權限代碼（CODE）

-- 更新 CHURCH_ADMIN 相關的權限（從 ID 轉換為 CODE）
UPDATE url_permissions up
INNER JOIN permissions p ON up.required_permission = CAST(p.id AS CHAR) AND p.permission_code = 'CHURCH_ADMIN'
SET up.required_permission = 'CHURCH_ADMIN'
WHERE up.required_permission REGEXP '^[0-9]+$'
   OR (up.required_permission IN ('PERM_CHURCH_ADMIN', 'CHURCH_ADMIN'))
   OR (up.required_permission NOT REGEXP '^[0-9]+$' 
       AND up.url_pattern LIKE '/api/church/admin/%')
   OR (up.required_permission NOT REGEXP '^[0-9]+$' 
       AND up.url_pattern = '/api/church/menus' 
       AND up.http_method IN ('GET', 'POST', 'PUT', 'DELETE'))
   OR (up.required_permission NOT REGEXP '^[0-9]+$' 
       AND up.url_pattern LIKE '/api/church/menus/*' 
       AND up.url_pattern NOT IN ('/api/church/menus/admin', '/api/church/menus/dashboard', '/api/church/menus/frontend'))
;

-- 如果上面的 JOIN 更新沒有匹配到，直接更新為 CHURCH_ADMIN
UPDATE url_permissions 
SET required_permission = 'CHURCH_ADMIN'
WHERE required_permission REGEXP '^[0-9]+$'
   AND url_pattern LIKE '/api/church/admin/%'
   AND NOT EXISTS (
       SELECT 1 FROM permissions p 
       WHERE p.id = CAST(url_permissions.required_permission AS UNSIGNED) 
       AND p.permission_code != 'CHURCH_ADMIN'
   );

-- 更新 SERVICE_SCHEDULE_EDIT 相關的權限（從 ID 轉換為 CODE）
UPDATE url_permissions up
INNER JOIN permissions p ON up.required_permission = CAST(p.id AS CHAR) AND p.permission_code = 'SERVICE_SCHEDULE_EDIT'
SET up.required_permission = 'SERVICE_SCHEDULE_EDIT'
WHERE up.required_permission REGEXP '^[0-9]+$'
   OR (up.required_permission = 'SERVICE_SCHEDULE_EDIT')
   OR (up.required_permission NOT REGEXP '^[0-9]+$' 
       AND up.url_pattern LIKE '/api/church/service-schedules%' 
       AND up.http_method IN ('POST', 'PUT', 'DELETE'));

-- 更新 PERSON_EDIT 相關的權限（從 ID 轉換為 CODE）
UPDATE url_permissions up
INNER JOIN permissions p ON up.required_permission = CAST(p.id AS CHAR) AND p.permission_code = 'PERSON_EDIT'
SET up.required_permission = 'PERSON_EDIT'
WHERE up.required_permission REGEXP '^[0-9]+$'
   OR (up.required_permission = 'PERSON_EDIT')
   OR (up.required_permission NOT REGEXP '^[0-9]+$' 
       AND up.url_pattern LIKE '/api/church/persons%' 
       AND up.http_method IN ('POST', 'PUT', 'DELETE'));

-- 更新 POSITION_EDIT 相關的權限（從 ID 轉換為 CODE）
UPDATE url_permissions up
INNER JOIN permissions p ON up.required_permission = CAST(p.id AS CHAR) AND p.permission_code = 'POSITION_EDIT'
SET up.required_permission = 'POSITION_EDIT'
WHERE up.required_permission REGEXP '^[0-9]+$'
   OR (up.required_permission = 'POSITION_EDIT')
   OR (up.required_permission NOT REGEXP '^[0-9]+$' 
       AND up.url_pattern LIKE '/api/church/positions%' 
       AND up.http_method IN ('POST', 'PUT', 'DELETE'));

-- ============================================
-- 第三步：顯示更新結果
-- ============================================
SELECT 
    'URL 權限更新完成' AS message,
    COUNT(*) AS total_url_permissions,
    SUM(CASE WHEN required_permission REGEXP '^[0-9]+$' THEN 1 ELSE 0 END) AS still_using_id_count,
    SUM(CASE WHEN required_permission NOT REGEXP '^[0-9]+$' AND required_permission IS NOT NULL THEN 1 ELSE 0 END) AS using_code_count
FROM url_permissions;

-- ============================================
-- 第四步：顯示需要 CHURCH_ADMIN 權限的 URL 配置
-- ============================================
SELECT 
    id,
    url_pattern,
    http_method,
    required_permission AS '權限代碼',
    description
FROM url_permissions
WHERE url_pattern IN (
    '/api/church/admin/users',
    '/api/church/admin/roles',
    '/api/church/admin/permissions'
)
ORDER BY url_pattern, http_method;

-- ============================================
-- 第五步：檢查是否還有使用 ID 的配置（需要轉換）
-- ============================================
SELECT 
    '仍使用 ID 的配置（需要轉換）' AS message,
    id,
    url_pattern,
    http_method,
    required_permission AS '權限ID',
    (SELECT permission_code FROM permissions WHERE id = CAST(url_permissions.required_permission AS UNSIGNED)) AS '對應權限代碼',
    description
FROM url_permissions
WHERE required_permission REGEXP '^[0-9]+$'
ORDER BY url_pattern;

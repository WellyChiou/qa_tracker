-- ============================================
-- 將 URL 權限表中的 required_permission 從 ID 轉換回 CODE
-- ============================================
-- 此腳本用於將 url_permissions 表中的 required_permission 從權限 ID 改回權限代碼
-- ============================================

USE church;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================
-- 第一步：更新 required_permission（從 ID 轉換為 CODE）
-- ============================================
-- 將權限 ID 轉換為權限代碼
UPDATE url_permissions up
INNER JOIN permissions p ON up.required_permission = CAST(p.id AS CHAR)
SET up.required_permission = p.permission_code
WHERE up.required_permission REGEXP '^[0-9]+$';

-- ============================================
-- 第二步：顯示轉換結果
-- ============================================
SELECT 
    'URL 權限轉換完成' AS message,
    COUNT(*) AS total_url_permissions,
    SUM(CASE WHEN required_permission REGEXP '^[0-9]+$' THEN 1 ELSE 0 END) AS still_using_id_count,
    SUM(CASE WHEN required_permission NOT REGEXP '^[0-9]+$' AND required_permission IS NOT NULL THEN 1 ELSE 0 END) AS using_code_count
FROM url_permissions;

-- ============================================
-- 第三步：顯示轉換後的配置（需要 CHURCH_ADMIN 權限的）
-- ============================================
SELECT 
    id,
    url_pattern,
    http_method,
    required_permission AS '權限代碼',
    description
FROM url_permissions
WHERE required_permission = 'CHURCH_ADMIN'
   OR url_pattern LIKE '/api/church/admin/%'
ORDER BY url_pattern, http_method
LIMIT 20;

-- ============================================
-- 第四步：檢查是否有無法轉換的記錄
-- ============================================
SELECT 
    '無法轉換的記錄（權限 ID 不存在）' AS message,
    id,
    url_pattern,
    http_method,
    required_permission AS '權限ID',
    description
FROM url_permissions
WHERE required_permission REGEXP '^[0-9]+$'
  AND NOT EXISTS (
      SELECT 1 FROM permissions WHERE id = CAST(url_permissions.required_permission AS UNSIGNED)
  );

-- ============================================
-- 驗證所有權限代碼是否存在
-- ============================================
-- 檢查 menu_items 和 url_permissions 中引用的所有權限代碼
-- 是否都存在於 permissions 表中
-- ============================================

USE church;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================
-- 1. 檢查 menu_items 中引用的權限是否存在
-- ============================================
SELECT 
    'menu_items 權限檢查' AS check_type,
    mi.menu_code,
    mi.menu_name,
    mi.required_permission,
    CASE 
        WHEN p.id IS NULL THEN '❌ 不存在'
        ELSE '✅ 存在'
    END AS status,
    p.permission_name AS permission_name_in_db
FROM menu_items mi
LEFT JOIN permissions p ON p.permission_code = mi.required_permission
WHERE mi.required_permission IS NOT NULL
  AND mi.required_permission != ''
ORDER BY 
    CASE WHEN p.id IS NULL THEN 0 ELSE 1 END,
    mi.menu_code;

-- ============================================
-- 2. 檢查 url_permissions 中引用的權限是否存在
-- ============================================
SELECT 
    'url_permissions 權限檢查' AS check_type,
    up.url_pattern,
    up.http_method,
    up.required_permission,
    CASE 
        WHEN p.id IS NULL THEN '❌ 不存在'
        ELSE '✅ 存在'
    END AS status,
    p.permission_name AS permission_name_in_db
FROM url_permissions up
LEFT JOIN permissions p ON p.permission_code = up.required_permission
WHERE up.required_permission IS NOT NULL
  AND up.required_permission != ''
ORDER BY 
    CASE WHEN p.id IS NULL THEN 0 ELSE 1 END,
    up.order_index,
    up.url_pattern;

-- ============================================
-- 3. 列出所有缺失的權限代碼（需要創建）
-- ============================================
SELECT DISTINCT
    '需要創建的權限' AS action,
    mi.required_permission AS missing_permission_code
FROM menu_items mi
WHERE mi.required_permission IS NOT NULL
  AND mi.required_permission != ''
  AND NOT EXISTS (
      SELECT 1 
      FROM permissions p 
      WHERE p.permission_code = mi.required_permission
  )

UNION

SELECT DISTINCT
    '需要創建的權限' AS action,
    up.required_permission AS missing_permission_code
FROM url_permissions up
WHERE up.required_permission IS NOT NULL
  AND up.required_permission != ''
  AND NOT EXISTS (
      SELECT 1 
      FROM permissions p 
      WHERE p.permission_code = up.required_permission
  )
ORDER BY missing_permission_code;


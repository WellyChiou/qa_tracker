-- ============================================
-- 檢查權限一致性
-- ============================================
-- 檢查 menu_items 和 url_permissions 中的權限代碼是否存在於 permissions 表
-- ============================================

USE church;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================
-- 1. 檢查 menu_items 中引用的權限是否存在
-- ============================================
SELECT 
    'menu_items 中不存在的權限' AS check_type,
    mi.menu_code,
    mi.menu_name,
    mi.required_permission AS missing_permission
FROM menu_items mi
WHERE mi.required_permission IS NOT NULL
  AND mi.required_permission != ''
  AND NOT EXISTS (
      SELECT 1 
      FROM permissions p 
      WHERE p.permission_code = mi.required_permission
  )
ORDER BY mi.menu_code;

-- ============================================
-- 2. 檢查 url_permissions 中引用的權限是否存在
-- ============================================
SELECT 
    'url_permissions 中不存在的權限' AS check_type,
    up.url_pattern,
    up.http_method,
    up.required_permission AS missing_permission
FROM url_permissions up
WHERE up.required_permission IS NOT NULL
  AND up.required_permission != ''
  AND NOT EXISTS (
      SELECT 1 
      FROM permissions p 
      WHERE p.permission_code = up.required_permission
  )
ORDER BY up.order_index, up.url_pattern;

-- ============================================
-- 3. 列出所有需要的權限代碼（從 menu_items）
-- ============================================
SELECT DISTINCT
    'menu_items 需要的權限' AS source,
    required_permission AS permission_code
FROM menu_items
WHERE required_permission IS NOT NULL
  AND required_permission != ''
ORDER BY required_permission;

-- ============================================
-- 4. 列出所有需要的權限代碼（從 url_permissions）
-- ============================================
SELECT DISTINCT
    'url_permissions 需要的權限' AS source,
    required_permission AS permission_code
FROM url_permissions
WHERE required_permission IS NOT NULL
  AND required_permission != ''
ORDER BY required_permission;


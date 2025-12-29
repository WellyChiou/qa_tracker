-- 檢查 url_permissions 表中的重複資料
-- 比較兩種判斷方式

USE church;

-- ============================================
-- 方式 1: 基於 url_pattern, http_method, description（您的查詢方式）
-- ============================================
-- 問題：這種方式會漏掉那些 url_pattern 和 http_method 相同但 description 不同的重複記錄
SELECT 
    '方式 1: 基於 url_pattern, http_method, description' AS query_type,
    url_pattern, 
    http_method,
    description,
    COUNT(*) as duplicate_count,
    GROUP_CONCAT(id ORDER BY id) as ids
FROM url_permissions
GROUP BY url_pattern, http_method, description
HAVING COUNT(*) > 1
ORDER BY duplicate_count DESC, url_pattern
LIMIT 10;

-- ============================================
-- 方式 2: 基於 url_pattern, http_method（推薦的正確方式）
-- ============================================
-- 正確：同一個 URL 和 HTTP 方法應該只有一個權限配置
-- 即使 description 不同，也應該視為重複
SELECT 
    '方式 2: 基於 url_pattern, http_method（推薦）' AS query_type,
    url_pattern, 
    http_method,
    COUNT(*) as duplicate_count,
    GROUP_CONCAT(id ORDER BY id) as ids,
    GROUP_CONCAT(DISTINCT description ORDER BY id SEPARATOR ' | ') as descriptions
FROM url_permissions
GROUP BY url_pattern, http_method
HAVING COUNT(*) > 1
ORDER BY duplicate_count DESC, url_pattern
LIMIT 10;

-- ============================================
-- 方式 3: 找出方式 1 漏掉的重複（description 不同但實際是重複的）
-- ============================================
-- 這些是方式 1 查不到但方式 2 能查到的重複記錄
SELECT 
    '方式 3: 方式 1 漏掉的重複（description 不同）' AS query_type,
    url_pattern, 
    http_method,
    COUNT(*) as duplicate_count,
    GROUP_CONCAT(id ORDER BY id) as ids,
    GROUP_CONCAT(DISTINCT description ORDER BY id SEPARATOR ' | ') as descriptions
FROM url_permissions
GROUP BY url_pattern, http_method
HAVING COUNT(*) > 1
  AND COUNT(DISTINCT description) > 1  -- description 不同的重複
ORDER BY duplicate_count DESC, url_pattern
LIMIT 10;

-- ============================================
-- 方式 4: 基於核心業務欄位（最精準，推薦）
-- ============================================
-- 排除的欄位：
--   - id: 主鍵，不參與判斷
--   - created_at, updated_at: 時間欄位，會導致相同的記錄被認為不同
--   - is_active: 可能會有停用/啟用的狀態變化，但實際上是同一個權限配置
--   - order_index: 排序順序可能會調整，但實際上是同一個權限配置
SELECT 
    '方式 4: 基於核心業務欄位（最精準，推薦）' AS query_type,
    url_pattern,
    http_method,
    description,
    is_public,
    required_permission,
    required_role,
    COUNT(*) as duplicate_count,
    GROUP_CONCAT(id ORDER BY id) as ids,
    GROUP_CONCAT(CONCAT('id:', id, ',active:', is_active, ',order:', order_index) ORDER BY id SEPARATOR ' | ') as details
FROM url_permissions
GROUP BY url_pattern, http_method, description, is_public, required_permission, required_role
HAVING COUNT(*) > 1
ORDER BY duplicate_count DESC, url_pattern
LIMIT 10;

-- ============================================
-- 方式 5: 包含 updated_at（僅供參考）
-- ============================================
-- 如果包含 updated_at，可能會漏掉一些重複（因為時間不同）
-- 但如果您想找出完全相同的記錄（包括時間），可以使用這個查詢
-- SELECT 
--     '方式 5: 包含 updated_at' AS query_type,
--     url_pattern,
--     http_method,
--     description,
--     is_public,
--     is_active,
--     order_index,
--     required_permission,
--     required_role,
--     updated_at,
--     COUNT(*) as duplicate_count,
--     GROUP_CONCAT(id ORDER BY id) as ids
-- FROM url_permissions
-- GROUP BY url_pattern, http_method, description, is_public, is_active, order_index, required_permission, required_role, updated_at
-- HAVING COUNT(*) > 1
-- ORDER BY duplicate_count DESC, url_pattern
-- LIMIT 10;

-- ============================================
-- 結論
-- ============================================
-- 推薦使用方式 4（基於核心業務欄位）：
-- 1. 最精準：找出核心欄位都完全相同的重複記錄
-- 2. 排除時間欄位：updated_at 和 created_at 會因為時間不同導致相同的記錄被認為不同
-- 3. 排除狀態欄位：is_active 可能會有停用/啟用的狀態變化，但實際上是同一個權限配置
-- 4. 排除排序欄位：order_index 排序順序可能會調整，但實際上是同一個權限配置
-- 5. 核心欄位：url_pattern, http_method, description, is_public, required_permission, required_role
-- 
-- 判斷重複的核心欄位：
--   - url_pattern: URL 模式
--   - http_method: HTTP 方法
--   - description: 描述
--   - is_public: 是否公開
--   - required_permission: 需要的權限
--   - required_role: 需要的角色


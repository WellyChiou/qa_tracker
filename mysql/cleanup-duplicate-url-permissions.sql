-- 清理 url_permissions 表中的重複資料
-- 使用前請先備份資料庫！

USE church;

-- ============================================
-- 步驟 1: 檢查重複資料（基於核心業務欄位）
-- ============================================
-- 這是最精準的方式：找出核心欄位都完全相同的重複記錄
-- 排除的欄位：
--   - id: 主鍵，不參與判斷
--   - created_at, updated_at: 時間欄位，會導致相同的記錄被認為不同
--   - is_active: 可能會有停用/啟用的狀態變化，但實際上是同一個權限配置
--   - order_index: 排序順序可能會調整，但實際上是同一個權限配置
SELECT 
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
ORDER BY duplicate_count DESC, url_pattern;

-- ============================================
-- 步驟 1.1: 檢查重複資料（包含 updated_at，僅供參考）
-- ============================================
-- 如果包含 updated_at，可能會漏掉一些重複（因為時間不同）
-- 但如果您想找出完全相同的記錄（包括時間），可以使用這個查詢
-- SELECT 
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
-- ORDER BY duplicate_count DESC, url_pattern;

-- ============================================
-- 步驟 2: 查看重複資料的詳細資訊（可選）
-- ============================================
-- 查看所有重複記錄的完整資訊
SELECT up.* 
FROM url_permissions up
INNER JOIN (
    SELECT url_pattern, http_method
    FROM url_permissions
    GROUP BY url_pattern, http_method
    HAVING COUNT(*) > 1
) dup ON up.url_pattern = dup.url_pattern 
    AND (up.http_method = dup.http_method OR (up.http_method IS NULL AND dup.http_method IS NULL))
ORDER BY up.url_pattern, up.http_method, up.id;

-- ============================================
-- 步驟 3: 移除重複資料（保留 id 最小的記錄）
-- ============================================
-- 注意：此操作會永久刪除重複的記錄，請先確認步驟 1 和步驟 2 的結果
-- 建議：先執行步驟 1 和 2 查看重複資料，確認無誤後再執行此步驟

-- 方法 1: 基於核心業務欄位移除重複（最精準，推薦）
-- 保留 id 最小的記錄，刪除其他完全相同的重複記錄
-- 排除的欄位：
--   - id: 主鍵，不參與判斷
--   - created_at, updated_at: 時間欄位，會導致相同的記錄被認為不同
--   - is_active: 可能會有停用/啟用的狀態變化，但實際上是同一個權限配置
--   - order_index: 排序順序可能會調整，但實際上是同一個權限配置
DELETE up1 FROM url_permissions up1
INNER JOIN url_permissions up2 
WHERE up1.id > up2.id 
  AND up1.url_pattern = up2.url_pattern 
  AND (up1.http_method = up2.http_method OR (up1.http_method IS NULL AND up2.http_method IS NULL))
  AND (up1.description = up2.description OR (up1.description IS NULL AND up2.description IS NULL))
  AND up1.is_public = up2.is_public
  AND (up1.required_permission = up2.required_permission OR (up1.required_permission IS NULL AND up2.required_permission IS NULL))
  AND (up1.required_role = up2.required_role OR (up1.required_role IS NULL AND up2.required_role IS NULL));

-- 方法 2: 基於 url_pattern 和 http_method 移除重複（較寬鬆）
-- 如果您的需求是只要 url_pattern 和 http_method 相同就視為重複，可以使用這個方法
-- DELETE up1 FROM url_permissions up1
-- INNER JOIN url_permissions up2 
-- WHERE up1.id > up2.id 
--   AND up1.url_pattern = up2.url_pattern 
--   AND (up1.http_method = up2.http_method OR (up1.http_method IS NULL AND up2.http_method IS NULL));

-- ============================================
-- 步驟 4: 驗證清理結果
-- ============================================
-- 再次執行步驟 1 的查詢，應該沒有結果了
SELECT 
    url_pattern,
    http_method,
    description,
    is_public,
    required_permission,
    required_role,
    COUNT(*) as duplicate_count
FROM url_permissions
GROUP BY url_pattern, http_method, description, is_public, required_permission, required_role
HAVING COUNT(*) > 1;

-- ============================================
-- 步驟 5: 建議添加唯一約束（可選）
-- ============================================
-- 為了防止未來再次出現重複資料，可以添加唯一約束
-- 注意：如果表中還有重複資料，此操作會失敗

-- 先檢查是否已有唯一約束
SELECT CONSTRAINT_NAME, CONSTRAINT_TYPE
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
WHERE TABLE_SCHEMA = 'church' 
  AND TABLE_NAME = 'url_permissions' 
  AND CONSTRAINT_TYPE = 'UNIQUE';

-- 如果沒有重複資料，可以執行以下語句添加唯一約束：
-- 注意：由於 http_method 可能為 NULL，MySQL 的 UNIQUE 約束對 NULL 值的處理
-- 在 MySQL 8.0.13+ 中，多個 NULL 值被視為不同的值，所以可以添加唯一約束
-- 但在較舊版本中，NULL 值可能被視為相同，需要特別處理

-- 添加唯一約束（僅在確認沒有重複資料後執行）
-- ALTER TABLE url_permissions 
-- ADD UNIQUE KEY uk_url_pattern_method (url_pattern, http_method);

SELECT '清理完成！請檢查步驟 4 的結果確認沒有重複資料。' AS message;


USE church;

-- 修正 Church 備份下載 URL 權限：
-- 既有規則為 /api/church/admin/backups/download/*，
-- 實際 endpoint 為 /api/church/admin/backups/download?path=...
-- 會導致 URL permission filter 命中 URL 但 method 不精準時回 403。

UPDATE url_permissions
SET url_pattern = '/api/church/admin/backups/download',
    updated_at = NOW()
WHERE url_pattern = '/api/church/admin/backups/download/*'
  AND http_method = 'GET';

-- 若既有資料不存在（例如某些環境已被手動清理），補最小規則
INSERT INTO url_permissions (
    url_pattern, http_method, is_public, required_role, required_permission, is_active, order_index, description
)
SELECT
    '/api/church/admin/backups/download', 'GET', 0, 'CHURCH_ADMIN', NULL, 1, 312,
    '下載備份檔案（需教會管理員權限）'
WHERE NOT EXISTS (
    SELECT 1
    FROM url_permissions
    WHERE url_pattern = '/api/church/admin/backups/download'
      AND http_method = 'GET'
);

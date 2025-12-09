-- ============================================
-- 清理和重新整理 URL 權限表
-- ============================================
-- 此腳本用於：
-- 1. 刪除重複的記錄
-- 2. 重新整理 order_index，確保更具體的路徑優先匹配
-- 3. 修正錯誤的權限配置
-- 4. 確保所有配置都正確
-- ============================================

USE church;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================
-- 第一步：刪除重複的記錄（保留 ID 較小的）
-- ============================================
DELETE t1 FROM url_permissions t1
INNER JOIN url_permissions t2 
WHERE t1.id > t2.id 
  AND t1.url_pattern = t2.url_pattern 
  AND (t1.http_method = t2.http_method OR (t1.http_method IS NULL AND t2.http_method IS NULL));

-- ============================================
-- 第二步：修正錯誤的配置
-- ============================================
-- 修正 /api/church/admin/url-permissions/active 應該是公開的
UPDATE url_permissions 
SET is_public = 1, required_permission = NULL
WHERE url_pattern = '/api/church/admin/url-permissions/active';

-- 修正 /api/church/menus/frontend 應該是公開的（如果 order_index 不對）
UPDATE url_permissions 
SET is_public = 1, required_permission = NULL, order_index = 105
WHERE url_pattern = '/api/church/menus/frontend';

-- 修正 /api/church/menus/admin 和 /api/church/menus/dashboard 只需要認證，不需要 PERM_CHURCH_ADMIN
UPDATE url_permissions 
SET required_permission = NULL
WHERE url_pattern IN ('/api/church/menus/admin', '/api/church/menus/dashboard');

-- ============================================
-- 第三步：重新整理 order_index
-- ============================================
-- 公開訪問的 API（order_index: 0-99）
UPDATE url_permissions SET order_index = 0 WHERE url_pattern = '/api/church/auth/**' AND is_public = 1;
UPDATE url_permissions SET order_index = 100 WHERE url_pattern = '/api/church/public/church-info' AND is_public = 1;
UPDATE url_permissions SET order_index = 101 WHERE url_pattern = '/api/church/public/about-info' AND is_public = 1;
UPDATE url_permissions SET order_index = 102 WHERE url_pattern = '/api/church/public/activities' AND is_public = 1;
UPDATE url_permissions SET order_index = 103 WHERE url_pattern = '/api/church/public/contact-submissions' AND is_public = 1 AND http_method = 'POST';
UPDATE url_permissions SET order_index = 104 WHERE url_pattern = '/api/church/menus/frontend' AND is_public = 1;
UPDATE url_permissions SET order_index = 107 WHERE url_pattern = '/api/church/service-schedules' AND is_public = 1 AND http_method = 'GET';
UPDATE url_permissions SET order_index = 108 WHERE url_pattern = '/api/church/service-schedules/**' AND is_public = 1 AND http_method = 'GET';
UPDATE url_permissions SET order_index = 109 WHERE url_pattern = '/api/church/persons' AND is_public = 1 AND http_method = 'GET';
UPDATE url_permissions SET order_index = 110 WHERE url_pattern = '/api/church/positions' AND is_public = 1 AND http_method = 'GET';
UPDATE url_permissions SET order_index = 111 WHERE url_pattern = '/api/church/positions/active' AND is_public = 1;
UPDATE url_permissions SET order_index = 112 WHERE url_pattern = '/api/church/positions/config/full' AND is_public = 1;
UPDATE url_permissions SET order_index = 113 WHERE url_pattern = '/api/church/positions/config/**' AND is_public = 1;
UPDATE url_permissions SET order_index = 114 WHERE url_pattern = '/api/church/positions/*/persons' AND is_public = 1;
UPDATE url_permissions SET order_index = 115 WHERE url_pattern = '/api/church/admin/url-permissions/active' AND is_public = 1;

-- 需要認證的 API（order_index: 120-129）
-- 注意：這些路徑應該在 /api/church/menus/* 之前匹配，所以 order_index 要小於 270
UPDATE url_permissions SET order_index = 120 WHERE url_pattern = '/api/church/menus/admin' AND is_public = 0 AND required_permission IS NULL;
UPDATE url_permissions SET order_index = 121 WHERE url_pattern = '/api/church/menus/dashboard' AND is_public = 0 AND required_permission IS NULL;

-- 需要編輯權限的 API（order_index: 130-149）
UPDATE url_permissions SET order_index = 130 WHERE url_pattern = '/api/church/service-schedules' AND http_method = 'POST' AND required_permission = 'SERVICE_SCHEDULE_EDIT';
UPDATE url_permissions SET order_index = 131 WHERE url_pattern = '/api/church/service-schedules/**' AND http_method = 'PUT' AND required_permission = 'SERVICE_SCHEDULE_EDIT';
UPDATE url_permissions SET order_index = 132 WHERE url_pattern = '/api/church/service-schedules/**' AND http_method = 'DELETE' AND required_permission = 'SERVICE_SCHEDULE_EDIT';
UPDATE url_permissions SET order_index = 140 WHERE url_pattern = '/api/church/persons' AND http_method = 'POST' AND required_permission = 'PERSON_EDIT';
UPDATE url_permissions SET order_index = 141 WHERE url_pattern = '/api/church/persons/**' AND http_method = 'PUT' AND required_permission = 'PERSON_EDIT';
UPDATE url_permissions SET order_index = 142 WHERE url_pattern = '/api/church/persons/**' AND http_method = 'DELETE' AND required_permission = 'PERSON_EDIT';
UPDATE url_permissions SET order_index = 150 WHERE url_pattern = '/api/church/positions' AND http_method = 'POST' AND required_permission = 'POSITION_EDIT';
UPDATE url_permissions SET order_index = 151 WHERE url_pattern = '/api/church/positions/**' AND http_method = 'PUT' AND required_permission = 'POSITION_EDIT';
UPDATE url_permissions SET order_index = 152 WHERE url_pattern = '/api/church/positions/**' AND http_method = 'DELETE' AND required_permission = 'POSITION_EDIT';

-- 需要 CHURCH_ADMIN 的 API（order_index: 200+）
-- 注意：required_permission 使用權限代碼（CODE），過濾器會自動加上 PERM_ 前綴
-- 聯絡表單管理（200-204）
UPDATE url_permissions SET order_index = 200 WHERE url_pattern = '/api/church/admin/contact-submissions' AND http_method = 'GET' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 201 WHERE url_pattern = '/api/church/admin/contact-submissions/*' AND http_method = 'GET' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 202 WHERE url_pattern = '/api/church/admin/contact-submissions/*/read' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 203 WHERE url_pattern = '/api/church/admin/contact-submissions/*' AND http_method = 'DELETE' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 204 WHERE url_pattern = '/api/church/admin/contact-submissions/stats' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');

-- 教會資訊管理（210-213）
UPDATE url_permissions SET order_index = 210 WHERE url_pattern = '/api/church/admin/church-info' AND http_method = 'GET' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 211 WHERE url_pattern = '/api/church/admin/church-info' AND http_method = 'POST' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 212 WHERE url_pattern = '/api/church/admin/church-info/*' AND http_method = 'GET' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 213 WHERE url_pattern = '/api/church/admin/church-info/batch' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');

-- 關於我們管理（220-224）
UPDATE url_permissions SET order_index = 220 WHERE url_pattern = '/api/church/admin/about-info' AND http_method = 'GET' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 221 WHERE url_pattern = '/api/church/admin/about-info' AND http_method = 'POST' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 222 WHERE url_pattern = '/api/church/admin/about-info/*' AND http_method = 'GET' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 223 WHERE url_pattern = '/api/church/admin/about-info/*' AND http_method = 'PUT' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 224 WHERE url_pattern = '/api/church/admin/about-info/*' AND http_method = 'DELETE' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');

-- 活動管理（230-234）
UPDATE url_permissions SET order_index = 230 WHERE url_pattern = '/api/church/admin/activities' AND http_method = 'GET' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 231 WHERE url_pattern = '/api/church/admin/activities' AND http_method = 'POST' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 232 WHERE url_pattern = '/api/church/admin/activities/*' AND http_method = 'GET' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 233 WHERE url_pattern = '/api/church/admin/activities/*' AND http_method = 'PUT' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 234 WHERE url_pattern = '/api/church/admin/activities/*' AND http_method = 'DELETE' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');

-- 用戶管理（240-246）
UPDATE url_permissions SET order_index = 240 WHERE url_pattern = '/api/church/admin/users' AND http_method = 'GET' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 241 WHERE url_pattern = '/api/church/admin/users' AND http_method = 'POST' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 242 WHERE url_pattern = '/api/church/admin/users/*' AND http_method = 'GET' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 243 WHERE url_pattern = '/api/church/admin/users/*' AND http_method = 'PUT' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 244 WHERE url_pattern = '/api/church/admin/users/*' AND http_method = 'DELETE' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 245 WHERE url_pattern = '/api/church/admin/users/*/roles' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 246 WHERE url_pattern = '/api/church/admin/users/*/permissions' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');

-- 角色管理（250-255）
UPDATE url_permissions SET order_index = 250 WHERE url_pattern = '/api/church/admin/roles' AND http_method = 'GET' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 251 WHERE url_pattern = '/api/church/admin/roles' AND http_method = 'POST' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 252 WHERE url_pattern = '/api/church/admin/roles/*' AND http_method = 'GET' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 253 WHERE url_pattern = '/api/church/admin/roles/*' AND http_method = 'PUT' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 254 WHERE url_pattern = '/api/church/admin/roles/*' AND http_method = 'DELETE' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 255 WHERE url_pattern = '/api/church/admin/roles/*/permissions' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');

-- 權限管理（260-265）
UPDATE url_permissions SET order_index = 260 WHERE url_pattern = '/api/church/admin/permissions' AND http_method = 'GET' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 261 WHERE url_pattern = '/api/church/admin/permissions' AND http_method = 'POST' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 262 WHERE url_pattern = '/api/church/admin/permissions/*' AND http_method = 'GET' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 263 WHERE url_pattern = '/api/church/admin/permissions/*' AND http_method = 'PUT' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 264 WHERE url_pattern = '/api/church/admin/permissions/*' AND http_method = 'DELETE' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 265 WHERE url_pattern = '/api/church/admin/permissions/resource/*' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');

-- 菜單管理（270-274）
-- 注意：/api/church/menus/admin 和 /api/church/menus/dashboard 已經在 order_index 120-121，這裡只處理需要 CHURCH_ADMIN 的
UPDATE url_permissions SET order_index = 270 WHERE url_pattern = '/api/church/menus' AND http_method = 'GET' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 271 WHERE url_pattern = '/api/church/menus' AND http_method = 'POST' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
-- /api/church/menus/* 應該在更具體的路徑之後，所以 order_index 較大
UPDATE url_permissions SET order_index = 272 WHERE url_pattern = '/api/church/menus/*' AND http_method = 'GET' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 273 WHERE url_pattern = '/api/church/menus/*' AND http_method = 'PUT' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 274 WHERE url_pattern = '/api/church/menus/*' AND http_method = 'DELETE' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');

-- URL 權限管理（280-285）
UPDATE url_permissions SET order_index = 280 WHERE url_pattern = '/api/church/admin/url-permissions' AND http_method = 'GET' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 281 WHERE url_pattern = '/api/church/admin/url-permissions' AND http_method = 'POST' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 282 WHERE url_pattern = '/api/church/admin/url-permissions/*' AND http_method = 'GET' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 283 WHERE url_pattern = '/api/church/admin/url-permissions/*' AND http_method = 'PUT' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');
UPDATE url_permissions SET order_index = 284 WHERE url_pattern = '/api/church/admin/url-permissions/*' AND http_method = 'DELETE' AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');


-- 通用規則（放在最後，order_index: 1000+）
-- 注意：這個通用規則應該放在最後，作為後備規則
UPDATE url_permissions SET order_index = 1000 WHERE url_pattern = '/api/church/admin/**' AND http_method IS NULL AND (required_permission = 'CHURCH_ADMIN' OR required_permission = 'PERM_CHURCH_ADMIN');

-- ============================================
-- 第四步：更新時間戳
-- ============================================
UPDATE url_permissions SET updated_at = NOW() WHERE updated_at IS NULL;

-- ============================================
-- 第五步：顯示整理後的結果
-- ============================================
SELECT 
    '整理後的 URL 權限配置（按 order_index 排序）' AS message,
    id,
    url_pattern,
    http_method,
    is_public,
    required_role,
    required_permission,
    order_index,
    is_active,
    description
FROM url_permissions
ORDER BY order_index, id;

-- ============================================
-- 第六步：顯示統計資訊
-- ============================================
SELECT 
    '統計資訊' AS message,
    COUNT(*) AS total_count,
    SUM(CASE WHEN is_public = 1 THEN 1 ELSE 0 END) AS public_count,
    SUM(CASE WHEN is_public = 0 AND required_permission IS NULL THEN 1 ELSE 0 END) AS authenticated_count,
    SUM(CASE WHEN required_permission IN ('CHURCH_ADMIN', 'PERM_CHURCH_ADMIN') THEN 1 ELSE 0 END) AS admin_count,
    SUM(CASE WHEN required_permission IN ('SERVICE_SCHEDULE_EDIT', 'PERSON_EDIT', 'POSITION_EDIT') THEN 1 ELSE 0 END) AS edit_permission_count
FROM url_permissions;

-- ============================================
-- 整合所有 API 權限配置
-- ============================================
-- 此腳本整合了所有 API 的 URL 權限配置
-- 包括：教會後台管理、文件上傳、前台公開、主日信息、排程任務、系統維護
-- ============================================

USE church;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================
-- 第一部分：確保必要的權限存在
-- ============================================

INSERT IGNORE INTO permissions (permission_code, permission_name, resource, action, description) VALUES
('CHURCH_ADMIN', '教會管理員權限', 'church', 'admin', '教會後台管理權限'),
('SUNDAY_MESSAGE_READ', '查看主日信息', 'sunday_message', 'read', '可以查看主日信息'),
('SUNDAY_MESSAGE_EDIT', '編輯主日信息', 'sunday_message', 'edit', '可以新增、修改、刪除主日信息'),
('ACTIVITY_EDIT', '編輯活動', 'activity', 'edit', '可以新增、修改、刪除活動');

-- ============================================
-- 第二部分：教會後台管理 API 權限配置
-- ============================================

INSERT IGNORE INTO url_permissions (
    url_pattern, 
    http_method, 
    is_public, 
    required_role, 
    required_permission, 
    is_active, 
    order_index, 
    description
) VALUES 
-- 教會資訊管理 API
('/api/church/admin/church-info', 'GET', 0, NULL, 'CHURCH_ADMIN', 1, 210, '獲取所有教會資訊（需教會管理員權限）'),
('/api/church/admin/church-info', 'POST', 0, NULL, 'CHURCH_ADMIN', 1, 211, '創建或更新教會資訊（需教會管理員權限）'),
('/api/church/admin/church-info/*', 'GET', 0, NULL, 'CHURCH_ADMIN', 1, 212, '根據 key 獲取教會資訊（需教會管理員權限）'),
('/api/church/admin/church-info/batch', 'PUT', 0, NULL, 'CHURCH_ADMIN', 1, 213, '批量更新教會資訊（需教會管理員權限）'),
-- 關於我們管理 API
('/api/church/admin/about-info', 'GET', 0, NULL, 'CHURCH_ADMIN', 1, 220, '獲取所有關於我們資訊（需教會管理員權限）'),
('/api/church/admin/about-info', 'POST', 0, NULL, 'CHURCH_ADMIN', 1, 221, '創建關於我們資訊（需教會管理員權限）'),
('/api/church/admin/about-info/*', 'GET', 0, NULL, 'CHURCH_ADMIN', 1, 222, '根據 ID 獲取關於我們資訊（需教會管理員權限）'),
('/api/church/admin/about-info/*', 'PUT', 0, NULL, 'CHURCH_ADMIN', 1, 223, '更新關於我們資訊（需教會管理員權限）'),
('/api/church/admin/about-info/*', 'DELETE', 0, NULL, 'CHURCH_ADMIN', 1, 224, '刪除關於我們資訊（需教會管理員權限）'),
-- 活動管理 API
('/api/church/admin/activities', 'GET', 0, NULL, 'CHURCH_ADMIN', 1, 230, '獲取所有活動（需教會管理員權限）'),
('/api/church/admin/activities', 'POST', 0, NULL, 'CHURCH_ADMIN', 1, 231, '創建活動（需教會管理員權限）'),
('/api/church/admin/activities/*', 'GET', 0, NULL, 'CHURCH_ADMIN', 1, 232, '根據 ID 獲取活動（需教會管理員權限）'),
('/api/church/admin/activities/*', 'PUT', 0, NULL, 'CHURCH_ADMIN', 1, 233, '更新活動（需教會管理員權限）'),
('/api/church/admin/activities/*', 'DELETE', 0, NULL, 'CHURCH_ADMIN', 1, 234, '刪除活動（需教會管理員權限）'),
-- 用戶管理 API
('/api/church/admin/users', 'GET', 0, NULL, 'CHURCH_ADMIN', 1, 240, '獲取所有用戶（需教會管理員權限）'),
('/api/church/admin/users', 'POST', 0, NULL, 'CHURCH_ADMIN', 1, 241, '創建用戶（需教會管理員權限）'),
('/api/church/admin/users/*', 'GET', 0, NULL, 'CHURCH_ADMIN', 1, 242, '根據 UID 獲取用戶（需教會管理員權限）'),
('/api/church/admin/users/*', 'PUT', 0, NULL, 'CHURCH_ADMIN', 1, 243, '更新用戶（需教會管理員權限）'),
('/api/church/admin/users/*', 'DELETE', 0, NULL, 'CHURCH_ADMIN', 1, 244, '刪除用戶（需教會管理員權限）'),
('/api/church/admin/users/*/roles', 'POST', 0, NULL, 'CHURCH_ADMIN', 1, 245, '為用戶分配角色（需教會管理員權限）'),
('/api/church/admin/users/*/permissions', 'POST', 0, NULL, 'CHURCH_ADMIN', 1, 246, '為用戶分配權限（需教會管理員權限）'),
-- 角色管理 API
('/api/church/admin/roles', 'GET', 0, NULL, 'CHURCH_ADMIN', 1, 250, '獲取所有角色（需教會管理員權限）'),
('/api/church/admin/roles', 'POST', 0, NULL, 'CHURCH_ADMIN', 1, 251, '創建角色（需教會管理員權限）'),
('/api/church/admin/roles/*', 'GET', 0, NULL, 'CHURCH_ADMIN', 1, 252, '根據 ID 獲取角色（需教會管理員權限）'),
('/api/church/admin/roles/*', 'PUT', 0, NULL, 'CHURCH_ADMIN', 1, 253, '更新角色（需教會管理員權限）'),
('/api/church/admin/roles/*', 'DELETE', 0, NULL, 'CHURCH_ADMIN', 1, 254, '刪除角色（需教會管理員權限）'),
('/api/church/admin/roles/*/permissions', 'POST', 0, NULL, 'CHURCH_ADMIN', 1, 255, '為角色分配權限（需教會管理員權限）'),
-- 權限管理 API
('/api/church/admin/permissions', 'GET', 0, NULL, 'CHURCH_ADMIN', 1, 260, '獲取所有權限（需教會管理員權限）'),
('/api/church/admin/permissions', 'POST', 0, NULL, 'CHURCH_ADMIN', 1, 261, '創建權限（需教會管理員權限）'),
('/api/church/admin/permissions/*', 'GET', 0, NULL, 'CHURCH_ADMIN', 1, 262, '根據 ID 獲取權限（需教會管理員權限）'),
('/api/church/admin/permissions/*', 'PUT', 0, NULL, 'CHURCH_ADMIN', 1, 263, '更新權限（需教會管理員權限）'),
('/api/church/admin/permissions/*', 'DELETE', 0, NULL, 'CHURCH_ADMIN', 1, 264, '刪除權限（需教會管理員權限）'),
('/api/church/admin/permissions/resource/*', 'GET', 0, NULL, 'CHURCH_ADMIN', 1, 265, '根據資源獲取權限（需教會管理員權限）'),
-- URL 權限管理 API
('/api/church/admin/url-permissions', 'GET', 0, NULL, 'CHURCH_ADMIN', 1, 280, '獲取所有 URL 權限配置（需教會管理員權限）'),
('/api/church/admin/url-permissions', 'POST', 0, NULL, 'CHURCH_ADMIN', 1, 281, '創建 URL 權限配置（需教會管理員權限）'),
('/api/church/admin/url-permissions/*', 'GET', 0, NULL, 'CHURCH_ADMIN', 1, 282, '根據 ID 獲取 URL 權限配置（需教會管理員權限）'),
('/api/church/admin/url-permissions/*', 'PUT', 0, NULL, 'CHURCH_ADMIN', 1, 283, '更新 URL 權限配置（需教會管理員權限）'),
('/api/church/admin/url-permissions/*', 'DELETE', 0, NULL, 'CHURCH_ADMIN', 1, 284, '刪除 URL 權限配置（需教會管理員權限）'),
('/api/church/admin/url-permissions/active', 'GET', 0, NULL, NULL, 1, 285, '獲取所有啟用的 URL 權限配置（公開）'),
-- 菜單 API（需要認證的端點，不需要 PERM_CHURCH_ADMIN，order_index 較小以優先匹配）
('/api/church/menus/admin', 'GET', 0, NULL, NULL, 1, 270, '獲取後台菜單（需認證）'),
('/api/church/menus/dashboard', 'GET', 0, NULL, NULL, 1, 271, '獲取儀表板快速操作菜單（需認證）'),
('/api/church/menus/frontend', 'GET', 1, NULL, NULL, 1, 272, '獲取前台菜單（公開）'),
-- 菜單管理 API（需要 CHURCH_ADMIN 的端點，order_index 較大）
('/api/church/menus', 'GET', 0, NULL, 'CHURCH_ADMIN', 1, 290, '獲取所有菜單項（需教會管理員權限）'),
('/api/church/menus', 'POST', 0, NULL, 'CHURCH_ADMIN', 1, 291, '創建菜單項（需教會管理員權限）'),
('/api/church/menus/*', 'GET', 0, NULL, 'CHURCH_ADMIN', 1, 292, '根據 ID 獲取菜單項（需教會管理員權限）'),
('/api/church/menus/*', 'PUT', 0, NULL, 'CHURCH_ADMIN', 1, 293, '更新菜單項（需教會管理員權限）'),
('/api/church/menus/*', 'DELETE', 0, NULL, 'CHURCH_ADMIN', 1, 294, '刪除菜單項（需教會管理員權限）');

-- ============================================
-- 第三部分：文件上傳 API 權限配置
-- ============================================

INSERT IGNORE INTO url_permissions (
    url_pattern, 
    http_method, 
    is_public, 
    required_role, 
    required_permission, 
    is_active, 
    order_index, 
    description
) VALUES 
-- 活動圖片上傳（需活動編輯權限）
('/api/church/admin/upload/image', 'POST', 0, NULL, 'ACTIVITY_EDIT', 1, 240, '上傳活動圖片（需活動編輯權限）'),
-- 主日信息圖片上傳（需主日信息編輯權限）
('/api/church/admin/upload/image', 'POST', 0, NULL, 'SUNDAY_MESSAGE_EDIT', 1, 241, '上傳主日信息圖片（需主日信息編輯權限）');

-- ============================================
-- 第四部分：前台公開 API 權限配置
-- ============================================

INSERT IGNORE INTO url_permissions (
    url_pattern, 
    http_method, 
    is_public, 
    required_role, 
    required_permission, 
    is_active, 
    order_index, 
    description
) VALUES 
-- 獲取教會基本資訊（公開）
('/api/church/public/church-info', 'GET', 1, NULL, NULL, 1, 100, '獲取教會基本資訊（公開）'),
-- 獲取關於我們資訊（公開）
('/api/church/public/about-info', 'GET', 1, NULL, NULL, 1, 101, '獲取關於我們資訊（公開）'),
-- 獲取活動資訊（公開）
('/api/church/public/activities', 'GET', 1, NULL, NULL, 1, 102, '獲取活動資訊（公開）'),
-- 提交聯絡表單（公開）
('/api/church/public/contact-submissions', 'POST', 1, NULL, NULL, 1, 103, '提交聯絡表單（公開）'),
-- 管理端：獲取所有聯絡表單提交記錄（需管理員權限）
('/api/church/admin/contact-submissions', 'GET', 0, NULL, 'CHURCH_ADMIN', 1, 200, '獲取所有聯絡表單提交記錄（需教會管理員權限）'),
-- 管理端：獲取單一聯絡表單提交記錄（需管理員權限）
('/api/church/admin/contact-submissions/*', 'GET', 0, NULL, 'CHURCH_ADMIN', 1, 201, '獲取單一聯絡表單提交記錄（需教會管理員權限）'),
-- 管理端：標記聯絡表單為已讀（需管理員權限）
('/api/church/admin/contact-submissions/*/read', 'PUT', 0, NULL, 'CHURCH_ADMIN', 1, 202, '標記聯絡表單為已讀（需教會管理員權限）'),
-- 管理端：刪除聯絡表單提交記錄（需管理員權限）
('/api/church/admin/contact-submissions/*', 'DELETE', 0, NULL, 'CHURCH_ADMIN', 1, 203, '刪除聯絡表單提交記錄（需教會管理員權限）'),
-- 管理端：獲取統計資訊（需管理員權限）
('/api/church/admin/contact-submissions/stats', 'GET', 0, NULL, 'CHURCH_ADMIN', 1, 204, '獲取聯絡表單統計資訊（需教會管理員權限）');

-- ============================================
-- 第五部分：主日信息 API 權限配置
-- ============================================

INSERT IGNORE INTO url_permissions (
    url_pattern, 
    http_method, 
    is_public, 
    required_role, 
    required_permission, 
    is_active, 
    order_index, 
    description
) VALUES 
-- 公開 API（前台訪問）
('/api/church/public/sunday-messages', 'GET', 1, NULL, NULL, 1, 103, '獲取主日信息（公開）'),
-- 後台管理 API（需主日信息編輯權限）
('/api/church/admin/sunday-messages', 'GET', 0, NULL, 'SUNDAY_MESSAGE_READ', 1, 235, '獲取所有主日信息（需查看主日信息權限）'),
('/api/church/admin/sunday-messages', 'POST', 0, NULL, 'SUNDAY_MESSAGE_EDIT', 1, 236, '創建主日信息（需編輯主日信息權限）'),
('/api/church/admin/sunday-messages/*', 'GET', 0, NULL, 'SUNDAY_MESSAGE_READ', 1, 237, '根據 ID 獲取主日信息（需查看主日信息權限）'),
('/api/church/admin/sunday-messages/*', 'PUT', 0, NULL, 'SUNDAY_MESSAGE_EDIT', 1, 238, '更新主日信息（需編輯主日信息權限）'),
('/api/church/admin/sunday-messages/*', 'DELETE', 0, NULL, 'SUNDAY_MESSAGE_EDIT', 1, 239, '刪除主日信息（需編輯主日信息權限）');

-- ============================================
-- 第六部分：系統維護 API 權限配置
-- ============================================

INSERT IGNORE INTO url_permissions (
    url_pattern, 
    http_method, 
    is_public, 
    required_role, 
    required_permission, 
    is_active, 
    order_index, 
    description
) VALUES 
-- 系統參數管理 API（注意：使用 /* 匹配單層路徑，與現有 SQL 文件保持一致）
('/api/church/admin/system-settings', 'GET', 0, NULL, 'CHURCH_ADMIN', 1, 300, '獲取所有系統參數（需教會管理員權限）'),
('/api/church/admin/system-settings', 'POST', 0, NULL, 'CHURCH_ADMIN', 1, 301, '創建系統參數（需教會管理員權限）'),
('/api/church/admin/system-settings/category/*', 'GET', 0, NULL, 'CHURCH_ADMIN', 1, 302, '根據分類獲取系統參數（需教會管理員權限）'),
('/api/church/admin/system-settings/refresh', 'POST', 0, NULL, 'CHURCH_ADMIN', 1, 303, '刷新系統配置緩存（需教會管理員權限）'),
('/api/church/admin/system-settings/*', 'GET', 0, NULL, 'CHURCH_ADMIN', 1, 304, '根據 key 獲取系統參數（需教會管理員權限）'),
('/api/church/admin/system-settings/*', 'PUT', 0, NULL, 'CHURCH_ADMIN', 1, 305, '更新系統參數（需教會管理員權限）'),
-- 備份管理 API
('/api/church/admin/backups', 'GET', 0, NULL, 'CHURCH_ADMIN', 1, 310, '獲取所有備份檔案列表（需教會管理員權限）'),
('/api/church/admin/backups/create', 'POST', 0, NULL, 'CHURCH_ADMIN', 1, 311, '手動創建備份（需教會管理員權限）'),
('/api/church/admin/backups/download/*', 'GET', 0, NULL, 'CHURCH_ADMIN', 1, 312, '下載備份檔案（需教會管理員權限）'),
('/api/church/admin/backups/*', 'DELETE', 0, NULL, 'CHURCH_ADMIN', 1, 313, '刪除備份檔案（需教會管理員權限）');

-- ============================================
-- 第七部分：更新現有配置（確保一致性）
-- ============================================

-- 如果已經存在，則更新（更新所有教會後台管理 API）
UPDATE url_permissions 
SET 
    required_permission = CASE 
        WHEN url_pattern IN ('/api/church/menus/admin', '/api/church/menus/dashboard') THEN NULL
        WHEN url_pattern = '/api/church/menus/frontend' THEN NULL
        WHEN url_pattern LIKE '/api/church/admin/%' OR url_pattern LIKE '/api/church/menus%' THEN 'CHURCH_ADMIN'
        ELSE required_permission
    END,
    is_public = CASE 
        WHEN url_pattern = '/api/church/menus/frontend' THEN 1
        ELSE is_public
    END,
    is_active = 1,
    updated_at = NOW()
WHERE (url_pattern LIKE '/api/church/admin/%' 
    OR url_pattern LIKE '/api/church/menus%')
  AND (required_permission IS NULL 
    OR (url_pattern IN ('/api/church/menus/admin', '/api/church/menus/dashboard') AND required_permission IS NOT NULL)
    OR (url_pattern NOT IN ('/api/church/menus/admin', '/api/church/menus/dashboard', '/api/church/menus/frontend') 
        AND required_permission != 'CHURCH_ADMIN'));

-- ============================================
-- 第八部分：顯示執行結果
-- ============================================

SELECT '✅ 所有 API 權限配置已添加' AS message;
SELECT COUNT(*) AS total_permissions FROM url_permissions WHERE is_active = 1;


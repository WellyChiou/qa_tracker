-- 為前台公開 API 添加 URL 權限配置
-- 這些 API 是前台網站公開訪問的，不需要登入

USE church;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 插入前台公開 API 的 URL 權限配置
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
('/api/church/admin/contact-submissions', 'GET', 0, NULL, 'PERM_CHURCH_ADMIN', 1, 200, '獲取所有聯絡表單提交記錄（需教會管理員權限）'),
-- 管理端：獲取單一聯絡表單提交記錄（需管理員權限）
('/api/church/admin/contact-submissions/*', 'GET', 0, NULL, 'PERM_CHURCH_ADMIN', 1, 201, '獲取單一聯絡表單提交記錄（需教會管理員權限）'),
-- 管理端：標記聯絡表單為已讀（需管理員權限）
('/api/church/admin/contact-submissions/*/read', 'PUT', 0, NULL, 'PERM_CHURCH_ADMIN', 1, 202, '標記聯絡表單為已讀（需教會管理員權限）'),
-- 管理端：刪除聯絡表單提交記錄（需管理員權限）
('/api/church/admin/contact-submissions/*', 'DELETE', 0, NULL, 'PERM_CHURCH_ADMIN', 1, 203, '刪除聯絡表單提交記錄（需教會管理員權限）'),
-- 管理端：獲取統計資訊（需管理員權限）
('/api/church/admin/contact-submissions/stats', 'GET', 0, NULL, 'PERM_CHURCH_ADMIN', 1, 204, '獲取聯絡表單統計資訊（需教會管理員權限）');

-- 顯示設定結果
SELECT 
    '前台公開 API 權限配置' AS message,
    url_pattern,
    http_method,
    is_public,
    required_role,
    required_permission,
    description
FROM url_permissions
WHERE url_pattern LIKE '/api/church/public/%'
ORDER BY order_index, id;

SELECT 
    '管理端聯絡表單 API 權限配置' AS message,
    url_pattern,
    http_method,
    is_public,
    required_role,
    required_permission,
    description
FROM url_permissions
WHERE url_pattern LIKE '/api/church/admin/contact-submissions%'
ORDER BY order_index, id;


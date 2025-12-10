-- ============================================
-- 說明 SERVICE_SCHEDULE_READ 和 SERVICE_SCHEDULE_EDIT 的使用場景
-- ============================================

USE church;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================
-- 權限使用場景說明
-- ============================================

/*
SERVICE_SCHEDULE_READ（查看服事表）應該用於：

1. 前台菜單項（如果前台服事表頁面需要登入才能查看）
   - FRONTEND_SERVICE_SCHEDULE 菜單項
   - 如果前台服事表頁面需要認證，應該設置 required_permission = 'SERVICE_SCHEDULE_READ'

2. URL 權限配置（如果需要認證才能查看服事表 API）
   - GET /api/church/service-schedules
   - GET /api/church/service-schedules/{id}
   - 如果這些 API 需要認證但不公開，應該設置 required_permission = 'SERVICE_SCHEDULE_READ'

SERVICE_SCHEDULE_EDIT（編輯服事表）應該用於：

1. 後台管理菜單項
   - ADMIN_SERVICE_SCHEDULE（服事表管理）
   - ADMIN_SERVICE_MANAGEMENT（服事管理母菜單）

2. URL 權限配置（需要編輯權限的 API）
   - POST /api/church/service-schedules（新增）
   - PUT /api/church/service-schedules/{id}（修改）
   - DELETE /api/church/service-schedules/{id}（刪除）
*/

-- ============================================
-- 檢查當前配置
-- ============================================

-- 1. 檢查菜單項配置
SELECT 
    '菜單項權限配置' AS check_type,
    menu_code,
    menu_name,
    menu_type,
    required_permission,
    CASE 
        WHEN menu_type = 'frontend' AND required_permission IS NULL THEN '✅ 前台公開（無需權限）'
        WHEN menu_type = 'frontend' AND required_permission = 'SERVICE_SCHEDULE_READ' THEN '✅ 前台需要查看權限'
        WHEN menu_type = 'admin' AND required_permission = 'SERVICE_SCHEDULE_EDIT' THEN '✅ 後台需要編輯權限'
        WHEN menu_type = 'admin' AND required_permission = 'SERVICE_SCHEDULE_READ' THEN '⚠️ 後台應使用 EDIT 權限'
        ELSE '❓ 需要檢查'
    END AS status
FROM menu_items
WHERE menu_code LIKE '%SERVICE_SCHEDULE%'
   OR menu_code LIKE '%SERVICE_MANAGEMENT%'
ORDER BY menu_type, menu_code;

-- 2. 檢查 URL 權限配置
SELECT 
    'URL 權限配置' AS check_type,
    url_pattern,
    http_method,
    is_public,
    required_permission,
    CASE 
        WHEN is_public = 1 THEN '✅ 公開訪問（無需權限）'
        WHEN http_method = 'GET' AND required_permission = 'SERVICE_SCHEDULE_READ' THEN '✅ GET 需要查看權限'
        WHEN http_method = 'GET' AND required_permission IS NULL THEN '✅ GET 只需認證'
        WHEN http_method IN ('POST', 'PUT', 'DELETE') AND required_permission = 'SERVICE_SCHEDULE_EDIT' THEN '✅ 編輯操作需要編輯權限'
        WHEN http_method IN ('POST', 'PUT', 'DELETE') AND required_permission = 'SERVICE_SCHEDULE_READ' THEN '⚠️ 編輯操作應使用 EDIT 權限'
        ELSE '❓ 需要檢查'
    END AS status
FROM url_permissions
WHERE url_pattern LIKE '%service-schedule%'
ORDER BY order_index, http_method;

-- ============================================
-- 建議的配置
-- ============================================

/*
建議配置：

1. 前台菜單項（FRONTEND_SERVICE_SCHEDULE）：
   - 如果前台服事表頁面公開：required_permission = NULL
   - 如果前台服事表頁面需要登入：required_permission = 'SERVICE_SCHEDULE_READ'

2. 後台菜單項：
   - ADMIN_SERVICE_SCHEDULE：required_permission = 'SERVICE_SCHEDULE_EDIT'
   - ADMIN_SERVICE_MANAGEMENT：required_permission = 'SERVICE_SCHEDULE_EDIT'

3. URL 權限：
   - GET /api/church/service-schedules：is_public = 1（公開）或 required_permission = 'SERVICE_SCHEDULE_READ'（需要認證）
   - POST/PUT/DELETE：required_permission = 'SERVICE_SCHEDULE_EDIT'
*/


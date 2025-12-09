-- ============================================
-- 初始化教會系統權限表
-- ============================================
-- 此腳本用於補齊 permissions 表的資料
-- 包含所有教會系統需要的權限
-- ============================================

USE church;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================
-- 插入教會系統權限
-- ============================================
-- 使用 INSERT IGNORE 避免重複插入
INSERT IGNORE INTO permissions (permission_code, permission_name, resource, action, description) VALUES

-- ============================================
-- 服事表相關權限
-- ============================================
('SERVICE_SCHEDULE_READ', '查看服事表', 'service_schedule', 'read', '可以查看服事表'),
('SERVICE_SCHEDULE_EDIT', '編輯服事表', 'service_schedule', 'edit', '可以新增、修改、刪除服事表'),

-- ============================================
-- 人員管理相關權限
-- ============================================
('PERSON_READ', '查看人員', 'person', 'read', '可以查看人員列表'),
('PERSON_EDIT', '編輯人員', 'person', 'edit', '可以新增、修改、刪除人員'),

-- ============================================
-- 崗位管理相關權限
-- ============================================
('POSITION_READ', '查看崗位', 'position', 'read', '可以查看崗位列表'),
('POSITION_EDIT', '編輯崗位', 'position', 'edit', '可以新增、修改、刪除崗位'),

-- ============================================
-- 教會管理員權限（最高權限）
-- ============================================
('CHURCH_ADMIN', '教會管理員權限', 'church', 'admin', '可以存取所有教會管理功能，包括用戶、角色、權限、菜單、URL權限等管理'),

-- ============================================
-- 教會資訊管理權限
-- ============================================
('CHURCH_INFO_READ', '查看教會資訊', 'church_info', 'read', '可以查看教會基本資訊'),
('CHURCH_INFO_EDIT', '編輯教會資訊', 'church_info', 'edit', '可以新增、修改、刪除教會基本資訊'),

-- ============================================
-- 關於我們管理權限
-- ============================================
('ABOUT_INFO_READ', '查看關於我們', 'about_info', 'read', '可以查看關於我們頁面內容'),
('ABOUT_INFO_EDIT', '編輯關於我們', 'about_info', 'edit', '可以新增、修改、刪除關於我們頁面內容'),

-- ============================================
-- 活動管理權限
-- ============================================
('ACTIVITY_READ', '查看活動', 'activity', 'read', '可以查看活動資訊'),
('ACTIVITY_EDIT', '編輯活動', 'activity', 'edit', '可以新增、修改、刪除活動資訊'),

-- ============================================
-- 聯絡表單管理權限
-- ============================================
('CONTACT_SUBMISSION_READ', '查看聯絡表單', 'contact_submission', 'read', '可以查看聯絡表單提交記錄'),
('CONTACT_SUBMISSION_EDIT', '管理聯絡表單', 'contact_submission', 'edit', '可以標記為已讀、刪除聯絡表單提交記錄'),

-- ============================================
-- 用戶管理權限
-- ============================================
('USER_READ', '查看用戶', 'user', 'read', '可以查看用戶列表和詳情'),
('USER_EDIT', '編輯用戶', 'user', 'edit', '可以新增、修改、刪除用戶'),
('USER_ROLE_MANAGE', '管理用戶角色', 'user', 'role_manage', '可以為用戶分配或移除角色'),
('USER_PERMISSION_MANAGE', '管理用戶權限', 'user', 'permission_manage', '可以為用戶分配或移除權限'),

-- ============================================
-- 角色管理權限
-- ============================================
('ROLE_READ', '查看角色', 'role', 'read', '可以查看角色列表和詳情'),
('ROLE_EDIT', '編輯角色', 'role', 'edit', '可以新增、修改、刪除角色'),
('ROLE_PERMISSION_MANAGE', '管理角色權限', 'role', 'permission_manage', '可以為角色分配或移除權限'),

-- ============================================
-- 權限管理權限
-- ============================================
('PERMISSION_READ', '查看權限', 'permission', 'read', '可以查看權限列表和詳情'),
('PERMISSION_EDIT', '編輯權限', 'permission', 'edit', '可以新增、修改、刪除權限'),

-- ============================================
-- 菜單管理權限
-- ============================================
('MENU_READ', '查看菜單', 'menu', 'read', '可以查看菜單列表和詳情'),
('MENU_EDIT', '編輯菜單', 'menu', 'edit', '可以新增、修改、刪除菜單項'),

-- ============================================
-- URL 權限管理權限
-- ============================================
('URL_PERMISSION_READ', '查看 URL 權限', 'url_permission', 'read', '可以查看 URL 權限配置列表和詳情'),
('URL_PERMISSION_EDIT', '編輯 URL 權限', 'url_permission', 'edit', '可以新增、修改、刪除 URL 權限配置');

-- ============================================
-- 顯示插入結果
-- ============================================
SELECT 
    '權限初始化完成' AS message,
    COUNT(*) AS total_permissions,
    COUNT(DISTINCT resource) AS total_resources
FROM permissions;

-- ============================================
-- 顯示所有權限（按資源分組）
-- ============================================
SELECT 
    resource AS '資源',
    action AS '操作',
    permission_code AS '權限代碼',
    permission_name AS '權限名稱',
    description AS '描述'
FROM permissions
ORDER BY 
    CASE 
        WHEN resource = 'church' THEN 1
        WHEN resource = 'service_schedule' THEN 2
        WHEN resource = 'person' THEN 3
        WHEN resource = 'position' THEN 4
        WHEN resource = 'church_info' THEN 5
        WHEN resource = 'about_info' THEN 6
        WHEN resource = 'activity' THEN 7
        WHEN resource = 'contact_submission' THEN 8
        WHEN resource = 'user' THEN 9
        WHEN resource = 'role' THEN 10
        WHEN resource = 'permission' THEN 11
        WHEN resource = 'menu' THEN 12
        WHEN resource = 'url_permission' THEN 13
        ELSE 99
    END,
    action,
    permission_code;

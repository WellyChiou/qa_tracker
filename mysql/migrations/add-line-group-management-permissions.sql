-- 1. 新增後端 API 權限 (UrlPermission)
-- 欄位：url_pattern, description, http_method, required_role (設為 ROLE_ADMIN 或其他), order_index, is_active

-- 獲取群組列表
INSERT INTO url_permissions (url_pattern, description, http_method, required_role, order_index, is_active, is_public) 
SELECT '/api/personal/line-groups', '獲取所有 LINE 群組', 'GET', 'ROLE_ADMIN', 10, 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM url_permissions WHERE url_pattern = '/api/personal/line-groups' AND http_method = 'GET');

-- 獲取單一群組
INSERT INTO url_permissions (url_pattern, description, http_method, required_role, order_index, is_active, is_public)
SELECT '/api/personal/line-groups/.*', '獲取/管理 LINE 群組詳情', 'GET', 'ROLE_ADMIN', 10, 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM url_permissions WHERE url_pattern = '/api/personal/line-groups/.*' AND http_method = 'GET');

-- 獲取群組成員
INSERT INTO url_permissions (url_pattern, description, http_method, required_role, order_index, is_active, is_public)
SELECT '/api/personal/line-groups/.*', '管理群組成員', 'GET', 'ROLE_ADMIN', 10, 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM url_permissions WHERE url_pattern = '/api/personal/line-groups/.*' AND http_method = 'GET');

-- 創建群組
INSERT INTO url_permissions (url_pattern, description, http_method, required_role, order_index, is_active, is_public)
SELECT '/api/personal/line-groups', '創建 LINE 群組', 'POST', 'ROLE_ADMIN', 10, 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM url_permissions WHERE url_pattern = '/api/personal/line-groups' AND http_method = 'POST');

-- 更新群組
INSERT INTO url_permissions (url_pattern, description, http_method, required_role, order_index, is_active, is_public)
SELECT '/api/personal/line-groups/.*', '更新 LINE 群組', 'PUT', 'ROLE_ADMIN', 10, 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM url_permissions WHERE url_pattern = '/api/personal/line-groups/.*' AND http_method = 'PUT');

-- 刪除群組
INSERT INTO url_permissions (url_pattern, description, http_method, required_role, order_index, is_active, is_public)
SELECT '/api/personal/line-groups/.*', '刪除 LINE 群組', 'DELETE', 'ROLE_ADMIN', 10, 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM url_permissions WHERE url_pattern = '/api/personal/line-groups/.*' AND http_method = 'DELETE');

-- 2. 新增功能權限 (Permission)
-- 欄位：permission_code, permission_name, resource, action, description
INSERT INTO permissions (permission_code, permission_name, resource, action, description)
SELECT 'line_group:view', '查看 LINE 群組', 'line_group', 'view', '查看 LINE 群組'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'line_group:view');

INSERT INTO permissions (permission_code, permission_name, resource, action, description)
SELECT 'line_group:create', '創建 LINE 群組', 'line_group', 'create', '創建 LINE 群組'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'line_group:create');

INSERT INTO permissions (permission_code, permission_name, resource, action, description)
SELECT 'line_group:edit', '編輯 LINE 群組', 'line_group', 'edit', '編輯 LINE 群組'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'line_group:edit');

INSERT INTO permissions (permission_code, permission_name, resource, action, description)
SELECT 'line_group:delete', '刪除 LINE 群組', 'line_group', 'delete', '刪除 LINE 群組'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'line_group:delete');

INSERT INTO permissions (permission_code, permission_name, resource, action, description)
SELECT 'line_group_member:view', '查看群組成員', 'line_group_member', 'view', '查看群組成員'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'line_group_member:view');

INSERT INTO permissions (permission_code, permission_name, resource, action, description)
SELECT 'line_group_member:add', '新增群組成員', 'line_group_member', 'add', '新增群組成員'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'line_group_member:add');

INSERT INTO permissions (permission_code, permission_name, resource, action, description)
SELECT 'line_group_member:edit', '編輯群組成員', 'line_group_member', 'edit', '編輯群組成員'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'line_group_member:edit');

INSERT INTO permissions (permission_code, permission_name, resource, action, description)
SELECT 'line_group_member:remove', '移除群組成員', 'line_group_member', 'remove', '移除群組成員'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'line_group_member:remove');


-- 3. 分配權限給 Role (假設 Role ID 1 為 Admin)
SET @role_id = 1;

-- 分配功能權限 (RolePermission)
INSERT INTO role_permissions (role_id, permission_id)
SELECT @role_id, p.id 
FROM permissions p 
WHERE p.permission_code LIKE 'line_group%'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp 
    WHERE rp.role_id = @role_id AND rp.permission_id = p.id
);

-- 4. 新增選單項目 (MenuItem)
-- 欄位：menu_code, menu_name, icon, url, parent_id, order_index, is_active, show_in_dashboard, required_permission
-- 假設放在 '系統管理' (假設 path/name 包含 'admin')
-- 我們插入一個頂級菜單項 'LINE 群組管理'

-- 暫時插入到根目錄 (parent_id = NULL) 或依據現有結構
INSERT INTO menu_items (menu_code, menu_name, icon, url, parent_id, order_index, is_active, show_in_dashboard, required_permission)
SELECT 'admin_line_groups', 'LINE 群組管理', 'fas fa-users', '/admin/line-groups', NULL, 10, 1, 1, 'line_group:view'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE url = '/admin/line-groups');

-- 如果要分配給角色，因為 personal 系統沒有 role_menu_items 表，
-- 而是根據 role_permissions 和 menu_items.required_permission 進行動態判斷
-- 所以這裡不需要插入 role_menu_items。

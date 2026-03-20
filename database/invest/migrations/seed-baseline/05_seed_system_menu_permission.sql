-- Slice G: Invest Admin baseline system menu + permission skeleton
-- 範圍：僅新增 ACL 骨架（permissions / menu_items.required_permission / role_permissions）

USE invest;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO permissions (permission_code, permission_name, resource, action, description)
VALUES
('INVEST_SYS_USERS_VIEW', '查看用戶管理', 'system_user', 'view', '可查看用戶管理骨架頁面'),
('INVEST_SYS_ROLES_VIEW', '查看角色管理', 'system_role', 'view', '可查看角色管理骨架頁面'),
('INVEST_SYS_PERMISSIONS_VIEW', '查看權限管理', 'system_permission', 'view', '可查看權限管理骨架頁面'),
('INVEST_SYS_MENUS_VIEW', '查看菜單管理', 'system_menu', 'view', '可查看菜單管理骨架頁面'),
('INVEST_SYS_URL_PERMISSIONS_VIEW', '查看 URL 權限管理', 'system_url_permission', 'view', '可查看 URL 權限管理骨架頁面'),
('INVEST_SYS_MAINTENANCE_VIEW', '查看系統維護', 'system_maintenance', 'view', '可查看系統維護骨架頁面'),
('INVEST_SYS_SCHEDULER_VIEW', '查看排程管理', 'system_scheduler', 'view', '可查看排程管理骨架頁面'),
('INVEST_SYS_LINE_GROUPS_VIEW', '查看 LINE 群組', 'system_line_group', 'view', '可查看 LINE 群組骨架頁面')
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    resource = VALUES(resource),
    action = VALUES(action),
    description = VALUES(description),
    updated_at = CURRENT_TIMESTAMP;

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p
  ON p.permission_code IN (
      'INVEST_SYS_USERS_VIEW',
      'INVEST_SYS_ROLES_VIEW',
      'INVEST_SYS_PERMISSIONS_VIEW',
      'INVEST_SYS_MENUS_VIEW',
      'INVEST_SYS_URL_PERMISSIONS_VIEW',
      'INVEST_SYS_MAINTENANCE_VIEW',
      'INVEST_SYS_SCHEDULER_VIEW',
      'INVEST_SYS_LINE_GROUPS_VIEW'
  )
WHERE r.role_name = 'ROLE_INVEST_ADMIN';

INSERT INTO menu_items (
    menu_code, menu_name, icon, url, parent_id, order_index, is_active, show_in_dashboard, required_permission, description
)
VALUES
('INVEST_BASIC_FEATURES', '基本功能', '🧩', '#', NULL, 20, 1, 1, NULL, 'Invest 基本功能群組'),
('INVEST_SYSTEM_SETTINGS', '系統設定', '⚙️', '#', NULL, 30, 1, 1, NULL, 'Invest 系統設定群組')
ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name),
    icon = VALUES(icon),
    url = VALUES(url),
    parent_id = VALUES(parent_id),
    order_index = VALUES(order_index),
    is_active = VALUES(is_active),
    show_in_dashboard = VALUES(show_in_dashboard),
    required_permission = VALUES(required_permission),
    description = VALUES(description),
    updated_at = CURRENT_TIMESTAMP;

UPDATE menu_items
SET menu_name = '投資儀表板',
    icon = '📊',
    url = '/invest-admin/',
    parent_id = NULL,
    order_index = 10,
    is_active = 1,
    show_in_dashboard = 1,
    required_permission = 'INVEST_DASHBOARD_VIEW',
    description = 'Invest 儀表板',
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code = 'INVEST_DASHBOARD';

UPDATE menu_items m
JOIN menu_items p ON p.menu_code = 'INVEST_BASIC_FEATURES'
SET m.parent_id = p.id,
    m.order_index = 10,
    m.menu_name = '持股中心',
    m.icon = '💼',
    m.url = '/invest-admin/portfolios',
    m.required_permission = 'INVEST_PORTFOLIO_VIEW',
    m.is_active = 1,
    m.show_in_dashboard = 1,
    m.description = '持股列表與明細',
    m.updated_at = CURRENT_TIMESTAMP
WHERE m.menu_code = 'INVEST_PORTFOLIOS';

UPDATE menu_items m
JOIN menu_items p ON p.menu_code = 'INVEST_BASIC_FEATURES'
SET m.parent_id = p.id,
    m.order_index = 20,
    m.menu_name = '股票主資料',
    m.icon = '🏷️',
    m.url = '/invest-admin/stocks',
    m.required_permission = 'INVEST_STOCK_VIEW',
    m.is_active = 1,
    m.show_in_dashboard = 1,
    m.description = '股票主資料管理',
    m.updated_at = CURRENT_TIMESTAMP
WHERE m.menu_code = 'INVEST_STOCKS';

UPDATE menu_items m
JOIN menu_items p ON p.menu_code = 'INVEST_BASIC_FEATURES'
SET m.parent_id = p.id,
    m.order_index = 30,
    m.menu_name = '每日行情',
    m.icon = '📈',
    m.url = '/invest-admin/stock-price-dailies',
    m.required_permission = 'INVEST_STOCK_PRICE_DAILY_VIEW',
    m.is_active = 1,
    m.show_in_dashboard = 1,
    m.description = '每日行情管理',
    m.updated_at = CURRENT_TIMESTAMP
WHERE m.menu_code = 'INVEST_STOCK_PRICE_DAILIES';

INSERT INTO menu_items (
    menu_code, menu_name, icon, url, parent_id, order_index, is_active, show_in_dashboard, required_permission, description
)
SELECT
    'INVEST_SYS_USERS', '用戶管理', '👤', '/invest-admin/system/users', p.id, 10, 1, 1, 'INVEST_SYS_USERS_VIEW', '用戶管理骨架頁面'
FROM menu_items p
WHERE p.menu_code = 'INVEST_SYSTEM_SETTINGS'
ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name),
    icon = VALUES(icon),
    url = VALUES(url),
    parent_id = VALUES(parent_id),
    order_index = VALUES(order_index),
    is_active = VALUES(is_active),
    show_in_dashboard = VALUES(show_in_dashboard),
    required_permission = VALUES(required_permission),
    description = VALUES(description),
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_items (
    menu_code, menu_name, icon, url, parent_id, order_index, is_active, show_in_dashboard, required_permission, description
)
SELECT
    'INVEST_SYS_ROLES', '角色管理', '🧾', '/invest-admin/system/roles', p.id, 20, 1, 1, 'INVEST_SYS_ROLES_VIEW', '角色管理骨架頁面'
FROM menu_items p
WHERE p.menu_code = 'INVEST_SYSTEM_SETTINGS'
ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name),
    icon = VALUES(icon),
    url = VALUES(url),
    parent_id = VALUES(parent_id),
    order_index = VALUES(order_index),
    is_active = VALUES(is_active),
    show_in_dashboard = VALUES(show_in_dashboard),
    required_permission = VALUES(required_permission),
    description = VALUES(description),
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_items (
    menu_code, menu_name, icon, url, parent_id, order_index, is_active, show_in_dashboard, required_permission, description
)
SELECT
    'INVEST_SYS_PERMISSIONS', '權限管理', '🛡️', '/invest-admin/system/permissions', p.id, 30, 1, 1, 'INVEST_SYS_PERMISSIONS_VIEW', '權限管理骨架頁面'
FROM menu_items p
WHERE p.menu_code = 'INVEST_SYSTEM_SETTINGS'
ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name),
    icon = VALUES(icon),
    url = VALUES(url),
    parent_id = VALUES(parent_id),
    order_index = VALUES(order_index),
    is_active = VALUES(is_active),
    show_in_dashboard = VALUES(show_in_dashboard),
    required_permission = VALUES(required_permission),
    description = VALUES(description),
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_items (
    menu_code, menu_name, icon, url, parent_id, order_index, is_active, show_in_dashboard, required_permission, description
)
SELECT
    'INVEST_SYS_MENUS', '菜單管理', '🧭', '/invest-admin/system/menus', p.id, 40, 1, 1, 'INVEST_SYS_MENUS_VIEW', '菜單管理骨架頁面'
FROM menu_items p
WHERE p.menu_code = 'INVEST_SYSTEM_SETTINGS'
ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name),
    icon = VALUES(icon),
    url = VALUES(url),
    parent_id = VALUES(parent_id),
    order_index = VALUES(order_index),
    is_active = VALUES(is_active),
    show_in_dashboard = VALUES(show_in_dashboard),
    required_permission = VALUES(required_permission),
    description = VALUES(description),
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_items (
    menu_code, menu_name, icon, url, parent_id, order_index, is_active, show_in_dashboard, required_permission, description
)
SELECT
    'INVEST_SYS_URL_PERMISSIONS', 'URL 權限管理', '🔗', '/invest-admin/system/url-permissions', p.id, 50, 1, 1, 'INVEST_SYS_URL_PERMISSIONS_VIEW', 'URL 權限管理骨架頁面'
FROM menu_items p
WHERE p.menu_code = 'INVEST_SYSTEM_SETTINGS'
ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name),
    icon = VALUES(icon),
    url = VALUES(url),
    parent_id = VALUES(parent_id),
    order_index = VALUES(order_index),
    is_active = VALUES(is_active),
    show_in_dashboard = VALUES(show_in_dashboard),
    required_permission = VALUES(required_permission),
    description = VALUES(description),
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_items (
    menu_code, menu_name, icon, url, parent_id, order_index, is_active, show_in_dashboard, required_permission, description
)
SELECT
    'INVEST_SYS_MAINTENANCE', '系統維護', '🛠️', '/invest-admin/system/maintenance', p.id, 60, 1, 1, 'INVEST_SYS_MAINTENANCE_VIEW', '系統維護骨架頁面'
FROM menu_items p
WHERE p.menu_code = 'INVEST_SYSTEM_SETTINGS'
ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name),
    icon = VALUES(icon),
    url = VALUES(url),
    parent_id = VALUES(parent_id),
    order_index = VALUES(order_index),
    is_active = VALUES(is_active),
    show_in_dashboard = VALUES(show_in_dashboard),
    required_permission = VALUES(required_permission),
    description = VALUES(description),
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_items (
    menu_code, menu_name, icon, url, parent_id, order_index, is_active, show_in_dashboard, required_permission, description
)
SELECT
    'INVEST_SYS_SCHEDULER', '排程管理', '⏱️', '/invest-admin/system/scheduler', p.id, 70, 1, 1, 'INVEST_SYS_SCHEDULER_VIEW', '排程管理骨架頁面'
FROM menu_items p
WHERE p.menu_code = 'INVEST_SYSTEM_SETTINGS'
ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name),
    icon = VALUES(icon),
    url = VALUES(url),
    parent_id = VALUES(parent_id),
    order_index = VALUES(order_index),
    is_active = VALUES(is_active),
    show_in_dashboard = VALUES(show_in_dashboard),
    required_permission = VALUES(required_permission),
    description = VALUES(description),
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_items (
    menu_code, menu_name, icon, url, parent_id, order_index, is_active, show_in_dashboard, required_permission, description
)
SELECT
    'INVEST_SYS_LINE_GROUPS', 'LINE 群組', '💬', '/invest-admin/system/line-groups', p.id, 80, 1, 1, 'INVEST_SYS_LINE_GROUPS_VIEW', 'LINE 群組骨架頁面'
FROM menu_items p
WHERE p.menu_code = 'INVEST_SYSTEM_SETTINGS'
ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name),
    icon = VALUES(icon),
    url = VALUES(url),
    parent_id = VALUES(parent_id),
    order_index = VALUES(order_index),
    is_active = VALUES(is_active),
    show_in_dashboard = VALUES(show_in_dashboard),
    required_permission = VALUES(required_permission),
    description = VALUES(description),
    updated_at = CURRENT_TIMESTAMP;

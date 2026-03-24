USE invest;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO permissions (permission_code, permission_name, resource, action, description)
VALUES
('INVEST_SYS_MAINTENANCE_EDIT', '管理系統維護設定', 'system_maintenance', 'edit', '可編輯系統維護設定、執行備份與 LINE 測試訊息'),
('INVEST_SYS_LINE_GROUPS_EDIT', '管理 LINE 群組', 'system_line_group', 'edit', '可新增/編輯/停用 LINE 群組與群組成員')
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
    'INVEST_SYS_MAINTENANCE_VIEW',
    'INVEST_SYS_MAINTENANCE_EDIT',
    'INVEST_SYS_LINE_GROUPS_VIEW',
    'INVEST_SYS_LINE_GROUPS_EDIT'
  )
WHERE r.role_name = 'ROLE_INVEST_ADMIN';

UPDATE menu_items
SET description = '集中管理 invest 系統參數、資料庫備份與 LINE 測試訊息',
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code = 'INVEST_SYS_MAINTENANCE';

UPDATE menu_items
SET description = '管理 invest LINE 群組與群組成員',
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code = 'INVEST_SYS_LINE_GROUPS';

INSERT INTO url_permissions (url_pattern, http_method, is_public, required_role, required_permission, description, is_active, order_index)
VALUES
('/api/invest/system/maintenance/settings', 'GET', 0, NULL, 'INVEST_SYS_MAINTENANCE_VIEW', '查詢全部系統維護設定', 1, 271),
('/api/invest/system/maintenance/settings/category/*', 'GET', 0, NULL, 'INVEST_SYS_MAINTENANCE_VIEW', '依分類查詢系統維護設定', 1, 272),
('/api/invest/system/maintenance/settings/*', 'GET', 0, NULL, 'INVEST_SYS_MAINTENANCE_VIEW', '查詢單一系統維護設定', 1, 273),
('/api/invest/system/maintenance/settings', 'POST', 0, NULL, 'INVEST_SYS_MAINTENANCE_EDIT', '新增系統維護設定', 1, 274),
('/api/invest/system/maintenance/settings/*', 'PUT', 0, NULL, 'INVEST_SYS_MAINTENANCE_EDIT', '更新系統維護設定', 1, 275),
('/api/invest/system/maintenance/settings/refresh', 'POST', 0, NULL, 'INVEST_SYS_MAINTENANCE_EDIT', '刷新系統維護設定快取', 1, 276),
('/api/invest/system/maintenance/backups', 'GET', 0, NULL, 'INVEST_SYS_MAINTENANCE_VIEW', '查詢 invest 備份檔案清單', 1, 277),
('/api/invest/system/maintenance/backups/create', 'POST', 0, NULL, 'INVEST_SYS_MAINTENANCE_EDIT', '手動建立 invest 備份', 1, 278),
('/api/invest/system/maintenance/backups/download', 'GET', 0, NULL, 'INVEST_SYS_MAINTENANCE_VIEW', '下載 invest 備份檔案', 1, 279),
('/api/invest/system/maintenance/backups/delete', 'DELETE', 0, NULL, 'INVEST_SYS_MAINTENANCE_EDIT', '刪除 invest 備份檔案', 1, 280),
('/api/invest/line/test/group-push', 'POST', 0, NULL, 'INVEST_SYS_MAINTENANCE_EDIT', '發送 invest LINE 測試群組訊息', 1, 281),
('/api/invest/system/line-groups', 'GET', 0, NULL, 'INVEST_SYS_LINE_GROUPS_VIEW', '查詢 LINE 群組列表', 1, 282),
('/api/invest/system/line-groups/*', 'GET', 0, NULL, 'INVEST_SYS_LINE_GROUPS_VIEW', '查詢單一 LINE 群組', 1, 283),
('/api/invest/system/line-groups/*/members', 'GET', 0, NULL, 'INVEST_SYS_LINE_GROUPS_VIEW', '查詢 LINE 群組成員', 1, 284),
('/api/invest/system/line-groups', 'POST', 0, NULL, 'INVEST_SYS_LINE_GROUPS_EDIT', '新增 LINE 群組', 1, 285),
('/api/invest/system/line-groups/*', 'PUT', 0, NULL, 'INVEST_SYS_LINE_GROUPS_EDIT', '更新 LINE 群組', 1, 286),
('/api/invest/system/line-groups/*', 'DELETE', 0, NULL, 'INVEST_SYS_LINE_GROUPS_EDIT', '停用 LINE 群組', 1, 287),
('/api/invest/system/line-groups/*/members', 'POST', 0, NULL, 'INVEST_SYS_LINE_GROUPS_EDIT', '新增 LINE 群組成員', 1, 288),
('/api/invest/system/line-groups/*/members/*', 'PUT', 0, NULL, 'INVEST_SYS_LINE_GROUPS_EDIT', '更新 LINE 群組成員', 1, 289),
('/api/invest/system/line-groups/*/members/*', 'DELETE', 0, NULL, 'INVEST_SYS_LINE_GROUPS_EDIT', '停用 LINE 群組成員', 1, 290)
ON DUPLICATE KEY UPDATE
    is_public = VALUES(is_public),
    required_role = VALUES(required_role),
    required_permission = VALUES(required_permission),
    description = VALUES(description),
    is_active = VALUES(is_active),
    order_index = VALUES(order_index),
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO system_settings (setting_key, setting_value, setting_type, category, description, is_editable)
VALUES
('backup.enabled', 'true', 'boolean', 'backup', '是否啟用手動備份功能', 1),
('backup.mysql_service', 'mysql', 'string', 'backup', 'MySQL 服務名稱（容器內）', 1),
('backup.mysql_root_password', 'rootpassword', 'string', 'backup', 'MySQL root 密碼（備份使用）', 1),
('backup.database_name', 'invest', 'string', 'backup', '備份目標資料庫名稱', 1),
('backup.retention_days', '7', 'number', 'backup', '備份檔案保留天數', 1),
('line.bot.channel-token', '', 'string', 'line.bot', 'LINE Messaging API Channel Access Token', 1),
('line.bot.channel-secret', '', 'string', 'line.bot', 'LINE Messaging API Channel Secret', 1),
('line.bot.webhook-url', '', 'string', 'line.bot', 'LINE Webhook URL（預留）', 1),
('line.bot.admin-user-id', '', 'string', 'line.bot', 'LINE 管理員 User ID（預留）', 1),
('scheduler.price-update.enabled', 'true', 'boolean', 'scheduler', '持股行情更新排程是否啟用（設定參考）', 1),
('scheduler.daily-report.enabled', 'true', 'boolean', 'scheduler', '每日報告排程是否啟用（設定參考）', 1),
('scheduler.alert-polling.enabled', 'true', 'boolean', 'scheduler', '警示輪詢排程是否啟用（設定參考）', 1),
('invest.analysis.min-history-days', '20', 'number', 'invest.analysis', '投資分析最小歷史天數', 1),
('invest.analysis.backfill-default-days', '30', 'number', 'invest.analysis', '歷史回補預設天數', 1),
('invest.analysis.alert-abnormal-drop-threshold', '2.0', 'number', 'invest.analysis', '異常下跌判斷閾值（百分比）', 1)
ON DUPLICATE KEY UPDATE
    setting_value = VALUES(setting_value),
    setting_type = VALUES(setting_type),
    category = VALUES(category),
    description = VALUES(description),
    is_editable = VALUES(is_editable),
    updated_at = CURRENT_TIMESTAMP;

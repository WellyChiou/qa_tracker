-- 在 permissions 表中增加 is_public 欄位
-- 用於設定權限對應的 API 是否公開（無需登入即可訪問）

USE church;

-- 增加 is_public 欄位
ALTER TABLE permissions 
ADD COLUMN is_public TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否公開（無需認證）' 
AFTER description;

-- 為現有的公開權限設定 is_public = 1
-- 注意：這裡需要根據實際需求調整哪些權限應該是公開的
-- 例如：查看類的權限可能設為公開，編輯類的權限需要認證

-- 更新查看類權限為公開（可選，根據實際需求）
-- UPDATE permissions SET is_public = 1 WHERE action = 'read';

-- 顯示更新結果
SELECT 'permissions 表已增加 is_public 欄位' AS message;
SELECT permission_code, permission_name, is_public FROM permissions;


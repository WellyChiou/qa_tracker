-- 將活動和主日信息的 image_url 欄位從 VARCHAR(500) 改為 TEXT
-- 以支援更長的 URL 和未來可能的 base64 存儲

USE church;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 修改活動表的 image_url 欄位
ALTER TABLE activities 
MODIFY COLUMN image_url TEXT COMMENT '活動圖片URL';

-- 修改主日信息表的 image_url 欄位
ALTER TABLE sunday_messages 
MODIFY COLUMN image_url TEXT COMMENT 'DM圖片URL';

-- 顯示修改結果
SELECT 
    'image_url 欄位已更新為 TEXT' AS message,
    TABLE_NAME,
    COLUMN_NAME,
    COLUMN_TYPE,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'church'
  AND TABLE_NAME IN ('activities', 'sunday_messages')
  AND COLUMN_NAME = 'image_url';


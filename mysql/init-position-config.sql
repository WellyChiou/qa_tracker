-- 初始化崗位人員配置
-- 將現有的寫死資料插入資料庫
-- 注意：此腳本只會在 MySQL 容器首次初始化時執行
-- 如果資料庫已存在，此腳本不會執行

USE church;

-- 插入默認崗位配置（如果不存在則插入，如果存在則更新）
-- 使用 ON DUPLICATE KEY UPDATE 確保可以安全地重複執行
INSERT INTO position_config (config_name, config_data, is_default) 
VALUES (
    'default',
    JSON_OBJECT(
        'computer', JSON_OBJECT(
            'saturday', JSON_ARRAY('世維', '佑庭', '朝翔'),
            'sunday', JSON_ARRAY('君豪', '家偉', '佑庭')
        ),
        'sound', JSON_OBJECT(
            'saturday', JSON_ARRAY('斌浩', '世維', '佑庭', '桓瑜', '朝翔'),
            'sunday', JSON_ARRAY('朝翔', '君豪', '家偉', '佑庭')
        ),
        'light', JSON_OBJECT(
            'saturday', JSON_ARRAY('阿皮', '佑庭', '桓瑜'),
            'sunday', JSON_ARRAY('佳佳', '鈞顯', '曹格', '佑庭', '榮一')
        )
    ),
    1
) ON DUPLICATE KEY UPDATE
    config_data = VALUES(config_data),
    updated_at = CURRENT_TIMESTAMP;


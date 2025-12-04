-- 修復崗位配置的字符編碼問題
-- 刪除現有的亂碼數據並重新插入

USE church;

-- 設置會話字符集為 utf8mb4
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 刪除現有的配置（如果存在）
DELETE FROM position_config WHERE config_name = 'default';

-- 重新插入正確的配置
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
);


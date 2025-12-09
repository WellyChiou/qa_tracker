-- 創建前台頁面資料表（存儲結構化資料，非 HTML）
-- 用於首頁、關於我們、活動、聯絡我們等頁面的資料管理

USE church;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================
-- 1. 教會基本資訊表（聯絡資訊、服務時間等）
-- ============================================
CREATE TABLE IF NOT EXISTS church_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    info_key VARCHAR(50) NOT NULL UNIQUE COMMENT '資訊鍵值（如：address, phone, email, service_hours）',
    info_value TEXT COMMENT '資訊內容',
    info_type VARCHAR(20) DEFAULT 'text' COMMENT '資訊類型（text, json）',
    display_order INT DEFAULT 0 COMMENT '顯示順序',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否啟用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_info_key (info_key),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教會基本資訊表';

-- ============================================
-- 2. 關於我們資訊表
-- ============================================
CREATE TABLE IF NOT EXISTS about_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    section_key VARCHAR(50) NOT NULL COMMENT '區塊鍵值（如：welcome, mission, vision, values, history）',
    title VARCHAR(200) NOT NULL COMMENT '標題',
    content TEXT COMMENT '內容',
    display_order INT DEFAULT 0 COMMENT '顯示順序',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否啟用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_section_key (section_key),
    INDEX idx_is_active (is_active),
    INDEX idx_display_order (display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='關於我們資訊表';

-- ============================================
-- 3. 活動資訊表
-- ============================================
CREATE TABLE IF NOT EXISTS activities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    title VARCHAR(200) NOT NULL COMMENT '活動標題',
    description TEXT COMMENT '活動描述',
    activity_date DATE COMMENT '活動日期',
    activity_time VARCHAR(100) COMMENT '活動時間',
    location VARCHAR(200) COMMENT '活動地點',
    tags JSON COMMENT '活動標籤（JSON 陣列）',
    image_url VARCHAR(500) COMMENT '活動圖片 URL',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否啟用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_activity_date (activity_date),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活動資訊表';

-- ============================================
-- 4. 聯絡表單提交記錄表
-- ============================================
CREATE TABLE IF NOT EXISTS contact_submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    name VARCHAR(100) NOT NULL COMMENT '姓名',
    email VARCHAR(255) NOT NULL COMMENT '電子郵件',
    phone VARCHAR(50) COMMENT '電話',
    message TEXT NOT NULL COMMENT '訊息內容',
    is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已讀',
    submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交時間',
    read_at DATETIME COMMENT '已讀時間',
    read_by VARCHAR(128) COMMENT '已讀者 UID',
    INDEX idx_submitted_at (submitted_at),
    INDEX idx_is_read (is_read),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聯絡表單提交記錄表';

-- ============================================
-- 初始化資料
-- ============================================

-- 插入教會基本資訊
INSERT IGNORE INTO church_info (info_key, info_value, display_order) VALUES
('address', '251新北市淡水區民族路31巷1號B1', 1),
('phone', '(02) 2809-0959', 2),
('email', 'contact@church.org', 3),
('service_hours_weekday', '週一至週五：上午 9:00 - 下午 5:00', 4),
('service_hours_weekend', '週六：上午 9:00 - 中午 12:00', 5);

-- 插入關於我們資訊
INSERT IGNORE INTO about_info (section_key, title, content, display_order) VALUES
('welcome', '歡迎加入PLC極光', '歡迎您加入PLC極光這個溫暖大家庭，讓我們在基督的愛及聖經真理中成長茁壯，邁向蒙福豐盛光明的人生！', 1),
('mission', '我們的使命', '我們致力於建立一個充滿愛、關懷與成長的信仰社群，透過敬拜、教導和團契，幫助每個人更認識神，並在信仰中成長。', 2),
('vision', '我們的願景', '成為一個影響社區、傳播福音、培養門徒的教會，讓每個人都能在這裡找到歸屬感，並活出神所賜的豐盛生命。', 3),
('values', '我們的價值', '愛：以基督的愛彼此相愛\n真理：持守聖經的教導\n合一：在基督裡彼此連結\n服務：關懷社區，服務他人\n成長：持續在信仰中成長', 4),
('history', '歷史沿革', '我們的教會成立於多年前，從一個小小的聚會開始，逐漸成長為今天的規模。我們感謝神的恩典，也感謝每一位弟兄姊妹的參與和奉獻。', 5);

-- 插入活動資訊
INSERT IGNORE INTO activities (title, description, activity_date, activity_time, location, tags) VALUES
('聯合受洗主日', '聯合受洗主日，歡迎弟兄姊妹一同參與這個重要的時刻。', '2024-12-28', '10:00am', '榮耀堂', '["主日", "受洗"]'),
('極光元旦登高', '新的一年，讓我們一起登高，迎接新的開始！', '2025-01-01', '9:30am', '觀音山遊客中心', '["戶外", "運動"]'),
('極光同工尾牙', '感謝同工們一年來的辛勞，讓我們一起歡聚慶祝！', '2025-01-11', '傍晚（時間待定）', '地點待定', '["同工", "聚餐"]'),
('極光門徒退修會', '門徒退修會，讓我們在安靜中親近神，重新得力。', '2025-02-27', '兩天一夜', '桃園復興鄉', '["退修會", "門徒"]');

-- 顯示結果
SELECT '資料表已創建並初始化' AS message;


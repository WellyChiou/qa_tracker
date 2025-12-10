-- 插入主日信息
-- 根據圖片「2025 極光教會 聖誕幸福系列活動」創建
USE church_db;

-- 1. 插入「幸福名人講座 3 - 在光中的奇蹟」（12/13 週六晚上7:00）
INSERT INTO sunday_messages (
    service_date,
    service_type,
    image_url,
    title,
    scripture,
    speaker,
    content,
    is_active,
    created_at,
    updated_at
) VALUES (
    '2025-12-13',  -- 圖片中「幸福名人講座 3」的日期
    'SATURDAY',    -- 圖片中顯示為「週六晚上7:00」
    'https://example.com/images/2025_aurora_church_miracle_in_light.jpg',  -- 請修改為實際的圖片URL
    '在光中的奇蹟',
    NULL,  -- 圖片中未明確指出經文，可根據實際講座內容補充
    '王倩倩 老師 / 曾俊盛 總經理',  -- 圖片中顯示的講員
    '這是極光教會2025聖誕幸福系列活動中的「幸福名人講座 3」，主題為「在光中的奇蹟」。由知名暢銷書作家王倩倩老師與知名企業家曾俊盛總經理分享。時間：週六晚上7:00。',
    1,
    NOW(),
    NOW()
);

-- 2. 插入「聖誕慶典」（12/20 週六下午4:00）
INSERT INTO sunday_messages (
    service_date,
    service_type,
    image_url,
    title,
    scripture,
    speaker,
    content,
    is_active,
    created_at,
    updated_at
) VALUES (
    '2025-12-20',  -- 圖片中「聖誕慶典」的日期
    'SATURDAY',    -- 圖片中顯示為「週六下午4:00」
    'https://example.com/images/2025_aurora_church_christmas_celebration.jpg',  -- 請修改為實際的圖片URL
    '2025 聖誕慶典',
    NULL,  -- 慶典可能沒有特定經文
    '極光教會團隊',  -- 慶典通常由團隊共同參與
    '極光教會2025聖誕幸福系列活動中的「聖誕慶典」，內容包含動感舞蹈、金曲表演、感人戲劇及豐富獎品。時間：週六下午4:00。',
    1,
    NOW(),
    NOW()
);


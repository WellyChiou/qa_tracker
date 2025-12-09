-- 創建頁面內容表（用於存儲前台頁面的內容）
-- 支援首頁、關於我們、活動、聯絡我們等頁面的內容管理

USE church;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 創建頁面內容表
CREATE TABLE IF NOT EXISTS page_contents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    page_code VARCHAR(50) NOT NULL UNIQUE COMMENT '頁面代碼（如：home, about, activities, contact）',
    page_title VARCHAR(200) NOT NULL COMMENT '頁面標題',
    content TEXT COMMENT '頁面內容（HTML 格式）',
    meta_description VARCHAR(500) COMMENT 'SEO 描述',
    meta_keywords VARCHAR(255) COMMENT 'SEO 關鍵字',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否啟用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    created_by VARCHAR(128) COMMENT '建立者 UID',
    updated_by VARCHAR(128) COMMENT '更新者 UID',
    INDEX idx_page_code (page_code),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='前台頁面內容表';

-- 創建聯絡表單提交記錄表
CREATE TABLE IF NOT EXISTS contact_submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵 ID',
    name VARCHAR(100) NOT NULL COMMENT '姓名',
    email VARCHAR(255) NOT NULL COMMENT '電子郵件',
    phone VARCHAR(50) COMMENT '電話',
    message TEXT NOT NULL COMMENT '訊息內容',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '狀態（pending=待處理, read=已讀, replied=已回覆, archived=已歸檔）',
    replied_at DATETIME COMMENT '回覆時間',
    replied_by VARCHAR(128) COMMENT '回覆者 UID',
    reply_message TEXT COMMENT '回覆內容',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聯絡表單提交記錄表';

-- 初始化預設頁面內容
INSERT INTO page_contents (page_code, page_title, content, meta_description, is_active) VALUES
('home', '首頁', 
'<section class="hero">
  <h1>Welcome to Power Light Church!</h1>
  <p>歡迎您加入PLC極光這個溫暖大家庭，讓我們在基督的愛及聖經真理中成長茁壯，邁向蒙福豐盛光明的人生！</p>
</section>
<section class="section">
  <h2>最新消息</h2>
  <p>這裡是最新消息的內容...</p>
</section>
<section class="section">
  <h2>聚會時間</h2>
  <div>
    <h3>晚崇聚會</h3>
    <p>時間：每週六晚上 7:00</p>
    <p>地點：教會大堂</p>
  </div>
  <div>
    <h3>早崇聚會</h3>
    <p>時間：每週日早上 10:00</p>
    <p>地點：教會大堂</p>
  </div>
</section>',
'極光教會首頁，歡迎加入我們這個溫暖的大家庭', 1),

('about', '關於我們',
'<section>
  <h2>歡迎加入PLC極光</h2>
  <p>歡迎您加入PLC極光這個溫暖大家庭，讓我們在基督的愛及聖經真理中成長茁壯，邁向蒙福豐盛光明的人生！</p>
</section>
<section>
  <h2>我們的使命</h2>
  <p>我們致力於建立一個充滿愛、關懷與成長的信仰社群，透過敬拜、教導和團契，幫助每個人更認識神，並在信仰中成長。</p>
</section>
<section>
  <h2>我們的願景</h2>
  <p>成為一個影響社區、傳播福音、培養門徒的教會，讓每個人都能在這裡找到歸屬感，並活出神所賜的豐盛生命。</p>
</section>
<section>
  <h2>我們的價值</h2>
  <ul>
    <li><strong>愛：</strong>以基督的愛彼此相愛</li>
    <li><strong>真理：</strong>持守聖經的教導</li>
    <li><strong>合一：</strong>在基督裡彼此連結</li>
    <li><strong>服務：</strong>關懷社區，服務他人</li>
    <li><strong>成長：</strong>持續在信仰中成長</li>
  </ul>
</section>
<section>
  <h2>歷史沿革</h2>
  <p>我們的教會成立於多年前，從一個小小的聚會開始，逐漸成長為今天的規模。我們感謝神的恩典，也感謝每一位弟兄姊妹的參與和奉獻。</p>
</section>',
'關於極光教會，我們的使命、願景和價值', 1),

('activities', '活動資訊',
'<section>
  <h2>活動資訊</h2>
  <p>這裡是活動資訊的內容，可以從後台管理系統進行編輯。</p>
</section>',
'極光教會活動資訊', 1),

('contact', '聯絡我們',
'<section>
  <h2>聯絡資訊</h2>
  <div>
    <strong>地址：</strong>
    <p>251新北市淡水區民族路31巷1號B1</p>
  </div>
  <div>
    <strong>電話：</strong>
    <p>(02) 2345-6789</p>
  </div>
  <div>
    <strong>電子郵件：</strong>
    <p>contact@church.org</p>
  </div>
  <div>
    <strong>服務時間：</strong>
    <p>週一至週五：上午 9:00 - 下午 5:00</p>
    <p>週六：上午 9:00 - 中午 12:00</p>
  </div>
</section>',
'聯絡極光教會', 1)
ON DUPLICATE KEY UPDATE
    page_title = VALUES(page_title),
    content = VALUES(content),
    meta_description = VALUES(meta_description);

-- 顯示結果
SELECT '頁面內容表已創建' AS message;
SELECT page_code, page_title, LEFT(content, 50) AS content_preview, is_active
FROM page_contents
ORDER BY page_code;


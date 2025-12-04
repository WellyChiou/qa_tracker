# LINE Bot 費用記錄功能

## 功能概述

本系統集成了 LINE Bot 功能，讓您可以通過 LINE 直接記錄費用，並接收每日費用提醒通知。

## 主要功能

### 1. LINE 訊息記錄費用
- **支出記錄**: 發送 `支出 餐費 150 午餐` 即可記錄支出
- **收入記錄**: 發送 `收入 薪水 50000` 即可記錄收入
- **支援格式**: `類型 類別 金額 描述`

### 2. 查詢功能
- **狀態查詢**: 發送 `狀態` 查看今日收支總計
- **今日記錄**: 發送 `今天` 查看今日所有費用
- **幫助**: 發送 `幫助` 查看使用說明

### 3. 自動提醒
- **每日提醒**: 晚上 8 點檢查是否記錄了今日費用
- **統計報告**: 晚上 9 點發送今日費用統計

## 設置步驟

### 1. 創建 LINE Bot

1. 前往 [LINE Developers Console](https://developers.line.biz/console/)
2. 創建新的 Provider 和 Channel (Messaging API)
3. 記下以下資訊：
   - Channel Access Token
   - Channel Secret
   - **Bot ID**（用於用戶搜尋和加入 Bot）
   - **QR Code**（用於用戶掃描加入 Bot）

### 1.1 獲取 Bot 的 QR Code 和 ID

**找到 Bot ID 的方法：**

1. 登入 [LINE Developers Console](https://developers.line.biz/console/)
2. 選擇您的 Provider，然後進入 Bot Channel
3. 點擊「Messaging API」標籤
4. 在頁面頂部或「Basic settings」中，您會看到：
   - **Channel ID**：這是 Bot 的內部 ID（格式如：`1234567890`）
   - **Channel name**：Bot 的名稱

**注意：** LINE Messaging API 的 Bot 通常沒有傳統的「Bot ID」（如 `@botname`），而是使用：
- **加入連結**：格式為 `https://line.me/R/ti/p/@channel_id`（需要先啟用「Add friends」功能）
- **QR Code**：在「Messaging API」頁面的「QR Code」區域可以下載

**如果沒有 QR Code：**
- 可以在 LINE Developers Console 的「Messaging API」頁面找到「QR Code」區域並下載
- 或者使用加入連結（需要先啟用「Add friends」功能）

### 1.2 配置 Bot 資訊到系統（可選）

為了讓用戶在網頁中直接看到 QR Code 或 Bot ID，您可以將這些資訊配置到資料庫：

**方法 1：配置 QR Code URL（推薦）**

**選項 A：放在項目中（推薦，最簡單）**
1. 在 LINE Developers Console 下載 QR Code 圖片
2. 將圖片放在 `frontend/public/` 目錄（例如：`frontend/public/line-bot-qrcode.png`）
3. 在資料庫中配置相對路徑或完整 URL：
   ```sql
   -- 使用相對路徑（推薦，自動適配域名）
   UPDATE config 
   SET config_value = '/line-bot-qrcode.png' 
   WHERE config_key = 'line_bot_qr_code_url';
   
   -- 或使用完整 URL（如果需要）
   UPDATE config 
   SET config_value = 'https://wc-project.duckdns.org/line-bot-qrcode.png' 
   WHERE config_key = 'line_bot_qr_code_url';
   ```
4. 重新部署前端，圖片會自動包含在構建中

**選項 B：使用圖床服務**
1. 將 QR Code 圖片上傳到圖床服務（如 [imgur](https://imgur.com)、[imgbb](https://imgbb.com) 等）
2. 獲取圖片的公開 URL
3. 在資料庫中配置：
   ```sql
   UPDATE config 
   SET config_value = 'https://圖床服務的URL/qrcode.png' 
   WHERE config_key = 'line_bot_qr_code_url';
   ```

**選項 C：使用 GitHub**
1. 將 QR Code 圖片上傳到 GitHub 倉庫
2. 使用 GitHub 的 raw URL：
   ```sql
   UPDATE config 
   SET config_value = 'https://raw.githubusercontent.com/您的用戶名/qa_tracker/main/frontend/public/qrcode.png' 
   WHERE config_key = 'line_bot_qr_code_url';
   ```

**方法 2：配置加入連結（推薦，最簡單）**

**步驟 1：找到 Channel ID**
1. 登入 [LINE Developers Console](https://developers.line.biz/console/)
2. 選擇您的 Provider，進入 Bot Channel
3. 點擊「Messaging API」標籤
4. 在頁面頂部可以看到「Channel ID」（一串數字，例如：`1234567890`）
5. 複製這個 Channel ID

**步驟 2：生成加入連結**
加入連結格式：`https://line.me/R/ti/p/@您的ChannelID`

例如：如果 Channel ID 是 `1234567890`，則加入連結為：
```
https://line.me/R/ti/p/@1234567890
```

**步驟 3：配置到資料庫**
```sql
-- 直接配置加入連結（推薦）
UPDATE config 
SET config_value = 'https://line.me/R/ti/p/@您的ChannelID' 
WHERE config_key = 'line_bot_join_url';

-- 或者使用 INSERT（如果配置項不存在）
INSERT INTO config (config_key, config_value, description) 
VALUES ('line_bot_join_url', 'https://line.me/R/ti/p/@您的ChannelID', 'LINE Bot 加入連結')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);
```

**注意：** 如果加入連結無法使用，可能需要：
1. 在 LINE Developers Console 的「Messaging API」頁面
2. 啟用「Add friends」功能
3. 然後重新生成加入連結

配置後，用戶在「個人資料設定」頁面中就能直接看到 QR Code 或加入連結，無需向管理員索取。

### 2. 配置環境變數

在您的 `.env` 文件或 docker-compose.yml 中設置：

```bash
LINE_BOT_CHANNEL_TOKEN=你的_channel_access_token
LINE_BOT_CHANNEL_SECRET=你的_channel_secret
LINE_BOT_WEBHOOK_URL=https://你的域名/api/line/webhook
LINE_BOT_DAILY_REMINDER_ENABLED=true
LINE_BOT_DAILY_REMINDER_TIME=20:00
```

### 2.1 獲取 LINE User ID

**重要：** 綁定 LINE Bot 需要的是 **LINE User ID**（系統內部 ID），不是您的自定義 LINE ID（如 jia-wei-chiou）。

**LINE User ID 格式：** 以 `U` 開頭，跟隨 32 位十六進制字符
**例如：** `U1234567890abcdef1234567890abcdef`

**獲取方法：**

**方法 1：通過 LINE Bot 獲取（最簡單，推薦）**

**步驟 1：加入 LINE Bot**
1. 在 LINE Developers Console 中找到您的 Bot
2. 進入 Bot 的「Messaging API」頁面
3. 找到「QR Code」或「Bot ID」
4. 使用 LINE 掃描 QR Code 或搜尋 Bot ID 加入 Bot

**步驟 2：獲取 LINE User ID**
1. 在 LINE 中發送任何訊息給 Bot（例如：「你好」或「test」）
2. Bot 會自動回覆並顯示您的 LINE User ID（以 `U` 開頭的長字符串）
3. 複製這個 ID

**步驟 3：綁定帳號**
1. 在網頁應用的「個人資料設定」中輸入剛才獲取的 LINE User ID
2. 點擊「綁定 LINE 帳號」完成綁定

**方法 2：使用瀏覽器開發者工具**
1. 在電腦上訪問 https://line.me
2. 用您的 LINE 帳號登入
3. 按 F12 開啟開發者工具
4. 在 Console 輸入：
   ```javascript
   JSON.parse(localStorage.getItem('loginInfo')).userId
   ```
5. 顯示的即為您的 LINE User ID

**方法 3：查看後端日誌**
1. 在 LINE 中發送訊息給 Bot
2. 查看後端日誌，會顯示您的真實 LINE User ID

### 3. 數據庫更新

檢查並更新資料庫結構以支援 LINE Bot 功能：

**選項 1：自動遷移（推薦）**
Spring Boot JPA 會自動處理表結構更新，無需手動執行 SQL。

**選項 2：手動遷移**
如果需要手動控制，可以使用提供的遷移腳本：

**自動腳本（推薦）：**
```bash
# 一鍵執行資料庫遷移
./update-db-for-line-bot.sh
```

**手動執行 SQL：**
```sql
-- 檢查並更新 users 表結構
mysql/check-and-update-users-table.sql

-- 或僅添加 LINE 用戶 ID 字段
mysql/add-line-user-id-column.sql
```

**驗證更新：**
```sql
DESCRIBE qa_tracker.users;
```

應該看到 `line_user_id` 欄位已添加。

### 4. 重啟服務

重新構建並啟動服務：

```bash
docker-compose down
docker-compose up --build
```

### 5. 設置 Webhook

在 LINE Developers Console 中設置 Webhook URL：
```
https://你的域名/api/line/webhook
```

## 用戶綁定流程

### 網頁應用綁定
1. 用戶登入網頁應用
2. 進入個人設定頁面
3. 點擊「綁定 LINE」按鈕
4. 掃描 QR 碼或輸入驗證碼完成綁定

### API 綁定
```bash
# 綁定 LINE 帳號
POST /api/users/{userUid}/bind-line
{
  "lineUserId": "U1234567890abcdef..."
}

# 解除綁定
POST /api/users/{userUid}/unbind-line

# 檢查綁定狀態
GET /api/users/{userUid}/line-status
```

## 使用示例

### 記錄費用
```
支出 餐費 150 午餐
收入 薪水 50000 月薪
支出 交通 25 公車
```

### 查詢功能
```
狀態
今天
幫助
```

## 訊息格式說明

### 費用記錄格式
```
[類型] [主類別] [金額] [描述(可選)]

類型: 支出 或 收入
主類別: 如 餐費、交通、娛樂等
金額: 數字，可包含小數點
描述: 可選的詳細說明
```

### 支援的指令
- `幫助` / `help` - 顯示使用說明
- `狀態` / `status` - 查看今日收支統計
- `今天` / `today` - 查看今日所有記錄

## 安全注意事項

1. **Channel Token 安全**: 妥善保管 Channel Access Token，不要在代碼中硬編碼
2. **Webhook 驗證**: LINE Bot SDK 會自動驗證 webhook 請求的真實性
3. **用戶驗證**: 只有綁定了 LINE 帳號的用戶才能使用 Bot 功能
4. **權限控制**: 費用記錄會按照用戶身份正確關聯

## 故障排除

### Webhook 無法接收訊息
1. 檢查 Webhook URL 是否正確
2. 確認服務器可以從公網訪問
3. 查看應用日誌中的錯誤訊息

### 用戶無法綁定
1. 確認 LINE Bot 的 Channel Access Token 正確
2. 檢查用戶權限和會話狀態
3. 查看服務器日誌

### 提醒功能不工作
1. 確認排程任務是否啟用 (`LINE_BOT_DAILY_REMINDER_ENABLED=true`)
2. 檢查用戶是否已綁定 LINE 帳號
3. 查看排程任務的執行日誌

## 技術架構

- **後端**: Spring Boot + 自定義 LINE Bot 整合 (使用 HTTP Client)
- **數據庫**: MySQL (用戶表添加 line_user_id 字段)
- **排程**: Spring @Scheduled 註解
- **安全性**: JWT 認證 + 自定義 webhook 驗證

## 日誌查看

重要日誌位置：
- LINE webhook 請求: `application.log` 中的 "📨 收到 LINE webhook"
- 訊息處理: "🎯 處理事件類型" 和 "✅ 已發送回覆訊息"
- 排程任務: "⏰ 開始執行每日費用提醒任務"

## 未來擴展

- 支援更多訊息類型 (圖片、貼圖)
- 自定義提醒時間
- 費用分類統計圖表
- 多語言支援
- 語音記錄功能

# 靜態資源目錄

此目錄用於存放靜態資源文件，如圖片、字體等。

## QR Code 圖片

如果您有 LINE Bot 的 QR Code 圖片，可以：

1. 將 QR Code 圖片放在此目錄（例如：`qrcode.png`）
2. 在資料庫中配置 URL：
   ```sql
   UPDATE config 
   SET config_value = '/qrcode.png' 
   WHERE config_key = 'line_bot_qr_code_url';
   ```
   或使用完整 URL（如果部署後需要）：
   ```sql
   UPDATE config 
   SET config_value = 'https://wc-project.duckdns.org/qrcode.png' 
   WHERE config_key = 'line_bot_qr_code_url';
   ```

## 注意

- 此目錄中的文件會直接複製到構建輸出的根目錄
- 不需要在代碼中 import，直接使用路徑即可
- 例如：`<img src="/qrcode.png" />` 即可訪問


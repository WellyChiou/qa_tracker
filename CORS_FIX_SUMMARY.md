# CORS 錯誤修正說明

## 問題原因

使用 `credentials: 'include'` 發送請求時，後端的 `Access-Control-Allow-Origin` 不能是 `*`，必須是具體的域名。

## 修正內容

### ✅ 後端修正（主要）

**文件：** `backend/src/main/java/com/example/helloworld/config/SecurityConfig.java`

**變更：**
- 將 `allowedOrigins` 從 `*` 改為具體域名列表
- 啟用 `allowCredentials(true)` 
- 添加必要的 CORS 標頭配置

**允許的來源：**
```java
configuration.setAllowedOrigins(Arrays.asList(
    "http://localhost",
    "http://localhost:80",
    "http://38.54.89.136",
    "http://38.54.89.136:80",
    "https://38.54.89.136",
    "https://38.54.89.136:443"
));
```

### ✅ 前端修正（次要）

前端已正確配置：
- `login.html` - 登入請求已包含 `credentials: 'include'`
- `index.html` - 認證相關請求已包含 `credentials: 'include'`
- `api.js` - 認證相關 API 已包含 `credentials: 'include'`

## 重要提醒

1. **必須重啟後端服務** 才能讓 CORS 配置生效
2. 如果您的域名不同，請在 `SecurityConfig.java` 中更新 `allowedOrigins` 列表
3. 所有需要認證的 API 請求都應該包含 `credentials: 'include'`

## 測試步驟

1. 重啟後端服務
2. 訪問 `http://38.54.89.136/login.html`
3. 使用預設帳號登入（admin / admin123）
4. 應該能成功登入而不會出現 CORS 錯誤


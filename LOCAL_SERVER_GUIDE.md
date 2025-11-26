# 🐍 使用 Python 本地伺服器執行 tracker.html

## ✅ 為什麼使用 Python 伺服器？

- ✅ 避免 Mixed Content 問題（使用 HTTP 協議）
- ✅ 不需要安裝額外工具（Python 通常已安裝）
- ✅ 簡單快速
- ✅ 可以正常呼叫 HTTP API

---

## 🚀 快速開始

### 步驟 1: 確認 Python 已安裝

```bash
# 檢查 Python 版本
python --version
# 或
python3 --version
```

如果顯示版本號（如 Python 3.8.0），表示已安裝。

### 步驟 2: 進入 tracker.html 所在目錄

```bash
# 進入專案目錄
cd /Users/wellychiou/my-github/fb-issue-record

# 或 tracker.html 所在的任何目錄
cd /path/to/tracker.html
```

### 步驟 3: 啟動 Python HTTP 伺服器

#### Python 3（推薦）

```bash
# 啟動伺服器（預設端口 8000）
python3 -m http.server 8000

# 或指定其他端口
python3 -m http.server 8080
```

#### Python 2（如果只有 Python 2）

```bash
python -m SimpleHTTPServer 8000
```

### 步驟 4: 在瀏覽器打開

伺服器啟動後，會顯示：
```
Serving HTTP on 0.0.0.0 port 8000 (http://0.0.0.0:8000/) ...
```

在瀏覽器打開：
```
http://localhost:8000/tracker.html
```

或
```
http://127.0.0.1:8000/tracker.html
```

---

## 📋 完整操作流程

### 1. 開啟終端機（Terminal）

在 Mac 上：
- 按 `Cmd + Space` 搜尋「Terminal」
- 或打開「應用程式」→「工具程式」→「終端機」

### 2. 進入專案目錄

```bash
cd /Users/wellychiou/my-github/fb-issue-record
```

### 3. 啟動伺服器

```bash
python3 -m http.server 8000
```

### 4. 打開瀏覽器

訪問：`http://localhost:8000/tracker.html`

### 5. 執行匯入

1. 登入 Firebase
2. 點擊「匯入到新系統」
3. 輸入 API 地址：`http://38.54.89.136:8080`
4. 開始匯入

### 6. 停止伺服器

在終端機按 `Ctrl + C` 停止伺服器。

---

## 🔧 進階設定

### 指定 IP 和端口

```bash
# 只允許本地訪問（預設）
python3 -m http.server 8000

# 允許區域網路訪問
python3 -m http.server 8000 --bind 0.0.0.0
```

### 使用不同端口

```bash
# 使用 3000 端口
python3 -m http.server 3000

# 使用 9000 端口
python3 -m http.server 9000
```

---

## 🎯 常見問題

### 問題 1: 端口已被佔用

**錯誤訊息：**
```
Address already in use
```

**解決方案：**
```bash
# 使用其他端口
python3 -m http.server 8001

# 或找出佔用端口的程序並停止
lsof -ti:8000 | xargs kill -9
```

### 問題 2: Python 未安裝

**錯誤訊息：**
```
command not found: python3
```

**解決方案：**

**Mac:**
```bash
# 安裝 Homebrew（如果沒有）
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 安裝 Python
brew install python3
```

**或下載安裝程式：**
- 前往：https://www.python.org/downloads/
- 下載並安裝 Python 3

### 問題 3: 權限問題

**錯誤訊息：**
```
Permission denied
```

**解決方案：**
```bash
# 檢查檔案權限
ls -la tracker.html

# 如果需要，修改權限
chmod 644 tracker.html
```

---

## 💡 實用技巧

### 技巧 1: 背景執行

```bash
# 在背景執行
python3 -m http.server 8000 &

# 查看背景程序
jobs

# 停止背景程序
kill %1
```

### 技巧 2: 查看伺服器日誌

Python HTTP 伺服器會自動顯示訪問日誌：
```
127.0.0.1 - - [15/Dec/2024 10:30:45] "GET /tracker.html HTTP/1.1" 200 -
```

### 技巧 3: 同時服務多個檔案

如果目錄中有多個 HTML 檔案，都可以通過伺服器訪問：
- `http://localhost:8000/tracker.html`
- `http://localhost:8000/index.html`
- `http://localhost:8000/expenses.html`

---

## 🔒 安全性提醒

### ⚠️ 注意事項

1. **僅用於本地開發**
   - Python HTTP 伺服器不適合生產環境
   - 僅用於本地測試和開發

2. **防火牆設定**
   - 預設只允許本地訪問（localhost）
   - 如果使用 `--bind 0.0.0.0`，會允許區域網路訪問

3. **不要暴露敏感資訊**
   - 確保目錄中沒有敏感檔案
   - 伺服器會列出目錄中的所有檔案

---

## 📝 完整範例

### 範例 1: 基本使用

```bash
# 1. 進入目錄
cd /Users/wellychiou/my-github/fb-issue-record

# 2. 啟動伺服器
python3 -m http.server 8000

# 3. 在瀏覽器打開
# http://localhost:8000/tracker.html

# 4. 使用完畢後按 Ctrl+C 停止
```

### 範例 2: 使用自訂端口

```bash
# 啟動在 3000 端口
python3 -m http.server 3000

# 訪問
# http://localhost:3000/tracker.html
```

### 範例 3: 允許區域網路訪問

```bash
# 啟動並允許區域網路訪問
python3 -m http.server 8000 --bind 0.0.0.0

# 其他設備可以通過您的 IP 訪問
# http://您的IP:8000/tracker.html
```

---

## 🎯 與其他方案比較

| 方案 | 優點 | 缺點 |
|------|------|------|
| **Python HTTP 伺服器** | ✅ 簡單快速<br>✅ 無需安裝<br>✅ 避免 Mixed Content | ⚠️ 僅本地使用 |
| **本地檔案（file://）** | ✅ 最簡單 | ⚠️ 某些功能可能受限 |
| **設置 HTTPS** | ✅ 最安全<br>✅ 生產環境適用 | ⚠️ 需要設定<br>⚠️ 需要域名或證書 |
| **允許混合內容** | ✅ 快速 | ⚠️ 不安全<br>⚠️ 不推薦 |

---

## ✅ 推薦流程

1. **啟動 Python 伺服器**
   ```bash
   cd /Users/wellychiou/my-github/fb-issue-record
   python3 -m http.server 8000
   ```

2. **打開瀏覽器**
   ```
   http://localhost:8000/tracker.html
   ```

3. **執行匯入**
   - 登入 Firebase
   - 點擊「匯入到新系統」
   - 輸入：`http://38.54.89.136:8080`
   - 開始匯入

4. **完成後停止伺服器**
   - 在終端機按 `Ctrl + C`

---

## 🎉 完成！

使用 Python HTTP 伺服器是最簡單且有效的方法，可以完美解決 Mixed Content 問題！

祝您匯入順利！🚀


# GitHub 推送指南

## 您的倉庫地址
- GitHub 倉庫: `https://github.com/WellyChiou/qa_tracker`
- 倉庫設置: `https://github.com/WellyChiou/qa_tracker/settings`

## 方案 1: 使用 Personal Access Token（推薦）

### 創建 GitHub Personal Access Token

1. 訪問：https://github.com/settings/tokens
2. 點擊 **"Generate new token"** → **"Generate new token (classic)"**
3. 填寫：
   - **Note**: `qa_tracker_push`
   - **Expiration**: 選擇過期時間（或選擇 `No expiration`）
   - **Select scopes**: 勾選 ✅ **`repo`**（這會自動選中所有 repo 權限）
4. 點擊 **"Generate token"**
5. **重要！** 複製生成的 Token（只會顯示一次）

### 推送時使用 Token

當 Git 要求輸入密碼時：
- **Username**: 輸入您的 GitHub 用戶名 `WellyChiou`
- **Password**: 輸入剛才創建的 **Personal Access Token**（不是 GitHub 密碼！）

## 方案 2: 使用 SSH（更簡單，推薦）

### 步驟 1: 檢查是否已有 SSH 密鑰

```bash
ls -al ~/.ssh
```

### 步驟 2: 創建 SSH 密鑰（如果沒有）

```bash
ssh-keygen -t ed25519 -C "your_email@example.com"
```

按 Enter 三次（使用默認設置）。

### 步驟 3: 複製公鑰

```bash
cat ~/.ssh/id_ed25519.pub
```

複製整個輸出內容。

### 步驟 4: 添加到 GitHub

1. 訪問：https://github.com/settings/ssh/new
2. **Title**: 輸入名稱，例如 `My Mac`
3. **Key**: 貼上剛才複製的公鑰
4. 點擊 **"Add SSH key"**

### 步驟 5: 測試連接

```bash
ssh -T git@github.com
```

如果看到 `Hi WellyChiou! You've successfully authenticated...`，說明成功。

### 步驟 6: 使用 SSH URL

```bash
git remote set-url origin git@github.com:WellyChiou/qa_tracker.git
git push -u origin main
```

## 推送命令

### 使用 HTTPS（需要 Token）
```bash
cd /Users/wellychiou/my-github/docker-vue-java-mysql
git remote add origin https://github.com/WellyChiou/qa_tracker.git
git push -u origin main
```

### 使用 SSH（不需要密碼）
```bash
cd /Users/wellychiou/my-github/docker-vue-java-mysql
git remote set-url origin git@github.com:WellyChiou/qa_tracker.git
git push -u origin main
```

## 注意事項

⚠️ **GitHub 已不再支持使用密碼推送**，必須使用：
- Personal Access Token（HTTPS 方式）
- SSH 密鑰（SSH 方式）

推薦使用 SSH 方式，設置一次後就不需要每次都輸入密碼了。


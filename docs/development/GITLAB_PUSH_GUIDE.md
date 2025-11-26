# GitLab 推送指南（使用 Personal Access Token）

## 為什麼不能使用帳號密碼？

GitLab 出於安全考慮，已經不再支持直接使用帳號密碼進行 Git 操作。即使您使用 Gmail 登入 GitLab，推送代碼時也需要使用 **Personal Access Token**。

## 步驟 1: 創建 Personal Access Token

### 方法 A: 在 Preferences 中查找

1. 登入您的 GitLab 帳號（使用 Gmail）
2. 點擊右上角的頭像，選擇 **"Preferences"**（偏好設置）
3. 在左側選單的 **"Access"** 部分，查找以下選項：
   - **"Access Tokens"**（訪問令牌）
   - 或 **"Personal Access Tokens"**（個人訪問令牌）
   - 或點擊 **"Password and authentication"**（密碼和認證），然後查找 Token 選項

### 方法 B: 直接訪問 URL

如果找不到選項，可以直接訪問以下 URL（替換成您的 GitLab 域名）：
- GitLab.com: `https://gitlab.com/-/user_settings/personal_access_tokens`
- 私有 GitLab: `https://您的GitLab域名/-/user_settings/personal_access_tokens`

### 方法 C: 使用項目級別的 Token（如果個人 Token 不可用）

在您的倉庫頁面：
1. 進入 `qa_tracker` 倉庫
2. 左側選單 → **Settings** → **Access Tokens**
3. 創建項目級別的訪問令牌
4. 填寫以下信息：
   - **Token name**（令牌名稱）: 例如 `qa_tracker_push`
   - **Expiration date**（過期日期）: 選擇一個日期，或留空為永不過期
   - **Select scopes**（選擇權限）: 
     - ✅ 勾選 **`write_repository`**（寫入倉庫）
     - ✅ 勾選 **`read_repository`**（讀取倉庫）
5. 點擊 **"Create personal access token"**（創建個人訪問令牌）
6. **重要！** 複製生成的 Token（只會顯示一次，請妥善保存）

## 步驟 2: 使用 Token 推送代碼

### 方法 1: 使用 HTTPS（推薦）

當 Git 要求輸入密碼時：
- **Username（用戶名）**: 輸入您的 GitLab 用戶名（或 Gmail）
- **Password（密碼）**: 輸入剛才創建的 **Personal Access Token**（不是 Gmail 密碼！）

### 方法 2: 將 Token 嵌入 URL（更方便）

在添加遠程倉庫時，直接將 Token 放入 URL：

```bash
git remote add origin https://oauth2:您的Token@gitlab.com/您的用戶名/qa_tracker.git
```

這樣推送時就不需要每次都輸入密碼了。

## 步驟 3: 推送代碼

```bash
git push -u origin main
```

或者如果是 `master` 分支：
```bash
git push -u origin master
```

## 注意事項

⚠️ **安全提醒**：
- Personal Access Token 就像密碼一樣重要，請妥善保管
- 不要將 Token 提交到 Git 倉庫中
- 如果 Token 洩露，請立即在 GitLab 中撤銷它

## 常見問題

**Q: 我忘記保存 Token 了怎麼辦？**
A: Token 只會顯示一次。如果忘記了，需要刪除舊的 Token 並創建一個新的。

**Q: Token 可以有多個嗎？**
A: 可以，您可以為不同的項目創建不同的 Token，這樣如果某個 Token 洩露，只需要撤銷它而不影響其他項目。

**Q: 如何撤銷 Token？**
A: 在 GitLab 的 Preferences → Access Tokens 頁面，找到對應的 Token，點擊 "Revoke"（撤銷）。


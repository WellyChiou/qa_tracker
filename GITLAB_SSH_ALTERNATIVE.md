# GitLab 推送替代方案：使用 SSH

如果找不到 Personal Access Token 選項，可以使用 SSH 方式推送代碼，這樣不需要每次都輸入密碼。

## 步驟 1: 檢查是否已有 SSH 密鑰

```bash
ls -al ~/.ssh
```

如果看到 `id_rsa.pub` 或 `id_ed25519.pub`，說明您已經有 SSH 密鑰了。

## 步驟 2: 創建 SSH 密鑰（如果沒有）

```bash
ssh-keygen -t ed25519 -C "your_email@example.com"
```

按 Enter 三次（使用默認路徑和空密碼）或設置密碼。

## 步驟 3: 複製公鑰

```bash
cat ~/.ssh/id_ed25519.pub
```

或者如果是 RSA 密鑰：
```bash
cat ~/.ssh/id_rsa.pub
```

**複製整個輸出內容**（從 `ssh-ed25519` 或 `ssh-rsa` 開始到郵箱結尾）

## 步驟 4: 將公鑰添加到 GitLab

1. 在 GitLab 中，點擊頭像 → **Preferences**
2. 左側選單 → **"SSH and GPG keys"**（SSH 和 GPG 密鑰）
3. 點擊 **"Add new key"**（添加新密鑰）
4. **Title**: 輸入名稱，例如 `My Mac`
5. **Key**: 貼上剛才複製的公鑰內容
6. 點擊 **"Add key"**

## 步驟 5: 測試 SSH 連接

```bash
ssh -T git@gitlab.com
```

如果看到 `Welcome to GitLab, @YourUsername!`，說明連接成功。

## 步驟 6: 使用 SSH URL 添加遠程倉庫

```bash
cd /Users/wellychiou/my-github/docker-vue-java-mysql
git remote add origin git@gitlab.com:您的用戶名/qa_tracker.git
git push -u origin main
```

這樣就不需要輸入密碼或 Token 了！


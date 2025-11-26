# 🔧 修復按鈕功能

## ✅ 已修復的問題

### 1. 執行中按鈕

**問題：** 執行中按鈕不能按

**修復：**
- 將 `inProgressCount` 從 `<span>` 改為可點擊的 `<button>`
- 添加點擊事件處理，點擊後會：
  1. 清空其他篩選欄位
  2. 設定狀態為「執行中」（status = 1）
  3. 執行搜尋

**使用方式：**
- 點擊「執行中」按鈕，會自動篩選出所有執行中的記錄

---

### 2. GitLab Issues 按鈕

**問題：** GitLab Issues 按鈕不見了

**修復：**
- 添加了 GitLab Issues 按鈕
- 添加了 GitLab Issues Modal（目前顯示開發中提示）

**使用方式：**
- 點擊「GitLab Issues」按鈕會打開 Modal
- 目前功能正在開發中，會顯示提示訊息

---

## 🚀 重新部署

修改完成後，需要重新構建前端：

```bash
# SSH 到虛擬主機
ssh root@38.54.89.136

# 進入專案目錄
cd /root/project/work/docker-vue-java-mysql

# 重新構建前端
docker compose up -d --build frontend

# 查看日誌確認
docker compose logs frontend
```

---

## 📝 後續開發

### GitLab Issues 功能

如果需要完整的 GitLab Issues 匯入功能，需要：

1. **後端 API**
   - 添加 GitLab API 整合
   - 提供 GitLab Token 設定 API
   - 提供 GitLab Issues 查詢 API

2. **前端功能**
   - 完整的 GitLab Issues Modal
   - 查詢 GitLab Issues
   - 選擇並匯入 Issues

3. **配置管理**
   - 儲存 GitLab Token（在 config 表中）
   - 管理 GitLab 專案設定

---

## ✅ 完成

現在兩個按鈕都已經修復：
- ✅ 執行中按鈕可以正常點擊
- ✅ GitLab Issues 按鈕已顯示（功能開發中）

請重新構建前端後測試！


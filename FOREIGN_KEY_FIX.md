# 🔗 外鍵約束錯誤修復

## ❓ 錯誤說明

**錯誤訊息：**
```
Cannot add or update a child row: a foreign key constraint fails 
(`qa_tracker`.`records`, CONSTRAINT `records_ibfk_1` 
FOREIGN KEY (`created_by_uid`) REFERENCES `users` (`uid`) 
ON DELETE SET NULL)
```

**原因：**
- `records` 表有外鍵約束指向 `users` 表
- 當匯入記錄時，如果 `created_by_uid` 指向的用戶不存在，就會觸發外鍵約束錯誤

---

## ✅ 解決方案

### 方案 1: 自動處理（已實作）

我已經修改了匯入邏輯，會自動檢查用戶是否存在：
- 如果 `created_by_uid` 對應的用戶不存在，會自動設為 `null`
- 如果 `updated_by_uid` 對應的用戶不存在，會自動設為 `null`

**這樣就可以先匯入 records，不需要先匯入 users。**

### 方案 2: 先匯入 Users（可選）

如果您想保留完整的用戶關聯，可以先匯入 users：

```javascript
// 在 tracker.html 中添加匯入 users 的功能
// 1. 先匯入 users
// 2. 再匯入 records
```

### 方案 3: 暫時移除外鍵約束（不推薦）

如果暫時不想處理外鍵，可以移除外鍵約束：

```sql
-- 移除外鍵約束
ALTER TABLE records DROP FOREIGN KEY records_ibfk_1;

-- 匯入資料後，可以重新添加
ALTER TABLE records 
ADD CONSTRAINT records_ibfk_1 
FOREIGN KEY (created_by_uid) REFERENCES users(uid) 
ON DELETE SET NULL;
```

---

## 🔧 已修改的邏輯

### 匯入記錄時的處理

1. **檢查用戶是否存在**
   ```javascript
   if (record.createdByUid) {
       const userExists = await checkUserExists(apiBaseUrl, record.createdByUid);
       if (!userExists) {
           record.createdByUid = null; // 設為 null 避免外鍵錯誤
       }
   }
   ```

2. **同樣處理 updated_by_uid**
   ```javascript
   if (record.updatedByUid) {
       const userExists = await checkUserExists(apiBaseUrl, record.updatedByUid);
       if (!userExists) {
           record.updatedByUid = null;
       }
   }
   ```

---

## 📝 使用說明

### 現在可以直接匯入 Records

1. 打開 `tracker.html`
2. 點擊「匯入到新系統」
3. 輸入 API 地址：`http://38.54.89.136:8080`
4. 開始匯入

**系統會自動處理：**
- 如果用戶不存在，會將 `created_by_uid` 設為 `null`
- 記錄可以正常匯入，不會觸發外鍵約束錯誤

### 後續可以匯入 Users

如果之後想匯入 users 資料，可以：
1. 手動匯入 users（如果有 users API）
2. 或直接使用 SQL 匯入

---

## ⚠️ 注意事項

### 資料完整性

- 匯入後，`created_by_uid` 可能為 `null`（如果對應的用戶不存在）
- 這是正常的，不會影響記錄的其他資料
- 如果之後匯入 users，可以手動更新 `created_by_uid`

### 外鍵約束

- 外鍵約束仍然存在，確保資料完整性
- 當用戶不存在時，會自動設為 `null`（符合 `ON DELETE SET NULL` 的設計）

---

## ✅ 完成

現在可以正常匯入 records 了，不需要先匯入 users！

如果還有問題，請告訴我。


# Expenses 前端完整搬移指南

## 概述

是的，**完整搬移主要是把 Firebase 調用改成 API 調用**。類似 `tracker.html` 的搬移方式。

## 目前狀態

### ✅ 已完成
1. **後端 API** - 所有 CRUD 操作都已建立
2. **資料庫結構** - expenses, assets, exchange_rates 表
3. **API Service** - `api.js` 已添加 expenses 相關方法
4. **資料遷移功能** - 原始 expenses.html 可以匯入資料

### ⏳ 待完成
- **新的前端頁面** - `frontend/app/expenses.html`（連接新系統 API）

## 搬移步驟

### 1. 建立新的前端頁面

需要建立 `frontend/app/expenses.html`，主要改動：

#### 移除 Firebase SDK
```html
<!-- 移除這些 -->
<script type="module">
  import { initializeApp } from 'https://www.gstatic.com/firebasejs/...';
  // ...
</script>
```

#### 添加 API Service
```html
<!-- 添加這個 -->
<script src="api.js"></script>
```

#### 替換 Firebase 調用為 API 調用

**原本（Firebase）：**
```javascript
// 讀取記錄
const expensesRef = window.firebaseCollection(window.firebaseDb, 'expenses');
const snap = await window.firebaseGetDocs(expensesRef);
const expenses = snap.docs.map(d => ({ id: d.id, ...d.data() }));
```

**改成（API）：**
```javascript
// 讀取記錄
const response = await window.apiService.getExpenses({ year: 2025, month: 1 });
const expenses = response.content; // 分頁結果
// 或
const expenses = await window.apiService.getAllExpenses({ year: 2025, month: 1 }); // 全部
```

**原本（Firebase）：**
```javascript
// 新增記錄
await window.firebaseAddDoc(expensesRef, expenseData);
```

**改成（API）：**
```javascript
// 新增記錄
await window.apiService.createExpense(expenseData);
```

**原本（Firebase）：**
```javascript
// 更新記錄
const docRef = window.firebaseDoc(window.firebaseDb, 'expenses', id);
await window.firebaseUpdateDoc(docRef, updateData);
```

**改成（API）：**
```javascript
// 更新記錄
await window.apiService.updateExpense(id, updateData);
```

**原本（Firebase）：**
```javascript
// 刪除記錄
const docRef = window.firebaseDoc(window.firebaseDb, 'expenses', id);
await window.firebaseDeleteDoc(docRef);
```

**改成（API）：**
```javascript
// 刪除記錄
await window.apiService.deleteExpense(id);
```

### 2. 主要改動對照表

| Firebase 操作 | API 操作 |
|--------------|---------|
| `firebaseCollection(db, 'expenses')` | `apiService.getExpenses()` |
| `firebaseAddDoc(ref, data)` | `apiService.createExpense(data)` |
| `firebaseUpdateDoc(docRef, data)` | `apiService.updateExpense(id, data)` |
| `firebaseDeleteDoc(docRef)` | `apiService.deleteExpense(id)` |
| `firebaseGetDocs(query)` | `apiService.getExpenses(params)` |
| `firebaseCollection(db, 'assets')` | `apiService.getAssets()` |
| `firebaseCollection(db, 'exchangeRates')` | `apiService.getExchangeRate(date)` |

### 3. 資料格式差異

#### Firebase 格式
```javascript
{
  id: "firebase-doc-id",  // Firebase 自動生成
  date: "2025-01-01",
  member: "爸爸",
  type: "支出",
  // ...
  createdAt: Timestamp,  // Firebase Timestamp
  updatedAt: Timestamp
}
```

#### API 格式
```javascript
{
  id: 1,  // MySQL 自動生成的數字 ID
  firebaseId: "firebase-doc-id",  // 遷移時保留的原始 ID
  date: "2025-01-01",
  member: "爸爸",
  type: "支出",
  // ...
  createdAt: "2025-01-01T10:00:00",  // ISO 字串
  updatedAt: "2025-01-01T10:00:00"
}
```

### 4. 需要注意的事項

1. **ID 格式**：Firebase 使用字串 ID，MySQL 使用數字 ID
   - 讀取時：使用 `expense.id`（數字）
   - 更新/刪除時：使用 `expense.id`（數字）

2. **日期格式**：Firebase Timestamp 需要轉換
   - Firebase: `createdAt.toDate()`
   - API: `new Date(createdAt)`

3. **分頁**：API 支援分頁，Firebase 需要手動實現
   ```javascript
   // API 分頁
   const response = await window.apiService.getExpenses({ page: 0, size: 20 });
   const expenses = response.content;
   const totalPages = response.totalPages;
   ```

4. **查詢篩選**：API 使用參數，Firebase 使用 query
   ```javascript
   // API 篩選
   await window.apiService.getExpenses({ 
     year: 2025, 
     month: 1, 
     type: '支出',
     member: '爸爸'
   });
   ```

5. **匯率查詢**：API 使用日期字串
   ```javascript
   // API 匯率
   const rate = await window.apiService.getLatestExchangeRate('2025-01-01');
   ```

## 完整搬移檢查清單

- [ ] 移除 Firebase SDK 引入
- [ ] 添加 `api.js` 引入
- [ ] 替換所有 `firebaseCollection` 為 `apiService.getExpenses()`
- [ ] 替換所有 `firebaseAddDoc` 為 `apiService.createExpense()`
- [ ] 替換所有 `firebaseUpdateDoc` 為 `apiService.updateExpense()`
- [ ] 替換所有 `firebaseDeleteDoc` 為 `apiService.deleteExpense()`
- [ ] 替換資產相關的 Firebase 調用為 `apiService.getAssets()` 等
- [ ] 替換匯率相關的 Firebase 調用為 `apiService.getExchangeRate()` 等
- [ ] 調整 ID 處理（字串 → 數字）
- [ ] 調整日期格式處理（Timestamp → ISO 字串）
- [ ] 調整分頁邏輯（使用 API 的分頁參數）
- [ ] 調整查詢篩選（使用 API 的參數）
- [ ] 測試所有功能：新增、編輯、刪除、查詢、圖表、資產管理、匯率管理

## 總結

**是的，完整搬移主要是把 Firebase 調用改成 API 調用**，其他邏輯（UI、計算、圖表等）基本不需要改動。

需要我幫您建立新的前端 `expenses.html` 頁面嗎？

